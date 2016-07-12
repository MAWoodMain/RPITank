package sensors.MPU9250;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import sensors.dataTypes.CircularArrayRing;
import sensors.dataTypes.TimestampedData3D;
import sensors.interfaces.Accelerometer;
import sensors.interfaces.Gyroscope;
import sensors.interfaces.Magnetometer;
import sensors.interfaces.Thermometer;

import java.io.IOException;

import static sensors.MPU9250.Registers.*;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public class MPU9250 implements Accelerometer, Gyroscope, Magnetometer, Thermometer, Runnable
{
    private static final MagScale magScale = MagScale.MFS_16BIT;
    private static final GyrScale gyrScale = GyrScale.GFS_2000DPS;
    private static final AccScale accScale = AccScale.AFS_4G;

    private CircularArrayRing<TimestampedData3D> accel;
    private CircularArrayRing<TimestampedData3D> gyro;
    private CircularArrayRing<TimestampedData3D> mag;
    private CircularArrayRing<Float> temp;

    private boolean paused;

    private final I2CDevice mpu9250;

    public MPU9250(int address) throws I2CFactory.UnsupportedBusNumberException, IOException, InterruptedException
    {
        paused = true;
        accel = new CircularArrayRing<>();
        gyro = new CircularArrayRing<>();
        mag = new CircularArrayRing<>();
        temp = new CircularArrayRing<>();
        // get device
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
        mpu9250 = bus.getDevice(address);

        selfTest();
        calibrateGyroAcc();
        //initMPU9250();
        //initAK8963();
        //calibrateMag();



        paused = false;
    }

    private void calibrateGyroAcc() throws IOException, InterruptedException
    {
        // Write a one to bit 7 reset bit; toggle reset device
        mpu9250.write(PWR_MGMT_1.getValue(),(byte)0x80);
        Thread.sleep(100);

        // get stable time source; Auto select clock source to be PLL gyroscope reference if ready
        // else use the internal oscillator, bits 2:0 = 001
        mpu9250.write(PWR_MGMT_1.getValue(),(byte)0x01);
        mpu9250.write(PWR_MGMT_2.getValue(),(byte)0x00);
        Thread.sleep(200);


        // Configure device for bias calculation
        mpu9250.write(INT_ENABLE.getValue(),(byte) 0x00);   // Disable all interrupts
        mpu9250.write(FIFO_EN.getValue(),(byte) 0x00);      // Disable FIFO
        mpu9250.write(PWR_MGMT_1.getValue(),(byte) 0x00);   // Turn on internal clock source
        mpu9250.write(I2C_MST_CTRL.getValue(),(byte) 0x00); // Disable I2C master
        mpu9250.write(USER_CTRL.getValue(),(byte) 0x00);    // Disable FIFO and I2C master modes
        mpu9250.write(USER_CTRL.getValue(),(byte) 0x0C);    // Reset FIFO and DMP
        Thread.sleep(15);

        // Configure MPU6050 gyro and accelerometer for bias calculation
        mpu9250.write(CONFIG.getValue(),(byte) 0x01);       // Set low-pass filter to 188 Hz
        mpu9250.write(SMPLRT_DIV.getValue(),(byte) 0x00);   // Set sample rate to 1 kHz
        mpu9250.write(GYRO_CONFIG.getValue(),(byte) 0x00);  // Set gyro full-scale to 250 degrees per second, maximum sensitivity
        mpu9250.write(ACCEL_CONFIG.getValue(),(byte) 0x00); // Set accelerometer full-scale to 2 g, maximum sensitivity

        int gyrosensitivity = 131;     // = 131 LSB/degrees/sec
        int accelSensitivity = 16384;  // = 16384 LSB/g

        // Configure FIFO to capture accelerometer and gyro data for bias calculation
        mpu9250.write(USER_CTRL.getValue(),(byte) 0x40);   // Enable FIFO
        mpu9250.write(FIFO_EN.getValue(),(byte) 0x78);     // Enable gyro and accelerometer sensors for FIFO  (max size 512 bytes in MPU-9150)
        Thread.sleep(40); // accumulate 40 samples in 40 milliseconds = 480 bytes

        // At end of sample accumulation, turn off FIFO sensor read
        mpu9250.write(FIFO_EN.getValue(),(byte) 0x00);        // Disable gyro and accelerometer sensors for FIFO
        byte[] buffer = new byte[2];
        mpu9250.read(FIFO_COUNTH.getValue(),buffer,0,2); // read FIFO sample count

        int packetCount = (buffer[0] << 8) | buffer[1];
        packetCount /= 12;

        buffer = new byte[12];
        int[] accelBias = new int[]{0,0,0};
        int[] gyroBias = new int[]{0,0,0};

        for(int s = 0; s < packetCount; s++)
        {
            mpu9250.read(FIFO_R_W.getValue(),buffer,0,12); // read FIFO sample count

            accelBias[0] += (buffer[0] << 8) | buffer[1];
            accelBias[1] += (buffer[2] << 8) | buffer[3];
            accelBias[2] += (buffer[4] << 8) | buffer[5];

            gyroBias[0] += (buffer[6] << 8) | buffer[7];
            gyroBias[1] += (buffer[8] << 8) | buffer[9];
            gyroBias[2] += (buffer[10] << 8) | buffer[11];
        }


        accelBias[0] /= packetCount;
        accelBias[1] /= packetCount;
        accelBias[2] /= packetCount;

        gyroBias[0] /= packetCount;
        gyroBias[1] /= packetCount;
        gyroBias[2] /= packetCount;

        if(accelBias[2] > 0L) {accelBias[2] -= accelSensitivity;}  // Remove gravity from the z-axis accelerometer bias calculation
        else {accelBias[2] += accelSensitivity;}


        // Construct the gyro biases for push to the hardware gyro bias registers, which are reset to zero upon device startup
        buffer[0] = (byte)((-gyroBias[0]/4  >> 8) & 0xFF); // Divide by 4 to get 32.9 LSB per deg/s to conform to expected bias input format
        buffer[1] = (byte)((-gyroBias[0]/4)       & 0xFF); // Biases are additive, so change sign on calculated average gyro biases
        buffer[2] = (byte)((-gyroBias[1]/4  >> 8) & 0xFF);
        buffer[3] = (byte)((-gyroBias[1]/4)       & 0xFF);
        buffer[4] = (byte)((-gyroBias[2]/4  >> 8) & 0xFF);
        buffer[5] = (byte)((-gyroBias[2]/4)       & 0xFF);


        // Push gyro biases to hardware registers
        mpu9250.write(XG_OFFSET_H.getValue(), buffer[0]);
        mpu9250.write(XG_OFFSET_L.getValue(), buffer[1]);
        mpu9250.write(YG_OFFSET_H.getValue(), buffer[2]);
        mpu9250.write(YG_OFFSET_L.getValue(), buffer[3]);
        mpu9250.write(ZG_OFFSET_H.getValue(), buffer[4]);
        mpu9250.write(ZG_OFFSET_L.getValue(), buffer[5]);

        int[] accelBiasReg = new int[]{0,0,0};
        mpu9250.read(XA_OFFSET_H.getValue(),buffer,0,2);
        accelBiasReg[0] = (buffer[0] << 8) | buffer[1];
        mpu9250.read(YA_OFFSET_H.getValue(),buffer,0,2);
        accelBiasReg[1] = (buffer[0] << 8) | buffer[1];
        mpu9250.read(ZA_OFFSET_H.getValue(),buffer,0,2);
        accelBiasReg[2] = (buffer[0] << 8) | buffer[1];


        int mask = 1; // Define mask for temperature compensation bit 0 of lower byte of accelerometer bias registers
        int[] mask_bit = new int[]{0, 0, 0}; // Define array to hold mask bit for each accelerometer bias axis

        for(int s = 0; s < 3; s++) {
            if((accelBiasReg[s] & mask)==1) mask_bit[s] = 0x01; // If temperature compensation bit is set, record that fact in mask_bit
        }

        // Construct total accelerometer bias, including calculated average accelerometer bias from above
        accelBiasReg[0] -= (accelBias[0]/8); // Subtract calculated averaged accelerometer bias scaled to 2048 LSB/g (16 g full scale)
        accelBiasReg[1] -= (accelBias[1]/8);
        accelBiasReg[2] -= (accelBias[2]/8);

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
        mpu9250.write(XA_OFFSET_H.getValue(), buffer[0]);
        mpu9250.write(XA_OFFSET_L.getValue(), buffer[1]);
        mpu9250.write(YA_OFFSET_H.getValue(), buffer[2]);
        mpu9250.write(YA_OFFSET_L.getValue(), buffer[3]);
        mpu9250.write(ZA_OFFSET_H.getValue(), buffer[4]);
        mpu9250.write(ZA_OFFSET_L.getValue(), buffer[5]);

    }

    private void selfTest() throws IOException, InterruptedException
    {
        // Set gyro sample rate to 1 kHz
        mpu9250.write(SMPLRT_DIV.getValue(),(byte)0x00);
        // Set gyro sample rate to 1 kHz and DLPF to 92 Hz
        mpu9250.write(CONFIG.getValue(),(byte)0x02);
        // Set full scale range for the gyro to 250 dps
        mpu9250.write(GYRO_CONFIG.getValue(),GyrScale.GFS_250DPS.getValue());
        // Set accelerometer rate to 1 kHz and bandwidth to 92 Hz
        mpu9250.write(ACCEL_CONFIG2.getValue(),(byte)0x02);
        // Set full scale range for the accelerometer to 2 g
        mpu9250.write(ACCEL_CONFIG.getValue(), AccScale.AFS_2G.getValue());

        final int TEST_LENGTH = 200;
        byte[] buffer = new byte[]{0,0,0,0,0,0};
        int ax,ay,az,gx,gy,gz;
        ax=ay=az=gx=gy=gz=0;

        for(int s=0; s<TEST_LENGTH; s++)
        {
            mpu9250.read(ACCEL_XOUT_H.getValue(),buffer,0,6);
            ax += ((buffer[0] << 8) | buffer[1]);
            ay += ((buffer[2] << 8) | buffer[3]);
            az += ((buffer[4] << 8) | buffer[5]);

            mpu9250.read(GYRO_XOUT_H.getValue(),buffer,0,6);
            gx += ((buffer[0] << 8) | buffer[1]);
            gy += ((buffer[2] << 8) | buffer[3]);
            gz += ((buffer[4] << 8) | buffer[5]);
        }

        int[] aAvg = new int[]{ax/TEST_LENGTH, ay/TEST_LENGTH, az/TEST_LENGTH};
        int[] gAvg = new int[]{gx/TEST_LENGTH, gy/TEST_LENGTH, gz/TEST_LENGTH};

        mpu9250.write(ACCEL_CONFIG.getValue(), (byte)0xE0);
        mpu9250.write(GYRO_CONFIG.getValue(), (byte)0xE0);
        Thread.sleep(25);

        ax=ay=az=gx=gy=gz=0;

        for(int s=0; s<TEST_LENGTH; s++)
        {
            mpu9250.read(GYRO_XOUT_H.getValue(),buffer,0,6);
            gx += ((buffer[0] << 8) | buffer[1]);
            gy += ((buffer[2] << 8) | buffer[3]);
            gz += ((buffer[4] << 8) | buffer[5]);

            mpu9250.read(ACCEL_XOUT_H.getValue(),buffer,0,6);
            ax += ((buffer[0] << 8) | buffer[1]);
            ay += ((buffer[2] << 8) | buffer[3]);
            az += ((buffer[4] << 8) | buffer[5]);
        }

        int[] aSTAvg = new int[]{ax/TEST_LENGTH, ay/TEST_LENGTH, az/TEST_LENGTH};
        int[] gSTAvg = new int[]{gx/TEST_LENGTH, gy/TEST_LENGTH, gz/TEST_LENGTH};

        mpu9250.write(GYRO_CONFIG.getValue(),GyrScale.GFS_250DPS.getValue());
        mpu9250.write(ACCEL_CONFIG.getValue(), AccScale.AFS_2G.getValue());
        Thread.sleep(25);

        int[] selfTest = new int[6];

        selfTest[0] = mpu9250.read(SELF_TEST_X_ACCEL.getValue());
        selfTest[1] = mpu9250.read(SELF_TEST_Y_ACCEL.getValue());
        selfTest[2] = mpu9250.read(SELF_TEST_Z_ACCEL.getValue());
        selfTest[3] = mpu9250.read(SELF_TEST_X_GYRO.getValue());
        selfTest[4] = mpu9250.read(SELF_TEST_Y_GYRO.getValue());
        selfTest[5] = mpu9250.read(SELF_TEST_Z_GYRO.getValue());

        float[] factoryTrim = new float[6];

        factoryTrim[0] = (float)(2620/(1<<0))*(float)Math.pow(1.01,(float)selfTest[0] - 1.0);
        factoryTrim[1] = (float)(2620/(1<<0))*(float)Math.pow(1.01,(float)selfTest[1] - 1.0);
        factoryTrim[2] = (float)(2620/(1<<0))*(float)Math.pow(1.01,(float)selfTest[2] - 1.0);
        factoryTrim[3] = (float)(2620/(1<<0))*(float)Math.pow(1.01,(float)selfTest[3] - 1.0);
        factoryTrim[4] = (float)(2620/(1<<0))*(float)Math.pow(1.01,(float)selfTest[4] - 1.0);
        factoryTrim[5] = (float)(2620/(1<<0))*(float)Math.pow(1.01,(float)selfTest[5] - 1.0);

        System.out.println("Accelerometer accuracy:(% away from factory values)");
        System.out.println("x: " + 100.0*((float)(aSTAvg[0] - aAvg[0]))/factoryTrim[0] + "%");
        System.out.println("y: " + 100.0*((float)(aSTAvg[1] - aAvg[1]))/factoryTrim[1] + "%");
        System.out.println("z: " + 100.0*((float)(aSTAvg[2] - aAvg[2]))/factoryTrim[2] + "%");
        System.out.println("Gyroscope accuracy:(% away from factory values)");
        System.out.println("x: " + 100.0*((float)(gSTAvg[0] - gAvg[0]))/factoryTrim[3] + "%");
        System.out.println("y: " + 100.0*((float)(gSTAvg[1] - gAvg[1]))/factoryTrim[4] + "%");
        System.out.println("z: " + 100.0*((float)(gSTAvg[2] - gAvg[2]))/factoryTrim[5] + "%");
    }

    public void updateData()
    {

    }

    private int getData(int address) throws IOException
    {
        byte high = (byte)mpu9250.read(address);
        byte low = (byte)mpu9250.read(address + 1);
        return (high<<8 | low); // construct 16 bit integer from two bytes
    }

    @Override
    public void run()
    {
        while(!Thread.interrupted())
        {
            if(!paused) updateData();
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
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
    public float getHeading()
    {
        //TODO: derive heading from Gaussian data
        return 0;
    }

    @Override
    public float getMaxGauss()
    {
        return magScale.getMinMax();
    }

    @Override
    public float getMinGauss()
    {
        return magScale.getMinMax();
    }

    @Override
    public float getMaxRotationalAcceleration()
    {
        return gyrScale.getMinMax();
    }

    @Override
    public float getMinRotationalAcceleration()
    {
        return gyrScale.getMinMax();
    }

    @Override
    public float getMaxAcceleration()
    {
        return accScale.getMinMax();
    }

    @Override
    public float getMinAcceleration()
    {
        return accScale.getMinMax();
    }
}
