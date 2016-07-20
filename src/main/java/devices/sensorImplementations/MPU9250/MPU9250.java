package devices.sensorImplementations.MPU9250;

import devices.I2C.I2CImplementation;
import devices.sensors.NineDOF;
import devices.sensors.dataTypes.TimestampedData3D;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 17/07/2016.
 */
public class MPU9250 extends NineDOF
{
    private static final AccScale accScale = AccScale.AFS_4G;
    private static final GyrScale gyrScale = GyrScale.GFS_2000DPS;
    private static final MagScale magScale = MagScale.MFS_16BIT;
    private static final MagMode magMode = MagMode.MAG_MODE_100HZ;

    private short lastRawMagX;
    private short lastRawMagY;
    private short lastRawMagZ;

    private final I2CImplementation mpu9250;
    private final I2CImplementation ak8963;

    public MPU9250(I2CImplementation mpu9250,I2CImplementation ak8963,int sampleRate, int sampleSize) throws IOException, InterruptedException
    {
        super(sampleRate,sampleSize);
        // get device
        this.mpu9250 = mpu9250;
        this.ak8963 = ak8963;

        selfTest();
        calibrateGyroAcc();
        initMPU9250();
        initAK8963();
        //calibrateMag();
    }

    private void selfTest() throws IOException, InterruptedException
    {

        byte FS = 0; 
        //int bytesRead =0;

        mpu9250.write(Registers.SMPLRT_DIV.getAddress(),(byte)0x00); // Set gyro sample rate to 1 kHz
        Thread.sleep(2);
        mpu9250.write(Registers.CONFIG.getAddress(),(byte)0x02); // Set gyro sample rate to 1 kHz and DLPF to 92 Hz
        Thread.sleep(2);
        // Set full scale range for the gyro to 250 dps
        mpu9250.write(Registers.GYRO_CONFIG.getAddress(),(byte)(1<<FS));//GyrScale.GFS_250DPS.getX());
        Thread.sleep(2);
        // Set accelerometer rate to 1 kHz and bandwidth to 92 Hz
        mpu9250.write(Registers.ACCEL_CONFIG2.getAddress(),(byte)0x02);
        Thread.sleep(2);
        // Set full scale range for the accelerometer to 2 g
        mpu9250.write(Registers.ACCEL_CONFIG.getAddress(),(byte)(1<<FS));// AccScale.AFS_2G.getX());
        Thread.sleep(2);

        final int TEST_LENGTH = 200;

        int[] aAvg = new int[3]; //16 bit integer to match registers
        int[] gAvg = new int[3];
        short[] registers; 
        for(int s=0; s<TEST_LENGTH; s++)
        {
            registers = read16BitRegisters(mpu9250,Registers.ACCEL_XOUT_H.getAddress(),3);
            aAvg[0] += registers[0];
            aAvg[1] += registers[1];
            aAvg[2] += registers[2];
            Thread.sleep(2);

            registers = read16BitRegisters(mpu9250,Registers.GYRO_XOUT_H.getAddress(),3);
            gAvg[0] += registers[0];
            gAvg[1] += registers[1];
            gAvg[2] += registers[2];
            Thread.sleep(2);
        }

        for(int i = 0; i<3; i++)
        {
            aAvg[i] /= TEST_LENGTH;
            gAvg[i] /= TEST_LENGTH;
        }

        mpu9250.write(Registers.ACCEL_CONFIG.getAddress(), (byte)0xE0);
        Thread.sleep(2);
        mpu9250.write(Registers.GYRO_CONFIG.getAddress(), (byte)0xE0);
        Thread.sleep(2);

        short[] aSTAvg = new short[3]; //16 bit integer to match registers
        short[] gSTAvg = new short[3];

        for(int s=0; s<TEST_LENGTH; s++)
        {
            registers = read16BitRegisters(mpu9250,Registers.ACCEL_XOUT_H.getAddress(),3);
            aSTAvg[0] += registers[0];
            aSTAvg[1] += registers[1];
            aSTAvg[2] += registers[2];
            Thread.sleep(2);

            registers = read16BitRegisters(mpu9250,Registers.GYRO_XOUT_H.getAddress(),3);
            gSTAvg[0] += registers[0];
            gSTAvg[1] += registers[1];
            gSTAvg[2] += registers[2];
            Thread.sleep(2);
        }

        for(int i = 0; i<3; i++)
        {
            aSTAvg[i] /= TEST_LENGTH;
            gSTAvg[i] /= TEST_LENGTH;
        }

        Thread.sleep(2);
        mpu9250.write(Registers.GYRO_CONFIG.getAddress(), GyrScale.GFS_250DPS.getValue());
        Thread.sleep(2);
        mpu9250.write(Registers.ACCEL_CONFIG.getAddress(), AccScale.AFS_2G.getValue());
        Thread.sleep(25);

        byte[] selfTest = new byte[6];

        selfTest[0] = mpu9250.read(Registers.SELF_TEST_X_ACCEL.getAddress());
        Thread.sleep(2);
        selfTest[1] = mpu9250.read(Registers.SELF_TEST_Y_ACCEL.getAddress());
        Thread.sleep(2);
        selfTest[2] = mpu9250.read(Registers.SELF_TEST_Z_ACCEL.getAddress());
        Thread.sleep(2);
        selfTest[3] = mpu9250.read(Registers.SELF_TEST_X_GYRO.getAddress());
        Thread.sleep(2);
        selfTest[4] = mpu9250.read(Registers.SELF_TEST_Y_GYRO.getAddress());
        Thread.sleep(2);
        selfTest[5] = mpu9250.read(Registers.SELF_TEST_Z_GYRO.getAddress());
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

    private void calibrateGyroAcc() throws IOException, InterruptedException
    {
        // Write a one to bit 7 reset bit; toggle reset device
        mpu9250.write(Registers.PWR_MGMT_1.getAddress(),(byte)0x80);
        Thread.sleep(100);

        // get stable time source; Auto select clock source to be PLL gyroscope reference if ready
        // else use the internal oscillator, bits 2:0 = 001
        mpu9250.write(Registers.PWR_MGMT_1.getAddress(),(byte)0x01);
        mpu9250.write(Registers.PWR_MGMT_2.getAddress(),(byte)0x00);
        Thread.sleep(200);


        // Configure device for bias calculation
        mpu9250.write(Registers.INT_ENABLE.getAddress(),(byte) 0x00);   // Disable all interrupts
        mpu9250.write(Registers.FIFO_EN.getAddress(),(byte) 0x00);      // Disable FIFO
        mpu9250.write(Registers.PWR_MGMT_1.getAddress(),(byte) 0x00);   // Turn on internal clock source
        mpu9250.write(Registers.I2C_MST_CTRL.getAddress(),(byte) 0x00); // Disable I2C master
        mpu9250.write(Registers.USER_CTRL.getAddress(),(byte) 0x00);    // Disable FIFO and I2C master modes
        mpu9250.write(Registers.USER_CTRL.getAddress(),(byte) 0x0C);    // Reset FIFO and DMP
        Thread.sleep(15);

        // Configure MPU6050 gyro and accelerometer for bias calculation
        mpu9250.write(Registers.CONFIG.getAddress(),(byte) 0x01);       // Set low-pass filter to 188 Hz
        mpu9250.write(Registers.SMPLRT_DIV.getAddress(),(byte) 0x00);   // Set sample rate to 1 kHz
        mpu9250.write(Registers.GYRO_CONFIG.getAddress(),(byte) 0x00);  // Set gyro full-scale to 250 degrees per second, maximum sensitivity
        mpu9250.write(Registers.ACCEL_CONFIG.getAddress(),(byte) 0x00); // Set accelerometer full-scale to 2 g, maximum sensitivity

        short gyrosensitivity = 131;     // = 131 LSB/degrees/sec
        short accelSensitivity = 16384;  // = 16384 LSB/g

        // Configure FIFO to capture accelerometer and gyro data for bias calculation
        mpu9250.write(Registers.USER_CTRL.getAddress(),(byte) 0x40);   // Enable FIFO
        mpu9250.write(Registers.FIFO_EN.getAddress(),(byte) 0x78);     // Enable gyro and accelerometer sensors for FIFO  (max size 512 bytes in MPU-9150)
        Thread.sleep(40); // accumulate 40 samples in 40 milliseconds = 480 bytes

        // At end of sample accumulation, turn off FIFO sensor read
        mpu9250.write(Registers.FIFO_EN.getAddress(),(byte) 0x00);        // Disable gyro and accelerometer sensors for FIFO

        short packetCount = read16BitRegisters(mpu9250, Registers.FIFO_COUNTH.getAddress(), 1)[0];
        int sampleCount =  packetCount / 12; // 12 bytes per sample 6 x 16 bit values

        int[] accelBiasl = new int[]{0,0,0}; 
        int[] gyroBias = new int[]{0,0,0};
        short[] tempAccelBias = new short[]{0,0,0}; 
        short[] tempGyroBias = new short[]{0,0,0};

        for(int s = 0; s < packetCount; s++)
        {
            tempGyroBias = read16BitRegisters(mpu9250,Registers.FIFO_R_W.getAddress(),6); //12 bytes
            accelBiasl[0] += tempAccelBias[0]; //Signed 32 bit quantities
            accelBiasl[1] += tempAccelBias[1];
            accelBiasl[2] += tempAccelBias[2];
            gyroBias[0] += tempGyroBias[0];
            gyroBias[1] += tempGyroBias[1];
            gyroBias[2] += tempGyroBias[2];
        }

        accelBiasl[0] /= sampleCount; // Normalise sums to get average count biases
        accelBiasl[1] /= sampleCount;
        accelBiasl[2] /= sampleCount;
        gyroBias[0] /= sampleCount;
        gyroBias[1] /= sampleCount;
        gyroBias[2] /= sampleCount;

        if(accelBiasl[2] > 0L) {accelBiasl[2] -= accelSensitivity;}  // Remove gravity from the z-axis accelerometer bias calculation
        else {accelBiasl[2] += accelSensitivity;}


        byte[] buffer = new byte[6];
        // Construct the gyro biases for push to the hardware gyro bias registers, which are reset to zero upon device startup
        buffer[0] = (byte)((-gyroBias[0]/4  >> 8) & 0xFF); // Divide by 4 to get 32.9 LSB per deg/s to conform to expected bias input format
        buffer[1] = (byte)((-gyroBias[0]/4)       & 0xFF); // Biases are additive, so change sign on calculated average gyro biases
        buffer[2] = (byte)((-gyroBias[1]/4  >> 8) & 0xFF);
        buffer[3] = (byte)((-gyroBias[1]/4)       & 0xFF);
        buffer[4] = (byte)((-gyroBias[2]/4  >> 8) & 0xFF);
        buffer[5] = (byte)((-gyroBias[2]/4)       & 0xFF);


        // Push gyro biases to hardware registers
        mpu9250.write(Registers.XG_OFFSET_H.getAddress(), buffer[0]);
        mpu9250.write(Registers.XG_OFFSET_L.getAddress(), buffer[1]);
        mpu9250.write(Registers.YG_OFFSET_H.getAddress(), buffer[2]);
        mpu9250.write(Registers.YG_OFFSET_L.getAddress(), buffer[3]);
        mpu9250.write(Registers.ZG_OFFSET_H.getAddress(), buffer[4]);
        mpu9250.write(Registers.ZG_OFFSET_L.getAddress(), buffer[5]);
        /*
         // Output scaled gyro biases for display in the main program
  		dest1[0] = (float) gyro_bias[0]/(float) gyrosensitivity;  
  		dest1[1] = (float) gyro_bias[1]/(float) gyrosensitivity;
  		dest1[2] = (float) gyro_bias[2]/(float) gyrosensitivity;
         */

        // Construct the accelerometer biases for push to the hardware accelerometer bias registers. These registers contain
        // factory trim values which must be added to the calculated accelerometer biases; on boot up these registers will hold
        // non-zero values. In addition, bit 0 of the lower byte must be preserved since it is used for temperature
        // compensation calculations. Accelerometer bias registers expect bias input as 2048 LSB per g, so that
        // the accelerometer biases calculated above must be divided by 8.
        
        short[] accelBiasReg = read16BitRegisters(mpu9250, Registers.XA_OFFSET_H.getAddress(), 3);

        int mask = 1; // Define mask for temperature compensation bit 0 of lower byte of accelerometer bias registers
        byte[] mask_bit = new byte[]{0, 0, 0}; // Define array to hold mask bit for each accelerometer bias axis

        for(int s = 0; s < 3; s++) {
            if((accelBiasReg[s] & mask)==1) mask_bit[s] = 0x01; // If temperature compensation bit is set, record that fact in mask_bit
        }

        // Construct total accelerometer bias, including calculated average accelerometer bias from above
        accelBiasReg[0] -= (accelBiasl[0]/8); // Subtract calculated averaged accelerometer bias scaled to 2048 LSB/g (16 g full scale)
        accelBiasReg[1] -= (accelBiasl[1]/8);
        accelBiasReg[2] -= (accelBiasl[2]/8);

        buffer = new byte[6];

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
        /*mpu9250.write(XA_OFFSET_H.getX(), buffer[0]);
        mpu9250.write(XA_OFFSET_L.getX(), buffer[1]);
        mpu9250.write(YA_OFFSET_H.getX(), buffer[2]);
        mpu9250.write(YA_OFFSET_L.getX(), buffer[3]);
        mpu9250.write(ZA_OFFSET_H.getX(), buffer[4]);
        mpu9250.write(ZA_OFFSET_L.getX(), buffer[5]);
        */


        accBias[0] = (float)accelBiasl[0]/(float)accelSensitivity;
        accBias[1] = (float)accelBiasl[1]/(float)accelSensitivity;
        accBias[2] = (float)accelBiasl[2]/(float)accelSensitivity;

    }

    private void initMPU9250() throws IOException, InterruptedException
    {
        // wake up device
        // Clear sleep mode bit (6), enable all sensors
        mpu9250.write(Registers.PWR_MGMT_1.getAddress(), (byte)0x00);
        Thread.sleep(100); // Wait for all registers to reset

        // get stable time source
        mpu9250.write(Registers.PWR_MGMT_1.getAddress(), (byte)0x01);  // Auto select clock source to be PLL gyroscope reference if ready else
        Thread.sleep(200);

        // Configure Gyro and Thermometer
        // Disable FSYNC and set thermometer and gyro bandwidth to 41 and 42 Hz, respectively;
        // minimum delay time for this setting is 5.9 ms, which means sensor fusion update rates cannot
        // be higher than 1 / 0.0059 = 170 Hz
        // DLPF_CFG = bits 2:0 = 011; this limits the sample rate to 1000 Hz for both
        // With the MPU9250_Pi4j, it is possible to get gyro sample rates of 32 kHz (!), 8 kHz, or 1 kHz
        mpu9250.write(Registers.CONFIG.getAddress(), (byte)0x03);

        // Set sample rate = gyroscope output rate/(1 + SMPLRT_DIV)
        mpu9250.write(Registers.SMPLRT_DIV.getAddress(), (byte)0x04);  // Use a 200 Hz rate; a rate consistent with the filter update rate
        // determined inset in CONFIG above

        // Set gyroscope full scale range
        // Range selects FS_SEL and AFS_SEL are 0 - 3, so 2-bit values are left-shifted into positions 4:3
        byte c = (byte) mpu9250.read(Registers.GYRO_CONFIG.getAddress()); // get current GYRO_CONFIG register value
        // c = c & ~0xE0; // Clear self-test bits [7:5]
        c = (byte)(c & ~0x02); // Clear Fchoice bits [1:0]
        c = (byte)(c & ~0x18); // Clear AFS bits [4:3]
        c = (byte)(c | gyrScale.getValue() << 3); // Set full scale range for the gyro
        // c =| 0x00; // Set Fchoice for the gyro to 11 by writing its inverse to bits 1:0 of GYRO_CONFIG
        mpu9250.write(Registers.GYRO_CONFIG.getAddress(), c ); // Write new GYRO_CONFIG value to register

        // Set accelerometer full-scale range configuration
        c = (byte) mpu9250.read(Registers.ACCEL_CONFIG.getAddress()); // get current ACCEL_CONFIG register value
        // c = c & ~0xE0; // Clear self-test bits [7:5]
        c = (byte)(c & ~0x18);  // Clear AFS bits [4:3]
        c = (byte)(c | accScale.getValue() << 3); // Set full scale range for the accelerometer
        mpu9250.write(Registers.ACCEL_CONFIG.getAddress(), c); // Write new ACCEL_CONFIG register value

        // Set accelerometer sample rate configuration
        // It is possible to get a 4 kHz sample rate from the accelerometer by choosing 1 for
        // accel_fchoice_b bit [3]; in this case the bandwidth is 1.13 kHz
        c = (byte) mpu9250.read(Registers.ACCEL_CONFIG2.getAddress()); // get current ACCEL_CONFIG2 register value
        c = (byte)(c & ~0x0F); // Clear accel_fchoice_b (bit 3) and A_DLPFG (bits [2:0])
        c = (byte)(c | 0x03);  // Set accelerometer rate to 1 kHz and bandwidth to 41 Hz
        mpu9250.write(Registers.ACCEL_CONFIG2.getAddress(), c); // Write new ACCEL_CONFIG2 register value

        // The accelerometer, gyro, and thermometer are set to 1 kHz sample rates,
        // but all these rates are further reduced by a factor of 5 to 200 Hz because of the SMPLRT_DIV setting

        // Configure Interrupts and Bypass Enable
        // Set interrupt pin active high, push-pull, hold interrupt pin level HIGH until interrupt cleared,
        // clear on read of INT_STATUS, and enable I2C_BYPASS_EN so additional chips
        // can join the I2C bus and all can be controlled by the Arduino as master
        //mpu9250.write(Registers.INT_PIN_CFG.getValue(), (byte)0x12);  // INT is 50 microsecond pulse and any read to clear
        mpu9250.write(Registers.INT_PIN_CFG.getAddress(), (byte)0x22);  // INT is 50 microsecond pulse and any read to clear - as per MPUBASICAHRS_T3
        mpu9250.write(Registers.INT_ENABLE.getAddress(), (byte)0x01);  // Enable data ready (bit 0) interrupt
        Thread.sleep(100);
    }

    private void initAK8963() throws InterruptedException, IOException
    {
        // First extract the factory calibration for each magnetometer axis

        ak8963.write(Registers.AK8963_CNTL.getAddress(),(byte) 0x00); // Power down magnetometer
        Thread.sleep(10);
        ak8963.write(Registers.AK8963_CNTL.getAddress(), (byte)0x0F); // Enter Fuse ROM access mode
        Thread.sleep(10);
        byte rawData[] = ak8963.read(Registers.AK8963_ASAX.getAddress(), 3);  // Read the x-, y-, and z-axis calibration values
        magScaling[0] =  (float)(rawData[0] - 128)/256f + 1f;   // Return x-axis sensitivity adjustment values, etc.
        magScaling[1] =  (float)(rawData[1] - 128)/256f + 1f;
        magScaling[2] =  (float)(rawData[2] - 128)/256f + 1f;
        ak8963.write(Registers.AK8963_CNTL.getAddress(), (byte)0x00); // Power down magnetometer
        Thread.sleep(10);
        // Configure the magnetometer for continuous read and highest resolution
        // set Mscale bit 4 to 1 (0) to enable 16 (14) bit resolution in CNTL register,
        // and enable continuous mode data acquisition Mmode (bits [3:0]), 0010 for 8 Hz and 0110 for 100 Hz sample rates
        ak8963.write(Registers.AK8963_CNTL.getAddress(), (byte)(MagScale.MFS_16BIT.getValue() << 4 | magMode.getMode())); // Set magnetometer data resolution and sample ODR
        Thread.sleep(10);
    }

    private void calibrateMag() throws InterruptedException, IOException
    {
        int sample_count = 0;
        int  mag_bias[] = {0, 0, 0}, mag_scale[] = {0, 0, 0};
        short mag_max[] = {(short)0x8000, (short)0x8000, (short)0x8000},
        		mag_min[] = {(short)0x7FFF, (short)0x7FFF, (short)0x7FFF},
        		mag_temp[] = {0, 0, 0};

        System.out.println("Mag Calibration: Wave device in a figure eight until done!");
        Thread.sleep(4000);

        // shoot for ~fifteen seconds of mag data
        for(int ii = 0; ii < magMode.getSampleCount(); ii++) {
            updateMagnetometerData();  // Read the mag data
            mag_temp[0] = (short) lastRawMagX;
            mag_temp[1] = (short) lastRawMagY;
            mag_temp[2] = (short) lastRawMagZ;
            for (int jj = 0; jj < 3; jj++) {
                if(mag_temp[jj] > mag_max[jj]) mag_max[jj] = mag_temp[jj];
                if(mag_temp[jj] < mag_min[jj]) mag_min[jj] = mag_temp[jj];
            }
            if(magMode == MagMode.MAG_MODE_8HZ) Thread.sleep(135);  // at 8 Hz ODR, new mag data is available every 125 ms
            if(magMode == MagMode.MAG_MODE_100HZ) Thread.sleep(12);  // at 100 Hz ODR, new mag data is available every 10 ms
        }
        // Get hard iron correction
        mag_bias[0]  = (mag_max[0] + mag_min[0])/2;  // get average x mag bias in counts
        mag_bias[1]  = (mag_max[1] + mag_min[1])/2;  // get average y mag bias in counts
        mag_bias[2]  = (mag_max[2] + mag_min[2])/2;  // get average z mag bias in counts

        magBias[0] = (float) mag_bias[0]*magScale.getRes()* magScaling[0];  // save mag biases in G for main program
        magBias[1] = (float) mag_bias[1]*magScale.getRes()* magScaling[1];
        magBias[2] = (float) mag_bias[2]*magScale.getRes()* magScaling[2];

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

    @Override
    public void updateAccelerometerData() throws IOException
    {
        float x,y,z;
        short registers[];
        //mpu9250.read(Registers.ACCEL_XOUT_H.getAddress(), 6);  // Read again to trigger
 
        registers = read16BitRegisters(mpu9250,Registers.ACCEL_XOUT_H.getAddress(),3);
        //System.out.println("Accelerometer " + xs + ", " + ys + ", " + zs);

        x = (float) ((float)registers[0]*accScale.getRes()); // transform from raw data to g
        y = (float) ((float)registers[1]*accScale.getRes()); // transform from raw data to g
        z = (float) ((float)registers[2]*accScale.getRes()); // transform from raw data to g

        x -= accBias[0];
        y -= accBias[1];
        z -= accBias[2];

        acc.add(new TimestampedData3D(x,y,z));
    }

    @Override
    public void updateGyroscopeData() throws IOException
    {
        float x,y,z;
        short registers[];
        //mpu9250.read(Registers.GYRO_XOUT_H.getAddress(), 6);  // Read again to trigger
        registers = read16BitRegisters(mpu9250,Registers.GYRO_XOUT_H.getAddress(),3);
        //System.out.println("Gyroscope " + x + ", " + y + ", " + z);

        x = (float) ((float)registers[0]*gyrScale.getRes()); // transform from raw data to degrees/s
        y = (float) ((float)registers[1]*gyrScale.getRes()); // transform from raw data to degrees/s
        z = (float) ((float)registers[2]*gyrScale.getRes()); // transform from raw data to degrees/s

        gyr.add(new TimestampedData3D(x,y,z));
    }

    @Override
    public void updateMagnetometerData() throws IOException
    {
        byte newMagData = (byte) (ak8963.read(Registers.AK8963_ST1.getAddress()) & 0x01);
        if (newMagData == 0) return;
        byte[] buffer = ak8963.read(Registers.AK8963_ST1.getAddress(), 7);

        byte c = buffer[6];
        if((c & 0x08) == 0)
        { // Check if magnetic sensor overflow set, if not then report data
            lastRawMagX = (short) ((buffer[1] << 8) | buffer[0]); // Turn the MSB and LSB into a signed 16-bit value
            lastRawMagY = (short) ((buffer[3] << 8) | buffer[2]); // Data stored as little Endian
            lastRawMagZ = (short) ((buffer[5] << 8) | buffer[4]);
            float x=lastRawMagX,y=lastRawMagY,z=lastRawMagZ;

            x *= magScale.getRes()* magScaling[0];
            y *= magScale.getRes()* magScaling[1];
            z *= magScale.getRes()* magScaling[2];

            x -= magBias[0];
            y -= magBias[1];
            z -= magBias[2];

            mag.add(new TimestampedData3D(x,y,z));
        }
        ak8963.read(0x09);
    }

    @Override
    public void updateThermometerData() throws Exception
    {
        byte rawData[] = mpu9250.read(Registers.TEMP_OUT_H.getAddress(),2);  // Read the two raw data registers sequentially into data array
        mpu9250.read(Registers.TEMP_OUT_H.getAddress(),2);  // Read again to trigger
        therm.add((float)(short)((rawData[0] << 8) | rawData[1]));  // Turn the MSB and LSB into a 16-bit value
    }
    /**
     * Reads the specified number of 16 bit Registers from a given device and address
     * @param address 	- the start address for the read
     * @param regCount 	- number of 16 bit registers to be read
     * @return 			- an array of shorts (16 bit signed values) holding the registers
     * Each registers is constructed from reading and combining 2 bytes, the first byte forms the more significant part of the register 
     */
    public short[] read16BitRegisters(I2CImplementation device, int address, int regCount)
    {
        byte rawData[] = null;
		try {
			rawData = device.read(address, regCount*2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        short[] registers = new short[regCount];
        for (int i=0;i<regCount;i++)		
        {
        	registers[i] = (short) ((rawData[i*2] << 8) | rawData[i*2+1]) ;  // Turn the MSB and LSB into a signed 16-bit value
        }
    	return registers;
    }
}