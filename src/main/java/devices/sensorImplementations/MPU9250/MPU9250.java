package devices.sensorImplementations.MPU9250;

import devices.I2C.I2CImplementation;
import devices.sensors.NineDOF;
import devices.sensors.dataTypes.TimestampedData3D;

import java.io.IOException;
import java.util.Arrays;

/**
 * MPU 9250 motion sensor
 * Created by MAWood on 17/07/2016 with contributions from G.J.Wood
 * Based on MPU9250_MS5637_t3 Basic Example Code by: Kris Winer date: April 1, 2014
 * https://github.com/kriswiner/MPU-9250/blob/master/MPU9250_MS5637_AHRS_t3.ino
 * 
 * Basic MPU-9250 gyroscope, accelerometer and magnetometer functionality including self test, initialisation, and calibration of the sensor,
 * getting properly scaled accelerometer, gyroscope, and magnetometer data out. This class is independent of the bus implementation, register 
 * addressing etc as this is handled by RegisterOperations 
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

    private final RegisterOperations roMPU;
    private final RegisterOperations roAK;


	public MPU9250(I2CImplementation mpu9250,I2CImplementation ak8963,int sampleRate, int sampleSize) throws IOException, InterruptedException
    {
        super(sampleRate,sampleSize);
        // get device
        this.roMPU = new RegisterOperations(mpu9250);
        this.roAK = new RegisterOperations(ak8963);

        selfTest();
        calibrateGyroAcc();
        initMPU9250();
        initAK8963();
        calibrateMag();
    }

    private void selfTest() throws IOException, InterruptedException
    {
    	System.out.println("selfTest");
        byte FS = 0; 

        roMPU.writeByteRegister(Registers.SMPLRT_DIV,(byte)0x00); // Set gyro sample rate to 1 kHz
        roMPU.writeByteRegister(Registers.CONFIG,(byte)0x02); // Set gyro sample rate to 1 kHz and DLPF to 92 Hz
        roMPU.writeByteRegister(Registers.GYRO_CONFIG,GyrScale.GFS_250DPS.getValue()); // Set full scale range for the gyro to 250 dps (was FS<<3) 
        roMPU.writeByteRegister(Registers.ACCEL_CONFIG,(byte)AccScale.AFS_2G.getValue());// Set full scale range for the accelerometer to 2 g (was FS<<3 )
        roMPU.writeByteRegister(Registers.ACCEL_CONFIG2,(byte)0x02); // Set accelerometer rate to 1 kHz and bandwidth to 92 Hz
        final int TEST_LENGTH = 200;

        int[] aSum = new int[] {0,0,0}; //32 bit integer to accumulate and avoid overflow
        int[] gSum = new int[] {0,0,0}; //32 bit integer to accumulate and avoid overflow
        short[] registers; 
        for(int s=0; s<TEST_LENGTH; s++)
        {
            //System.out.print("aAvg acc: "+Arrays.toString(aAvg));
            registers = roMPU.read16BitRegisters(Registers.ACCEL_XOUT_H,3);
            aSum[0] += registers[0];
            aSum[1] += registers[1];
            aSum[2] += registers[2];
        	//System.out.format("reg added [%d, %d, %d] [0x%X, 0x%X, 0x%X]%n",
        	//		registers[0],registers[1],registers[2],registers[0],registers[1],registers[2]);

            //System.out.print("gAvg acc: "+Arrays.toString(gAvg));
            registers = roMPU.read16BitRegisters(Registers.GYRO_XOUT_H,3);
            gSum[0] += registers[0];
            gSum[1] += registers[1];
            gSum[2] += registers[2];
        	//System.out.format("reg added [%d, %d, %d] [0x%X, 0x%X, 0x%X]%n",
        	//		registers[0],registers[1],registers[2],registers[0],registers[1],registers[2]);
        }
        short[] aAvg = new short[] {0,0,0};
        short[] gAvg = new short[] {0,0,0};
        for(int i = 0; i<3; i++)
        {
            aAvg[i] = (short) ((short)(aSum[i]/TEST_LENGTH) & (short)0xFFFF); //average and mask off top bits
            gAvg[i] = (short) ((short)(gSum[i]/TEST_LENGTH) & (short)0xFFFF); //average and mask off top bits
        }

        System.out.print("aAvg average: "+Arrays.toString(aAvg));
    	System.out.format(" [0x%X, 0x%X, 0x%X]%n", aAvg[0], aAvg[1], aAvg[2]);
        System.out.print("gAvg average: "+Arrays.toString(gAvg));
    	System.out.format(" [0x%X, 0x%X, 0x%X]%n", gAvg[0], gAvg[1], gAvg[2]);
        
        // Configure the accelerometer for self-test
        roMPU.writeByteRegister(Registers.ACCEL_CONFIG, (byte)(0xE0 | AccScale.AFS_2G.getValue())); // Enable self test on all three axes and set accelerometer range to +/- 2 g
        roMPU.writeByteRegister(Registers.GYRO_CONFIG, (byte)(0xE0 | GyrScale.GFS_250DPS.getValue()));// Enable self test on all three axes and set gyro range to +/- 250 degrees/s
        Thread.sleep(25); // Delay a while to let the device stabilise
        //outputConfigRegisters();
        int[] aSelfTestSum = new int[] {0,0,0}; //32 bit integer to accumulate and avoid overflow
        int[] gSelfTestSum = new int[] {0,0,0}; //32 bit integer to accumulate and avoid overflow
        
        // get average self-test values of gyro and accelerometer
        for(int s=0; s<TEST_LENGTH; s++) 
        {
            registers = roMPU.read16BitRegisters(Registers.ACCEL_XOUT_H,3);
            aSelfTestSum[0] += registers[0];
            aSelfTestSum[1] += registers[1];
            aSelfTestSum[2] += registers[2];

            registers = roMPU.read16BitRegisters(Registers.GYRO_XOUT_H,3);
            gSelfTestSum[0] += registers[0];
            gSelfTestSum[1] += registers[1];
            gSelfTestSum[2] += registers[2];
        }
        
        short[] aSTAvg = new short[] {0,0,0};
        short[] gSTAvg = new short[] {0,0,0};

        for(int i = 0; i<3; i++)
        {
            aSTAvg[i] = (short) ((short)(aSelfTestSum[i]/TEST_LENGTH) & (short)0xFFFF); //average and mask off top bits
            gSTAvg[i] = (short) ((short)(gSelfTestSum[i]/TEST_LENGTH) & (short)0xFFFF); //average and mask off top bits
        }
        System.out.print("aSTAvg average: "+Arrays.toString(aSTAvg));
    	System.out.format(" [0x%X, 0x%X, 0x%X]%n", aSTAvg[0], aSTAvg[1], aSTAvg[2]);
        System.out.print("gSTAvg average: "+Arrays.toString(gSTAvg));
    	System.out.format(" [0x%X, 0x%X, 0x%X]%n", aSTAvg[0], aSTAvg[1], aSTAvg[2]);


        // Calculate Accelerometer accuracy
        short[] selfTestAccel = new short[3]; //Longer than byte to allow for removal of sign bit as this is unsigned
        selfTestAccel[0] = (short)((short)roMPU.readByteRegister(Registers.SELF_TEST_X_ACCEL) & 0xFF);
        selfTestAccel[1] = (short)((short)roMPU.readByteRegister(Registers.SELF_TEST_Y_ACCEL) & 0xFF);
        selfTestAccel[2] = (short)((short)roMPU.readByteRegister(Registers.SELF_TEST_Z_ACCEL) & 0xFF);
        System.out.print("Self test Accel bytes: "+Arrays.toString(selfTestAccel));
    	System.out.format(" [0x%X, 0x%X, 0x%X]%n", selfTestAccel[0], selfTestAccel[1], selfTestAccel[2]);
        
        float[] factoryTrimAccel = new float[3];
        factoryTrimAccel[0] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestAccel[0] - 1f);
        factoryTrimAccel[1] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestAccel[1] - 1f);
        factoryTrimAccel[2] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestAccel[2] - 1f);
        System.out.println("factoryTrimAcc (float): "+Arrays.toString(factoryTrimAccel)); 

        float[] AccuracyAccel = new float[3];
        AccuracyAccel[0] = 100f*((float)(aSTAvg[0] - aAvg[0]))/factoryTrimAccel[0]-100f;
        AccuracyAccel[1] = 100f*((float)(aSTAvg[1] - aAvg[1]))/factoryTrimAccel[1]-100f;
        AccuracyAccel[2] = 100f*((float)(aSTAvg[2] - aAvg[2]))/factoryTrimAccel[2]-100f;

        System.out.println("Accelerometer accuracy:(% away from factory values)");
        System.out.println("x: " + AccuracyAccel[0] + "%");
        System.out.println("y: " + AccuracyAccel[1] + "%");
        System.out.println("z: " + AccuracyAccel[2] + "%");
               
        // Calculate Gyro accuracy       
        short[] selfTestGyro = new short[3]; //Longer than byte to allow for removal of sign bit as this is unsigned
        selfTestGyro[0] = (short)((short)roMPU.readByteRegister(Registers.SELF_TEST_X_GYRO) & 0xFF);
        selfTestGyro[1] = (short)((short)roMPU.readByteRegister(Registers.SELF_TEST_Y_GYRO) & 0xFF);
        selfTestGyro[2] = (short)((short)roMPU.readByteRegister(Registers.SELF_TEST_Z_GYRO) & 0xFF);
        System.out.println("Self test Gyro bytes: "+Arrays.toString(selfTestGyro));        

        float[] factoryTrimGyro = new float[3];
        factoryTrimGyro[0] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestGyro[0] - 1f);
        factoryTrimGyro[1] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestGyro[1] - 1f);
        factoryTrimGyro[2] = (float)(2620/1<<FS)*(float)Math.pow(1.01,(float)selfTestGyro[2] - 1f);
        System.out.println("factoryTrimGyro (float): "+Arrays.toString(factoryTrimAccel)); 

        float[] AccuracyGyro = new float[3];
        AccuracyGyro[0] = 100f*((float)(gSTAvg[0] - gAvg[0]))/factoryTrimGyro[0]-100f;
        AccuracyGyro[1] = 100f*((float)(gSTAvg[1] - gAvg[1]))/factoryTrimGyro[1]-100f;
        AccuracyGyro[2] = 100f*((float)(gSTAvg[2] - gAvg[2]))/factoryTrimGyro[2]-100f;
        
        System.out.println("Gyroscope accuracy:(% away from factory values)");
        System.out.println("x: " + AccuracyGyro[0] + "%");
        System.out.println("y: " + AccuracyGyro[1] + "%");
        System.out.println("z: " + AccuracyGyro[2] + "%");

        roMPU.writeByteRegister(Registers.ACCEL_CONFIG, (byte)(0x00 | AccScale.AFS_2G.getValue())); //Clear self test mode and set accelerometer range to +/- 2 g
        roMPU.writeByteRegister(Registers.GYRO_CONFIG,  (byte)(0x00 | GyrScale.GFS_250DPS.getValue())); //Clear self test mode and set gyro range to +/- 250 degrees/s
        
        Thread.sleep(25); // Delay a while to let the device stabilise

        System.out.println("End selfTest");
    }

    private void calibrateGyroAcc() throws IOException, InterruptedException
    {
    	System.out.println("calibrateGyroAcc");
        // Write a one to bit 7 reset bit; toggle reset device
        roMPU.writeByteRegister(Registers.PWR_MGMT_1,(byte)0x80);
        Thread.sleep(100);

        // get stable time source; Auto select clock source to be PLL gyroscope reference if ready
        // else use the internal oscillator, bits 2:0 = 001
        roMPU.writeByteRegister(Registers.PWR_MGMT_1,(byte)0x01);
        roMPU.writeByteRegister(Registers.PWR_MGMT_2,(byte)0x00);
        Thread.sleep(200);


        // Configure device for bias calculation
        roMPU.writeByteRegister(Registers.INT_ENABLE,(byte) 0x00);   // Disable all interrupts
        roMPU.writeByteRegister(Registers.FIFO_EN,(byte) 0x00);      // Disable FIFO
        roMPU.writeByteRegister(Registers.PWR_MGMT_1,(byte) 0x00);   // Turn on internal clock source
        roMPU.writeByteRegister(Registers.I2C_MST_CTRL,(byte) 0x00); // Disable I2C master
        roMPU.writeByteRegister(Registers.USER_CTRL,(byte) 0x00);    // Disable FIFO and I2C master modes
        roMPU.writeByteRegister(Registers.USER_CTRL,(byte) 0x0C);    // Reset FIFO and DMP NB the 0x08 bit is the DMP shown as reserved in docs
        Thread.sleep(15);

        // Configure MPU6050 gyro and accelerometer for bias calculation
        roMPU.writeByteRegister(Registers.CONFIG,(byte) 0x01);       // Set low-pass filter to 188 Hz
        roMPU.writeByteRegister(Registers.SMPLRT_DIV,(byte) 0x00);   // Set sample rate to 1 kHz
        roMPU.writeByteRegister(Registers.GYRO_CONFIG,(byte) 0x00);  // Set gyro full-scale to 250 degrees per second, maximum sensitivity
        roMPU.writeByteRegister(Registers.ACCEL_CONFIG,(byte) 0x00); // Set accelerometer full-scale to 2 g, maximum sensitivity

        short gyrosensitivity = 131;     // = 131 LSB/degrees/sec
        short accelSensitivity = 16384;  // = 16384 LSB/g - OK in short max 32,767

        // Configure FIFO to capture accelerometer and gyro data for bias calculation
        roMPU.writeByteRegister(Registers.USER_CTRL,(byte) 0x40);   // Enable FIFO
        roMPU.writeByteRegister(Registers.FIFO_EN,(byte) 0x78);     // Enable gyro and accelerometer sensors for FIFO  (max size 512 bytes in MPU-9150)
        Thread.sleep(40); // accumulate 40 samples in 40 milliseconds = 480 bytes

        // At end of sample accumulation, turn off FIFO sensor read
        roMPU.writeByteRegister(Registers.FIFO_EN,(byte) 0x00);        // Disable gyro and accelerometer sensors for FIFO

        short packetCount = roMPU.read16BitRegisters( Registers.FIFO_COUNTH, 1)[0];
        int sampleCount =  packetCount / 12; // 12 bytes per sample 6 x 16 bit values

        int[] accelBiasSum = new int[]{0,0,0}; //32 bit to allow for accumulation without overflow
        int[] gyroBiasSum = new int[]{0,0,0}; //32 bit to allow for accumulation without overflow
        short[] accelBiasAvg = new short[]{0,0,0}; //16 bit average
        short[] gyroBiasAvg = new short[]{0,0,0}; //16 bit average
        short[] tempBias;
        System.out.println("packetCount: "+packetCount);
        for(int s = 0; s < sampleCount; s++)
        {
            tempBias = roMPU.read16BitRegisters(Registers.FIFO_R_W,6); //12 bytes
            //System.out.print("bias sample bytes: "+Arrays.toString(tempBias));
        	//System.out.format(" [0x%X, 0x%X, 0x%X, 0x%X, 0x%X, 0x%X]%n",tempBias[0],tempBias[1],tempBias[2],tempBias[3],tempBias[4],tempBias[5]);
            
            accelBiasSum[0] += tempBias[0]; // Sum individual signed 16-bit biases to get accumulated signed 32-bit biases
            accelBiasSum[1] += tempBias[1];
            accelBiasSum[2] += tempBias[2];
            gyroBiasSum[0] += tempBias[3];
            gyroBiasSum[1] += tempBias[4];
            gyroBiasSum[2] += tempBias[5];
        }

        accelBiasAvg[0] = (short)((accelBiasSum[0] / sampleCount) & 0xffff); // Normalise sums to get average count biases
        accelBiasAvg[1] = (short)((accelBiasSum[1] / sampleCount) & 0xffff); 
        accelBiasAvg[2] = (short)((accelBiasSum[2] / sampleCount) & 0xffff); 
        gyroBiasAvg[0] = (short)((gyroBiasSum[0] / sampleCount) & 0xffff);
        gyroBiasAvg[1] = (short)((gyroBiasSum[1] / sampleCount) & 0xffff);
        gyroBiasAvg[2] = (short)((gyroBiasSum[2] / sampleCount) & 0xffff);
        
        System.out.print("Accel Bias average: "+Arrays.toString(accelBiasAvg));
    	System.out.format(" [0x%X, 0x%X, 0x%X]%n",accelBiasAvg[0],accelBiasAvg[1],accelBiasAvg[2]);

        if(accelBiasAvg[2] > 0) {accelBiasAvg[2] -= accelSensitivity;}  // Remove gravity from the z-axis accelerometer bias calculation
        else {accelBiasAvg[2] += accelSensitivity;}
    	System.out.format("z ajusted for gravity %d 0x%X%n",accelBiasAvg[2],accelBiasAvg[2]);

        System.out.print("Gyro Bias average: "+Arrays.toString(gyroBiasAvg));
    	System.out.format(" [0x%X, 0x%X, 0x%X]%n",gyroBiasAvg[0],gyroBiasAvg[1],gyroBiasAvg[2]);

        byte[] buffer = new byte[6];
        // Construct the gyro biases for push to the hardware gyro bias registers, which are reset to zero upon device startup
        buffer[0] = (byte)(((-gyroBiasAvg[0]/4)  >> 8) & 0xFF); // Divide by 4 to get 32.9 LSB per deg/s to conform to expected bias input format
        buffer[1] = (byte)((-gyroBiasAvg[0]/4)       & 0xFF); // Biases are additive, so change sign on calculated average gyro biases
        buffer[2] = (byte)(((-gyroBiasAvg[1]/4)  >> 8) & 0xFF);
        buffer[3] = (byte)((-gyroBiasAvg[1]/4)       & 0xFF);
        buffer[4] = (byte)(((-gyroBiasAvg[2]/4)  >> 8) & 0xFF);
        buffer[5] = (byte)((-gyroBiasAvg[2]/4)       & 0xFF);
        System.out.print("Bias bytes: "+Arrays.toString(buffer));
    	System.out.format(" [0x%X, 0x%X, 0x%X, 0x%X, 0x%X, 0x%X]%n",buffer[0],buffer[1],buffer[2],buffer[3],buffer[4],buffer[5]);
        

        // Push gyro biases to hardware registers
        roMPU.writeByteRegister(Registers.XG_OFFSET_H, buffer[0]);
        roMPU.writeByteRegister(Registers.XG_OFFSET_L, buffer[1]);
        roMPU.writeByteRegister(Registers.YG_OFFSET_H, buffer[2]);
        roMPU.writeByteRegister(Registers.YG_OFFSET_L, buffer[3]);
        roMPU.writeByteRegister(Registers.ZG_OFFSET_H, buffer[4]);
        roMPU.writeByteRegister(Registers.ZG_OFFSET_L, buffer[5]);
        
         // Output scaled gyro biases for display in the main program
  		gyrBias[0] = (float) gyroBiasAvg[0]/(float) gyrosensitivity;  
  		gyrBias[1] = (float) gyroBiasAvg[1]/(float) gyrosensitivity;
  		gyrBias[2] = (float) gyroBiasAvg[2]/(float) gyrosensitivity;
        System.out.println("gyrBias (float): "+Arrays.toString(gyrBias));

        // Construct the accelerometer biases for push to the hardware accelerometer bias registers. These registers contain
        // factory trim values which must be added to the calculated accelerometer biases; on boot up these registers will hold
        // non-zero values. In addition, bit 0 of the lower byte must be preserved since it is used for temperature
        // compensation calculations. Accelerometer bias registers expect bias input as 2048 LSB per g, so that
        // the accelerometer biases calculated above must be divided by 8.
        // XA_OFFSET is a 15 bit quantity with bits 14:7 in the high byte and 6:0 in the low byte with temperature compensation in bit0
        // so having got it in a 16 bit short, and having preserved the bottom bit, the number must be shifted right by 1 or divide by 2
        // to give the correct value for calculations. After calculations it must be shifted left by 1 or multiplied by 2 to get
        // the bytes correct, then the preserved bit0 can be put back before the bytes are written to registers
        
        short[] accelBiasReg = roMPU.read16BitRegisters( Registers.XA_OFFSET_H, 3);
        System.out.print("accelBiasReg (16bit): "+Arrays.toString(accelBiasReg));
    	System.out.format(" [0x%X, 0x%X, 0x%X] %n",accelBiasReg[0],accelBiasReg[1],accelBiasReg[2]);

        int mask = 0x01; // Define mask for temperature compensation bit 0 of lower byte of accelerometer bias registers
        byte[] mask_bit = new byte[]{0, 0, 0}; // Define array to hold mask bit for each accelerometer bias axis

        for(int s = 0; s < 3; s++) {
            if((accelBiasReg[s] & mask)==1) mask_bit[s] = 0x01; // If temperature compensation bit is set, record that fact in mask_bit
            //divide accelBiasReg by 2 to remove the bottom bit and preserve any sign (java has no unsigned 16 bit numbers)
            accelBiasReg[s] /=2;
        }
        // Construct total accelerometer bias, including calculated average accelerometer bias from above
        for (int i = 0; i<3; i++)
        {
        	accelBiasReg[i] -= (accelBiasAvg[i]/8); // Subtract calculated averaged accelerometer bias scaled to 2048 LSB/g (16 g full scale)
        	accelBiasReg[i] *=2; //multiply by two to leave the bottom bit clear and but all the bits in the correct bytes
        }
        System.out.print("(accelBiasReg - biasAvg/8)*2 (16bit): "+Arrays.toString(accelBiasReg));
    	System.out.format(" [0x%X, 0x%X, 0x%X] %n",accelBiasReg[0],accelBiasReg[1],accelBiasReg[2]);

        buffer = new byte[6];
        
        // XA_OFFSET is a 15 bit quantity with bits 14:7 in the high byte and 6:0 in the low byte with temperature compensation in bit0

        buffer[0] = (byte)((accelBiasReg[0] >> 8) & 0xFF); //Shift down and mask top 8 bits
        buffer[1] = (byte)((accelBiasReg[0])      & 0xFE); //copy bits 7-1 clear bit 0
        buffer[1] = (byte)(buffer[1] | mask_bit[0]); // preserve temperature compensation bit when writing back to accelerometer bias registers
        buffer[2] = (byte)((accelBiasReg[1] >> 8) & 0xFF); //Shift down and mask top 8 bits
        buffer[3] = (byte)((accelBiasReg[1])      & 0xFE); //copy bits 7-1 clear bit 0
        buffer[3] = (byte)(buffer[3] | mask_bit[1]); // preserve temperature compensation bit when writing back to accelerometer bias registers
        buffer[4] = (byte)((accelBiasReg[2] >> 8) & 0xFF); //Shift down and mask top 8 bits
        buffer[5] = (byte)((accelBiasReg[2])      & 0xFE); //copy bits 7-1 clear bit 0
        buffer[5] = (byte)(buffer[5] | mask_bit[2]); // preserve temperature compensation bit when writing back to accelerometer bias registers
        System.out.print("accelBiasReg bytes: "+Arrays.toString(buffer));
    	System.out.format(" [0x%X, 0x%X, 0x%X, 0x%X, 0x%X, 0x%X]%n",buffer[0],buffer[1],buffer[2],buffer[3],buffer[4],buffer[5]);

        // Apparently this is not working for the acceleration biases in the MPU-9250
        // Are we handling the temperature correction bit properly? - see comments above
    	
        // Push accelerometer biases to hardware registers  	
        roMPU.writeByteRegister(Registers.XA_OFFSET_H, buffer[0]);
        roMPU.writeByteRegister(Registers.XA_OFFSET_L, buffer[1]);
        roMPU.writeByteRegister(Registers.YA_OFFSET_H, buffer[2]);
        roMPU.writeByteRegister(Registers.YA_OFFSET_L, buffer[3]);
        roMPU.writeByteRegister(Registers.ZA_OFFSET_H, buffer[4]);
        roMPU.writeByteRegister(Registers.ZA_OFFSET_L, buffer[5]);
        


        accBias[0] = (float)accelBiasAvg[0]/(float)accelSensitivity;
        accBias[1] = (float)accelBiasAvg[1]/(float)accelSensitivity;
        accBias[2] = (float)accelBiasAvg[2]/(float)accelSensitivity;
        System.out.println("accelBias (float): "+Arrays.toString(buffer));
      
    	System.out.println("End calibrateGyroAcc");
    }

    private void initMPU9250() throws IOException, InterruptedException
    {
    	System.out.println("initMPU9250");
        // wake up device
        // Clear sleep mode bit (6), enable all sensors
        roMPU.writeByteRegister(Registers.PWR_MGMT_1, (byte)0x00);
        Thread.sleep(100); // Wait for all registers to reset

        // get stable time source
        roMPU.writeByteRegister(Registers.PWR_MGMT_1, (byte)0x01);  // Auto select clock source to be PLL gyroscope reference if ready else
        Thread.sleep(200);

        // Configure Gyro and Thermometer
        // Disable FSYNC and set thermometer and gyro bandwidth to 41 and 42 Hz, respectively;
        // minimum delay time for this setting is 5.9 ms, which means sensor fusion update rates cannot
        // be higher than 1 / 0.0059 = 170 Hz
        // DLPF_CFG = bits 2:0 = 011; this limits the sample rate to 1000 Hz for both
        // With the MPU9250_Pi4j, it is possible to get gyro sample rates of 32 kHz (!), 8 kHz, or 1 kHz
        roMPU.writeByteRegister(Registers.CONFIG, (byte)0x03);

        // Set sample rate = gyroscope output rate/(1 + SMPLRT_DIV)
        roMPU.writeByteRegister(Registers.SMPLRT_DIV, (byte)0x04);  // Use a 200 Hz rate; a rate consistent with the filter update rate
        // determined inset in CONFIG above

        // Set gyroscope full scale range
        // Range selects FS_SEL and AFS_SEL are 0 - 3, so 2-bit values are left-shifted into positions 4:3 (not in java!)
        byte c = roMPU.readByteRegister(Registers.GYRO_CONFIG); // get current GYRO_CONFIG register value
        c = (byte)(c & ~0xE0); // Clear self-test bits [7:5]  ####
        c = (byte)(c & ~0x02); // Clear Fchoice bits [1:0]
        c = (byte)(c & ~0x18); // Clear AFS bits [4:3]
        c = (byte)(c | gyrScale.getValue() ); // Set full scale range for the gyro GFS_2000DP = 0x18 = 24 #### does not require shifting!!!!
        c = (byte)(c | 0x00); // Set Fchoice for the gyro to 11 by writing its inverse to bits 1:0 of GYRO_CONFIG
        roMPU.writeByteRegister(Registers.GYRO_CONFIG, c ); // Write new GYRO_CONFIG value to register

        // Set accelerometer full-scale range configuration
        c = roMPU.readByteRegister(Registers.ACCEL_CONFIG); // get current ACCEL_CONFIG register value
        c = (byte)(c & ~0xE0); // Clear self-test bits [7:5] ####
        c = (byte)(c & ~0x18);  // Clear AFS bits [4:3]
        c = (byte)(c | accScale.getValue() ); // Set full scale range for the accelerometer #### does not require shifting!!!!
        roMPU.writeByteRegister(Registers.ACCEL_CONFIG, c); // Write new ACCEL_CONFIG register value

        // Set accelerometer sample rate configuration
        // It is possible to get a 4 kHz sample rate from the accelerometer by choosing 1 for
        // accel_fchoice_b bit [3]; in this case the bandwidth is 1.13 kHz
        c = roMPU.readByteRegister(Registers.ACCEL_CONFIG2); // get current ACCEL_CONFIG2 register value
        c = (byte)(c & ~0x0F); // Clear accel_fchoice_b (bit 3) and A_DLPFG (bits [2:0])
        c = (byte)(c | 0x03);  // Set accelerometer rate to 1 kHz and bandwidth to 41 Hz 
        roMPU.writeByteRegister(Registers.ACCEL_CONFIG2, c); // Write new ACCEL_CONFIG2 register value

        // The accelerometer, gyro, and thermometer are set to 1 kHz sample rates,
        // but all these rates are further reduced by a factor of 5 to 200 Hz because of the SMPLRT_DIV setting

        // Configure Interrupts and Bypass Enable
        // Set interrupt pin active high, push-pull, hold interrupt pin level HIGH until interrupt cleared,
        // clear on read of INT_STATUS, and enable I2C_BYPASS_EN so additional chips
        // can join the I2C bus and all can be controlled by the Arduino as master
        //ro.writeByteRegister(Registers.INT_PIN_CFG.getValue(), (byte)0x12);  // INT is 50 microsecond pulse and any read to clear
        roMPU.writeByteRegister(Registers.INT_PIN_CFG, (byte)0x22);  // INT is 50 microsecond pulse and any read to clear - as per MPUBASICAHRS_T3
        roMPU.writeByteRegister(Registers.INT_ENABLE, (byte)0x01);  // Enable data ready (bit 0) interrupt
        //roMPU.outputConfigRegisters();
        Thread.sleep(100);
    	System.out.println("End initMPU9250");
    }

    private void initAK8963() throws InterruptedException, IOException
    {
    	System.out.println("initAK8963");
        // First extract the factory calibration for each magnetometer axis

        roAK.writeByteRegister(Registers.AK8963_CNTL,(byte) 0x00); // Power down magnetometer
        Thread.sleep(10);
        roAK.writeByteRegister(Registers.AK8963_CNTL, (byte)0x0F); // Enter Fuse ROM access mode
        Thread.sleep(10);
        byte rawData[] = roAK.readByteRegisters(Registers.AK8963_ASAX, 3);  // Read the x-, y-, and z-axis calibration values
        magScaling[0] =  (float)(rawData[0] - 128)/256f + 1f;   // Return x-axis sensitivity adjustment values, etc.
        magScaling[1] =  (float)(rawData[1] - 128)/256f + 1f;
        magScaling[2] =  (float)(rawData[2] - 128)/256f + 1f;
        roAK.writeByteRegister(Registers.AK8963_CNTL, (byte)0x00); // Power down magnetometer
        Thread.sleep(10);
        // Configure the magnetometer for continuous read and highest resolution
        // set Mscale bit 4 to 1 (0) to enable 16 (14) bit resolution in CNTL register,
        // and enable continuous mode data acquisition Mmode (bits [3:0]), 0010 for 8 Hz and 0110 for 100 Hz sample rates
        roAK.writeByteRegister(Registers.AK8963_CNTL, (byte)(MagScale.MFS_16BIT.getValue() << 4 | magMode.getMode())); // Set magnetometer data resolution and sample ODR
        Thread.sleep(10);
    	System.out.println("End initAK8963");
    }

    private void calibrateMag() throws InterruptedException, IOException
    {
    	System.out.println("calibrateMag");

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

        System.out.println("End calibrateMag");
    }

    @Override
    public void updateAccelerometerData() throws IOException
    {
        float x,y,z;
        short registers[];
        //roMPU.readByteRegister(Registers.ACCEL_XOUT_H, 6);  // Read again to trigger
 
        registers = roMPU.read16BitRegisters(Registers.ACCEL_XOUT_H,3);
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
        //roMPU.readByteRegister(Registers.GYRO_XOUT_H, 6);  // Read again to trigger
        registers = roMPU.read16BitRegisters(Registers.GYRO_XOUT_H,3);
        //System.out.println("Gyroscope " + x + ", " + y + ", " + z);

        x = (float) ((float)registers[0]*gyrScale.getRes()); // transform from raw data to degrees/s
        y = (float) ((float)registers[1]*gyrScale.getRes()); // transform from raw data to degrees/s
        z = (float) ((float)registers[2]*gyrScale.getRes()); // transform from raw data to degrees/s

        gyr.add(new TimestampedData3D(x,y,z));
    }

    @Override
    public void updateMagnetometerData() throws IOException
    {
        byte dataReady = (byte)(roAK.readByteRegister(Registers.AK8963_ST1) & 0x01); //DRDY - Data ready bit0 1 = data is ready
        if (dataReady == 0) return; //no data ready
        
        // data is ready, read it NB bug fix here read was starting from ST1 not XOUT_L
        byte[] buffer = roAK.readByteRegisters(Registers.AK8963_XOUT_L, 7); //6 data bytes x,y,z 16 bits stored as little Endian (L/H)

        byte status2 = buffer[6]; // Status2 register must be read as part of data read to show device data has been read
        if((status2 & 0x08) == 0) //bit3 HOFL: Magnetic sensor overflow is normal (no Overflow), data is valid
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
        //roAK.readByteRegister(Registers.AK8963_ST2);// Data overflow bit 3 and data read error status bit 2
    }

    @Override
    public void updateThermometerData() throws Exception
    {
    	short[] temperature = roMPU.read16BitRegisters(Registers.TEMP_OUT_H,1);
    	temperature = roMPU.read16BitRegisters(Registers.TEMP_OUT_H,1);
    	therm.add((float)temperature[0]);
    }
}