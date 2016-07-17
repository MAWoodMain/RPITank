package sensors.MPU9250;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import sensors.MPU9250.MPU9250;
import sensors.dataTypes.CircularArrayRing;
import sensors.dataTypes.TimestampedData3D;
import sensors.interfaces.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public class MPU9250_Pi4j implements Accelerometer, Gyroscope, Magnetometer, Thermometer, Runnable
{
    private static final MPU9250.MagScale magScale = MPU9250.MagScale.MFS_16BIT;
    private static final MPU9250.GyrScale gyrScale = MPU9250.GyrScale.GFS_2000DPS;
    private static final MPU9250.AccScale accScale = MPU9250.AccScale.AFS_4G;

    private int lastRawMagX;
    private int lastRawMagY;
    private int lastRawMagZ;

    private final int sampleRate;

    private final float[] magCalibration = new float[3];
    private final float[] magBias = new float[3];
    private final float[] magScaling = new float[3]; // TODO investigate why unused
    private final float[] accelBias = new float[3];

    private final CircularArrayRing<TimestampedData3D> accel;
    private final CircularArrayRing<TimestampedData3D> gyro;
    private final CircularArrayRing<TimestampedData3D> mag;
    private final CircularArrayRing<Float> temp;

    ArrayList<SensorUpdateListener> listeners;

    private boolean paused;

    private final I2CDevice mpu9250;
    private final I2CDevice ak8963;

    public MPU9250_Pi4j() throws I2CFactory.UnsupportedBusNumberException, IOException, InterruptedException
    {
    	this(10,10);
    }
    
    public MPU9250_Pi4j(int sampleRate, int sampleSize) throws I2CFactory.UnsupportedBusNumberException, IOException, InterruptedException
    {
        this.sampleRate = sampleRate;
        this.paused = true;
        this.accel = new CircularArrayRing<>(sampleSize);
        this.gyro = new CircularArrayRing<>(sampleSize);
        this.mag = new CircularArrayRing<>(sampleSize);
        this.temp = new CircularArrayRing<>(sampleSize);
        // get device
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
        this.mpu9250 = bus.getDevice(0x68);
        this.ak8963 = bus.getDevice(0x0C);

        listeners = new ArrayList<>();
        Thread.sleep(2000);

        System.out.println(mpu9250.read(MPU9250.Registers.WHO_AM_I_MPU9250.getValue()));

        selfTest();
        calibrateGyroAcc();
        initMPU9250();
        initAK8963();
        //calibrateMag();


        this.paused = false;
    }

    private void calibrateMag() throws InterruptedException, IOException
    {
        int sample_count = 0;
        int mag_bias[] = {0, 0, 0}, mag_scale[] = {0, 0, 0};
        int mag_max[] = {0x8000, 0x8000, 0x8000}, mag_min[] = {0x7FFF, 0x7FFF, 0x7FFF}, mag_temp[] = {0, 0, 0};

        System.out.println("Mag Calibration: Wave device in a figure eight until done!");
        Thread.sleep(4000);

        // shoot for ~fifteen seconds of mag data
        if(MPU9250.Registers.M_MODE.getValue() == 0x02) sample_count = 128;  // at 8 Hz ODR, new mag data is available every 125 ms
        if(MPU9250.Registers.M_MODE.getValue() == 0x06) sample_count = 1500;  // at 100 Hz ODR, new mag data is available every 10 ms
        for(int ii = 0; ii < sample_count; ii++) {
            updateMagnetometerData();  // Read the mag data
            mag_temp[0] = lastRawMagX;
            mag_temp[1] = lastRawMagY;
            mag_temp[2] = lastRawMagZ;
            for (int jj = 0; jj < 3; jj++) {
                if(mag_temp[jj] > mag_max[jj]) mag_max[jj] = mag_temp[jj];
                if(mag_temp[jj] < mag_min[jj]) mag_min[jj] = mag_temp[jj];
            }
            if(MPU9250.Registers.M_MODE.getValue() == 0x02) Thread.sleep(135);  // at 8 Hz ODR, new mag data is available every 125 ms
            if(MPU9250.Registers.M_MODE.getValue() == 0x06) Thread.sleep(12);  // at 100 Hz ODR, new mag data is available every 10 ms
        }
        // Get hard iron correction
        mag_bias[0]  = (mag_max[0] + mag_min[0])/2;  // get average x mag bias in counts
        mag_bias[1]  = (mag_max[1] + mag_min[1])/2;  // get average y mag bias in counts
        mag_bias[2]  = (mag_max[2] + mag_min[2])/2;  // get average z mag bias in counts

        magBias[0] = (float) mag_bias[0]*magScale.getRes()*magCalibration[0];  // save mag biases in G for main program
        magBias[1] = (float) mag_bias[1]*magScale.getRes()*magCalibration[1];
        magBias[2] = (float) mag_bias[2]*magScale.getRes()*magCalibration[2];

        // Get soft iron correction estimate
        mag_scale[0]  = (mag_max[0] - mag_min[0])/2;  // get average x axis max chord length in counts
        mag_scale[1]  = (mag_max[1] - mag_min[1])/2;  // get average y axis max chord length in counts
        mag_scale[2]  = (mag_max[2] - mag_min[2])/2;  // get average z axis max chord length in counts

        float avg_rad = mag_scale[0] + mag_scale[1] + mag_scale[2];
        avg_rad /= 3.0;

        magScaling[0] = avg_rad/((float)mag_scale[0]);
        magScaling[1] = avg_rad/((float)mag_scale[1]);
        magScaling[2] = avg_rad/((float)mag_scale[2]);

        System.out.println("Mag Calibration done!");
    }

    private void initAK8963() throws InterruptedException, IOException
    {
        // First extract the factory calibration for each magnetometer axis
        byte rawData[] = new byte[3];  // x/y/z gyro calibration data stored here
        ak8963.write(MPU9250.Registers.AK8963_CNTL.getValue(),(byte) 0x00); // Power down magnetometer
        Thread.sleep(10);
        ak8963.write(MPU9250.Registers.AK8963_CNTL.getValue(), (byte)0x0F); // Enter Fuse ROM access mode
        Thread.sleep(10);
        ak8963.read(MPU9250.Registers.AK8963_ASAX.getValue(), rawData,0,3);  // Read the x-, y-, and z-axis calibration values
        magCalibration[0] =  (float)(rawData[0] - 128)/256f + 1f;   // Return x-axis sensitivity adjustment values, etc.
        magCalibration[1] =  (float)(rawData[1] - 128)/256f + 1f;
        magCalibration[2] =  (float)(rawData[2] - 128)/256f + 1f;
        ak8963.write(MPU9250.Registers.AK8963_CNTL.getValue(), (byte)0x00); // Power down magnetometer
        Thread.sleep(10);
        // Configure the magnetometer for continuous read and highest resolution
        // set Mscale bit 4 to 1 (0) to enable 16 (14) bit resolution in CNTL register,
        // and enable continuous mode data acquisition Mmode (bits [3:0]), 0010 for 8 Hz and 0110 for 100 Hz sample rates
        ak8963.write(MPU9250.Registers.AK8963_CNTL.getValue(), (byte)(magScale.getValue() << 4 | MPU9250.Registers.M_MODE.getValue())); // Set magnetometer data resolution and sample ODR
        Thread.sleep(10);
    }

    private void initMPU9250() throws IOException, InterruptedException
    {
        // wake up device
        // Clear sleep mode bit (6), enable all sensors
        mpu9250.write(MPU9250.Registers.PWR_MGMT_1.getValue(), (byte)0x00);
        Thread.sleep(100); // Wait for all registers to reset

        // get stable time source
        mpu9250.write(MPU9250.Registers.PWR_MGMT_1.getValue(), (byte)0x01);  // Auto select clock source to be PLL gyroscope reference if ready else
        Thread.sleep(200);

        // Configure Gyro and Thermometer
        // Disable FSYNC and set thermometer and gyro bandwidth to 41 and 42 Hz, respectively;
        // minimum delay time for this setting is 5.9 ms, which means sensor fusion update rates cannot
        // be higher than 1 / 0.0059 = 170 Hz
        // DLPF_CFG = bits 2:0 = 011; this limits the sample rate to 1000 Hz for both
        // With the MPU9250_Pi4j, it is possible to get gyro sample rates of 32 kHz (!), 8 kHz, or 1 kHz
        mpu9250.write(MPU9250.Registers.CONFIG.getValue(), (byte)0x03);

        // Set sample rate = gyroscope output rate/(1 + SMPLRT_DIV)
        mpu9250.write(MPU9250.Registers.SMPLRT_DIV.getValue(), (byte)0x04);  // Use a 200 Hz rate; a rate consistent with the filter update rate
        // determined inset in CONFIG above

        // Set gyroscope full scale range
        // Range selects FS_SEL and AFS_SEL are 0 - 3, so 2-bit values are left-shifted into positions 4:3
        byte c = (byte) mpu9250.read(MPU9250.Registers.GYRO_CONFIG.getValue()); // get current GYRO_CONFIG register value
        // c = c & ~0xE0; // Clear self-test bits [7:5]
        c = (byte)(c & ~0x02); // Clear Fchoice bits [1:0]
        c = (byte)(c & ~0x18); // Clear AFS bits [4:3]
        c = (byte)(c | gyrScale.getValue() << 3); // Set full scale range for the gyro
        // c =| 0x00; // Set Fchoice for the gyro to 11 by writing its inverse to bits 1:0 of GYRO_CONFIG
        mpu9250.write(MPU9250.Registers.GYRO_CONFIG.getValue(), c ); // Write new GYRO_CONFIG value to register

        // Set accelerometer full-scale range configuration
        c = (byte) mpu9250.read(MPU9250.Registers.ACCEL_CONFIG.getValue()); // get current ACCEL_CONFIG register value
        // c = c & ~0xE0; // Clear self-test bits [7:5]
        c = (byte)(c & ~0x18);  // Clear AFS bits [4:3]
        c = (byte)(c | accScale.getValue() << 3); // Set full scale range for the accelerometer
        mpu9250.write(MPU9250.Registers.ACCEL_CONFIG.getValue(), c); // Write new ACCEL_CONFIG register value

        // Set accelerometer sample rate configuration
        // It is possible to get a 4 kHz sample rate from the accelerometer by choosing 1 for
        // accel_fchoice_b bit [3]; in this case the bandwidth is 1.13 kHz
        c = (byte) mpu9250.read(MPU9250.Registers.ACCEL_CONFIG2.getValue()); // get current ACCEL_CONFIG2 register value
        c = (byte)(c & ~0x0F); // Clear accel_fchoice_b (bit 3) and A_DLPFG (bits [2:0])
        c = (byte)(c | 0x03);  // Set accelerometer rate to 1 kHz and bandwidth to 41 Hz
        mpu9250.write(MPU9250.Registers.ACCEL_CONFIG2.getValue(), c); // Write new ACCEL_CONFIG2 register value

        // The accelerometer, gyro, and thermometer are set to 1 kHz sample rates,
        // but all these rates are further reduced by a factor of 5 to 200 Hz because of the SMPLRT_DIV setting

        // Configure Interrupts and Bypass Enable
        // Set interrupt pin active high, push-pull, hold interrupt pin level HIGH until interrupt cleared,
        // clear on read of INT_STATUS, and enable I2C_BYPASS_EN so additional chips
        // can join the I2C bus and all can be controlled by the Arduino as master
        //   writeByte(MPU9250_ADDRESS, INT_PIN_CFG, 0x22);
        mpu9250.write(MPU9250.Registers.INT_PIN_CFG.getValue(), (byte)0x12);  // INT is 50 microsecond pulse and any read to clear
        mpu9250.write(MPU9250.Registers.INT_ENABLE.getValue(), (byte)0x01);  // Enable data ready (bit 0) interrupt
        Thread.sleep(100);
    }

    private void calibrateGyroAcc() throws IOException, InterruptedException
    {
        // Write a one to bit 7 reset bit; toggle reset device
        mpu9250.write(MPU9250.Registers.PWR_MGMT_1.getValue(),(byte)0x80);
        Thread.sleep(100);

        // get stable time source; Auto select clock source to be PLL gyroscope reference if ready
        // else use the internal oscillator, bits 2:0 = 001
        mpu9250.write(MPU9250.Registers.PWR_MGMT_1.getValue(),(byte)0x01);
        mpu9250.write(MPU9250.Registers.PWR_MGMT_2.getValue(),(byte)0x00);
        Thread.sleep(200);


        // Configure device for bias calculation
        mpu9250.write(MPU9250.Registers.INT_ENABLE.getValue(),(byte) 0x00);   // Disable all interrupts
        mpu9250.write(MPU9250.Registers.FIFO_EN.getValue(),(byte) 0x00);      // Disable FIFO
        mpu9250.write(MPU9250.Registers.PWR_MGMT_1.getValue(),(byte) 0x00);   // Turn on internal clock source
        mpu9250.write(MPU9250.Registers.I2C_MST_CTRL.getValue(),(byte) 0x00); // Disable I2C master
        mpu9250.write(MPU9250.Registers.USER_CTRL.getValue(),(byte) 0x00);    // Disable FIFO and I2C master modes
        mpu9250.write(MPU9250.Registers.USER_CTRL.getValue(),(byte) 0x0C);    // Reset FIFO and DMP
        Thread.sleep(15);

        // Configure MPU6050 gyro and accelerometer for bias calculation
        mpu9250.write(MPU9250.Registers.CONFIG.getValue(),(byte) 0x01);       // Set low-pass filter to 188 Hz
        mpu9250.write(MPU9250.Registers.SMPLRT_DIV.getValue(),(byte) 0x00);   // Set sample rate to 1 kHz
        mpu9250.write(MPU9250.Registers.GYRO_CONFIG.getValue(),(byte) 0x00);  // Set gyro full-scale to 250 degrees per second, maximum sensitivity
        mpu9250.write(MPU9250.Registers.ACCEL_CONFIG.getValue(),(byte) 0x00); // Set accelerometer full-scale to 2 g, maximum sensitivity

        int gyrosensitivity = 131;     // = 131 LSB/degrees/sec
        int accelSensitivity = 16384;  // = 16384 LSB/g

        // Configure FIFO to capture accelerometer and gyro data for bias calculation
        mpu9250.write(MPU9250.Registers.USER_CTRL.getValue(),(byte) 0x40);   // Enable FIFO
        mpu9250.write(MPU9250.Registers.FIFO_EN.getValue(),(byte) 0x78);     // Enable gyro and accelerometer sensors for FIFO  (max size 512 bytes in MPU-9150)
        Thread.sleep(40); // accumulate 40 samples in 40 milliseconds = 480 bytes

        // At end of sample accumulation, turn off FIFO sensor read
        mpu9250.write(MPU9250.Registers.FIFO_EN.getValue(),(byte) 0x00);        // Disable gyro and accelerometer sensors for FIFO
        byte[] buffer = new byte[2];
        mpu9250.read(MPU9250.Registers.FIFO_COUNTH.getValue(),buffer,0,2); // read FIFO sample count

        int packetCount = (buffer[0] << 8) | buffer[1];
        packetCount /= 12;

        buffer = new byte[12];
        int[] accelBiasl = new int[]{0,0,0};
        int[] gyroBias = new int[]{0,0,0};

        for(int s = 0; s < packetCount; s++)
        {
            mpu9250.read(MPU9250.Registers.FIFO_R_W.getValue(),buffer,0,12); // read FIFO sample count

            accelBiasl[0] += (buffer[0] << 8) | buffer[1];
            accelBiasl[1] += (buffer[2] << 8) | buffer[3];
            accelBiasl[2] += (buffer[4] << 8) | buffer[5];

            gyroBias[0] += (buffer[6] << 8) | buffer[7];
            gyroBias[1] += (buffer[8] << 8) | buffer[9];
            gyroBias[2] += (buffer[10] << 8) | buffer[11];
        }


        accelBiasl[0] /= packetCount;
        accelBiasl[1] /= packetCount;
        accelBiasl[2] /= packetCount;

        gyroBias[0] /= packetCount;
        gyroBias[1] /= packetCount;
        gyroBias[2] /= packetCount;

        if(accelBiasl[2] > 0L) {accelBiasl[2] -= accelSensitivity;}  // Remove gravity from the z-axis accelerometer bias calculation
        else {accelBiasl[2] += accelSensitivity;}


        // Construct the gyro biases for push to the hardware gyro bias registers, which are reset to zero upon device startup
        buffer[0] = (byte)((-gyroBias[0]/4  >> 8) & 0xFF); // Divide by 4 to get 32.9 LSB per deg/s to conform to expected bias input format
        buffer[1] = (byte)((-gyroBias[0]/4)       & 0xFF); // Biases are additive, so change sign on calculated average gyro biases
        buffer[2] = (byte)((-gyroBias[1]/4  >> 8) & 0xFF);
        buffer[3] = (byte)((-gyroBias[1]/4)       & 0xFF);
        buffer[4] = (byte)((-gyroBias[2]/4  >> 8) & 0xFF);
        buffer[5] = (byte)((-gyroBias[2]/4)       & 0xFF);


        // Push gyro biases to hardware registers
        mpu9250.write(MPU9250.Registers.XG_OFFSET_H.getValue(), buffer[0]);
        mpu9250.write(MPU9250.Registers.XG_OFFSET_L.getValue(), buffer[1]);
        mpu9250.write(MPU9250.Registers.YG_OFFSET_H.getValue(), buffer[2]);
        mpu9250.write(MPU9250.Registers.YG_OFFSET_L.getValue(), buffer[3]);
        mpu9250.write(MPU9250.Registers.ZG_OFFSET_H.getValue(), buffer[4]);
        mpu9250.write(MPU9250.Registers.ZG_OFFSET_L.getValue(), buffer[5]);

        int[] accelBiasReg = new int[]{0,0,0};
        mpu9250.read(MPU9250.Registers.XA_OFFSET_H.getValue(),buffer,0,2);
        accelBiasReg[0] = (buffer[0] << 8) | buffer[1];
        mpu9250.read(MPU9250.Registers.YA_OFFSET_H.getValue(),buffer,0,2);
        accelBiasReg[1] = (buffer[0] << 8) | buffer[1];
        mpu9250.read(MPU9250.Registers.ZA_OFFSET_H.getValue(),buffer,0,2);
        accelBiasReg[2] = (buffer[0] << 8) | buffer[1];


        int mask = 1; // Define mask for temperature compensation bit 0 of lower byte of accelerometer bias registers
        int[] mask_bit = new int[]{0, 0, 0}; // Define array to hold mask bit for each accelerometer bias axis

        for(int s = 0; s < 3; s++) {
            if((accelBiasReg[s] & mask)==1) mask_bit[s] = 0x01; // If temperature compensation bit is set, record that fact in mask_bit
        }

        // Construct total accelerometer bias, including calculated average accelerometer bias from above
        accelBiasReg[0] -= (accelBiasl[0]/8); // Subtract calculated averaged accelerometer bias scaled to 2048 LSB/g (16 g full scale)
        accelBiasReg[1] -= (accelBiasl[1]/8);
        accelBiasReg[2] -= (accelBiasl[2]/8);

        buffer[0] = (byte)((accelBiasReg[0] >> 8) & 0xFF);
        buffer[1] = (byte)((accelBiasReg[0])      & 0xFF);
        buffer[1] = (byte)(buffer[1] | mask_bit[0]); // preserve temperature compensation bit when writing back to accelerometer bias registers
        buffer[2] = (byte)((accelBiasReg[1] >> 8) & 0xFF);
        buffer[3] = (byte)((accelBiasReg[1])      & 0xFF);
        buffer[3] = (byte)(buffer[3] | mask_bit[1]); // preserve temperature compensation bit when writing back to accelerometer bias registers
        buffer[4] = (byte)((accelBiasReg[2] >> 8) & 0xFF);
        buffer[5] = (byte)((accelBiasReg[2])      & 0xFF);
        buffer[5] = (byte)(buffer[5] | mask_bit[2]); // preserve temperature compensation bit when writing back to accelerometer bias registers

        // Apparently this is not working for the acceleration biases in the MPU-9250
        // Are we handling the temperature correction bit properly?
        // Push accelerometer biases to hardware registers
        /*mpu9250.write(XA_OFFSET_H.getValue(), buffer[0]);
        mpu9250.write(XA_OFFSET_L.getValue(), buffer[1]);
        mpu9250.write(YA_OFFSET_H.getValue(), buffer[2]);
        mpu9250.write(YA_OFFSET_L.getValue(), buffer[3]);
        mpu9250.write(ZA_OFFSET_H.getValue(), buffer[4]);
        mpu9250.write(ZA_OFFSET_L.getValue(), buffer[5]);
        */


        accelBias[0] = (float)accelBiasl[0]/(float)accelSensitivity;
        accelBias[1] = (float)accelBiasl[1]/(float)accelSensitivity;
        accelBias[2] = (float)accelBiasl[2]/(float)accelSensitivity;

    }

    /**
     * Accelerometer and gyroscope self test; check calibration wrt factory settings
     * calculates & prints percent deviation from factory trim values, +/- 14 or less deviation is a pass
     * @throws IOException
     * @throws InterruptedException
     */
    private void selfTest() throws IOException, InterruptedException
    {

        byte FS = 0;
        int bytesRead =0;

        mpu9250.write(MPU9250.Registers.SMPLRT_DIV.getValue(),(byte)0x00); // Set gyro sample rate to 1 kHz
        Thread.sleep(2);
        mpu9250.write(MPU9250.Registers.CONFIG.getValue(),(byte)0x02); // Set gyro sample rate to 1 kHz and DLPF to 92 Hz
        Thread.sleep(2);
        // Set full scale range for the gyro to 250 dps
        mpu9250.write(MPU9250.Registers.GYRO_CONFIG.getValue(),(byte)(1<<FS));//GyrScale.GFS_250DPS.getValue());
        Thread.sleep(2);
        // Set accelerometer rate to 1 kHz and bandwidth to 92 Hz
        mpu9250.write(MPU9250.Registers.ACCEL_CONFIG2.getValue(),(byte)0x02);
        Thread.sleep(2);
        // Set full scale range for the accelerometer to 2 g
        mpu9250.write(MPU9250.Registers.ACCEL_CONFIG.getValue(),(byte)(1<<FS));// AccScale.AFS_2G.getValue());
        Thread.sleep(2);

        final int TEST_LENGTH = 200;
        byte[] buffer = new byte[]{0,0,0,0,0,0};

        short[] aAvg = new short[3];
        short[] gAvg = new short[3];

        for(int s=0; s<TEST_LENGTH; s++)
        {
            bytesRead = mpu9250.read(MPU9250.Registers.ACCEL_XOUT_H.getValue(),buffer,0,6);
            System.out.print("ACCEL_XOUT_H read - "+ bytesRead+":");
            System.out.println(" "+bytesToHex(buffer));
            aAvg[0] += ((buffer[0] << 8) | buffer[1]);
            aAvg[1] += ((buffer[2] << 8) | buffer[3]);
            aAvg[2] += ((buffer[4] << 8) | buffer[5]);
            Thread.sleep(2);

            buffer = new byte[]{0,0,0,0,0,0};
            bytesRead = mpu9250.read(MPU9250.Registers.GYRO_XOUT_H.getValue(),buffer,0,6);
            System.out.print("GYRO_XOUT_H  read - "+ bytesRead+":");
            System.out.println(" "+bytesToHex(buffer));
            gAvg[0] += ((buffer[0] << 8) | buffer[1]);
            gAvg[1] += ((buffer[2] << 8) | buffer[3]);
            gAvg[2] += ((buffer[4] << 8) | buffer[5]);
            Thread.sleep(2);
            buffer = new byte[]{0,0,0,0,0,0};
        }

        for(int i = 0; i<3; i++)
        {
            aAvg[i] /= TEST_LENGTH;
            gAvg[i] /= TEST_LENGTH;
        }

        mpu9250.write(MPU9250.Registers.ACCEL_CONFIG.getValue(), (byte)0xE0);
        Thread.sleep(2);
        mpu9250.write(MPU9250.Registers.GYRO_CONFIG.getValue(), (byte)0xE0);
        Thread.sleep(2);

        int[] aSTAvg = new int[3];
        int[] gSTAvg = new int[3];

        for(int s=0; s<TEST_LENGTH; s++)
        {
            mpu9250.read(MPU9250.Registers.GYRO_XOUT_H.getValue(),buffer,0,6);
            aSTAvg[0] += ((buffer[0] << 8) | buffer[1]);
            aSTAvg[1] += ((buffer[2] << 8) | buffer[3]);
            aSTAvg[2] += ((buffer[4] << 8) | buffer[5]);
            buffer = new byte[]{0,0,0,0,0,0};
            Thread.sleep(2);

            mpu9250.read(MPU9250.Registers.ACCEL_XOUT_H.getValue(),buffer,0,6);
            gSTAvg[0] += ((buffer[0] << 8) | buffer[1]);
            gSTAvg[1] += ((buffer[2] << 8) | buffer[3]);
            gSTAvg[2] += ((buffer[4] << 8) | buffer[5]);
            buffer = new byte[]{0,0,0,0,0,0};
            Thread.sleep(2);
        }

        for(int i = 0; i<3; i++)
        {
            aSTAvg[i] /= TEST_LENGTH;
            gSTAvg[i] /= TEST_LENGTH;
        }

        Thread.sleep(2);
        mpu9250.write(MPU9250.Registers.GYRO_CONFIG.getValue(), MPU9250.GyrScale.GFS_250DPS.getValue());
        Thread.sleep(2);
        mpu9250.write(MPU9250.Registers.ACCEL_CONFIG.getValue(), MPU9250.AccScale.AFS_2G.getValue());
        Thread.sleep(25);

        int[] selfTest = new int[6];

        selfTest[0] = mpu9250.read(MPU9250.Registers.SELF_TEST_X_ACCEL.getValue());
        Thread.sleep(2);
        selfTest[1] = mpu9250.read(MPU9250.Registers.SELF_TEST_Y_ACCEL.getValue());
        Thread.sleep(2);
        selfTest[2] = mpu9250.read(MPU9250.Registers.SELF_TEST_Z_ACCEL.getValue());
        Thread.sleep(2);
        selfTest[3] = mpu9250.read(MPU9250.Registers.SELF_TEST_X_GYRO.getValue());
        Thread.sleep(2);
        selfTest[4] = mpu9250.read(MPU9250.Registers.SELF_TEST_Y_GYRO.getValue());
        Thread.sleep(2);
        selfTest[5] = mpu9250.read(MPU9250.Registers.SELF_TEST_Z_GYRO.getValue());
        Thread.sleep(2);

        float[] factoryTrim = new float[6];

        factoryTrim[0] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTest[0] - 1f);
        factoryTrim[1] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTest[1] - 1f);
        factoryTrim[2] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTest[2] - 1f);
        factoryTrim[3] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTest[3] - 1f);
        factoryTrim[4] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTest[4] - 1f);
        factoryTrim[5] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTest[5] - 1f);

        float aXAccuracy = 100*((float)(aSTAvg[0] - aAvg[0]))/factoryTrim[0];
        float aYAccuracy = 100*((float)(aSTAvg[1] - aAvg[1]))/factoryTrim[1];
        float aZAccuracy = 100*((float)(aSTAvg[2] - aAvg[2]))/factoryTrim[2];

        System.out.println("Accelerometer accuracy:(% away from factory values)");
        System.out.println("x: " + aXAccuracy + "%");
        System.out.println("y: " + aYAccuracy + "%");
        System.out.println("z: " + aZAccuracy + "%");
        System.out.println("Gyroscope accuracy:(% away from factory values)");
        System.out.println("x: " + 100.0*((float)(gSTAvg[0] - gAvg[0]))/factoryTrim[3] + "%");
        System.out.println("y: " + 100.0*((float)(gSTAvg[1] - gAvg[1]))/factoryTrim[4] + "%");
        System.out.println("z: " + 100.0*((float)(gSTAvg[2] - gAvg[2]))/factoryTrim[5] + "%");
    }


    @Override
    public void run()
    {
        long lastTime;
        final long waitTime = 1000000000L /sampleRate;
        while(!Thread.interrupted())
        {
            if(!paused)
            {
                try
                {
                    lastTime = System.nanoTime();
                    updateMagnetometerData();
                    updateAccelerometerData();
                    updateGyroscopeData();
                    updateThermometerData();
                    Thread.sleep(100);


                    for(SensorUpdateListener listener:listeners) listener.dataUpdated();

                    while(System.nanoTime() - lastTime < waitTime);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public static final byte[] intToByteArray(short value) {
        return new byte[] {
                (byte)(value >>> 8),
                (byte)value};
    }

    public void pause()
    {
        paused = true;
    }

    public void unpause()
    {
        paused = false;
    }

    @Override
    public TimestampedData3D getLatestAcceleration()
    {
        return accel.get(0);
    }

    @Override
    public TimestampedData3D getAcceleration(int i)
    {
        return accel.get(i);
    }

    @Override
    public int getAccelerometerReadingCount()
    {
        return gyro.size();
    }

    @Override
    public TimestampedData3D getLatestRotationalAcceleration()
    {
        return gyro.get(0);
    }

    @Override
    public TimestampedData3D getRotationalAcceleration(int i)
    {
        return gyro.get(i);
    }

    @Override
    public int getGyroscopeReadingCount() {
        return gyro.size();
    }

    @Override
    public TimestampedData3D getLatestGaussianData()
    {
        return mag.get(0);
    }

    @Override
    public TimestampedData3D getGaussianData(int i)
    {
        return mag.get(i);
    }

    @Override
    public int getMagnetometerReadingCount() {
        return mag.size();
    }


    @Override
    public float getLatestTemperature()
    {
        return temp.get(0);
    }

    @Override
    public float getTemperature(int i)
    {
        return temp.get(i);
    }

    @Override
    public int getThermometerReadingCount() {
        return temp.size();
    }

    @Override
    public void updateThermometerData() throws Exception
    {
        byte rawData[] = new byte[2];  // x/y/z gyro register data stored here
        mpu9250.read(MPU9250.Registers.TEMP_OUT_H.getValue(),rawData,0,2);  // Read the two raw data registers sequentially into data array
        mpu9250.read(MPU9250.Registers.TEMP_OUT_H.getValue(),rawData,0,2);  // Read again to trigger
        temp.add((float)((rawData[0] << 8) | rawData[1]));  // Turn the MSB and LSB into a 16-bit value
    }

    @Override
    public void updateMagnetometerData() throws IOException
    {
        int newMagData = (ak8963.read(MPU9250.Registers.AK8963_ST1.getValue()) & 0x01);
        if (newMagData == 0) return;
        byte[] buffer = new byte[7];
        ak8963.read(MPU9250.Registers.AK8963_ST1.getValue(), buffer,0,7);

        byte c = buffer[6];
        if((c & 0x08) == 0)
        { // Check if magnetic sensor overflow set, if not then report data
            lastRawMagX = (buffer[1] << 8) | buffer[0];
            lastRawMagY = (buffer[3] << 8) | buffer[2];
            lastRawMagZ = (buffer[5] << 8) | buffer[4];
            float x=lastRawMagX,y=lastRawMagY,z=lastRawMagZ;

            x *= magScale.getRes()*magCalibration[0];
            y *= magScale.getRes()*magCalibration[1];
            z *= magScale.getRes()*magCalibration[2];

            x -= magBias[0];
            y -= magBias[1];
            z -= magBias[2];

            mag.add(new TimestampedData3D(x,y,z));
        }
        ak8963.read(0x09);
    }

    @Override
    public void updateGyroscopeData() throws IOException
    {
        float x,y,z;
        byte rawData[] = new byte[6];  // x/y/z gyro register data stored here
        mpu9250.read(MPU9250.Registers.GYRO_XOUT_H.getValue(), rawData,0,6);  // Read the six raw data registers sequentially into data array
        mpu9250.read(MPU9250.Registers.GYRO_XOUT_H.getValue(), rawData,0,6);  // Read again to trigger
        x = (rawData[0] << 8) | rawData[1] ;  // Turn the MSB and LSB into a signed 16-bit value
        y = (rawData[2] << 8) | rawData[3] ;
        z = (rawData[4] << 8) | rawData[5] ;

        //System.out.println("Gyroscope " + x + ", " + y + ", " + z);

        x *= gyrScale.getRes(); // transform from raw data to degrees/s
        y *= gyrScale.getRes(); // transform from raw data to degrees/s
        z *= gyrScale.getRes(); // transform from raw data to degrees/s

        gyro.add(new TimestampedData3D(x,y,z));
    }

    @Override
    public void updateAccelerometerData() throws IOException
    {
        float x,y,z;
        byte rawData[] = new byte[6];  // x/y/z gyro register data stored here
        mpu9250.read(MPU9250.Registers.ACCEL_XOUT_H.getValue(), rawData,0,6);  // Read the six raw data registers sequentially into data array
        mpu9250.read(MPU9250.Registers.ACCEL_XOUT_H.getValue(), rawData,0,6);  // Read again to trigger
        x = (rawData[0] << 8) | rawData[1] ;  // Turn the MSB and LSB into a signed 16-bit value
        y = (rawData[2] << 8) | rawData[3] ;
        z = (rawData[4] << 8) | rawData[5] ;

        System.out.println("Accelerometer " + x + ", " + y + ", " + z);

        x *= gyrScale.getRes(); // transform from raw data to degrees/s
        y *= gyrScale.getRes(); // transform from raw data to degrees/s
        z *= gyrScale.getRes(); // transform from raw data to degrees/s

        x -= accelBias[0];
        y -= accelBias[1];
        z -= accelBias[2];

        accel.add(new TimestampedData3D(x,y,z));
    }
    
    public void registerInterest(SensorUpdateListener listener)
    {
    	listeners.add(listener);
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }
}