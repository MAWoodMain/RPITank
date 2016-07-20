package devices.sensorImplementations.MPU9250;

import devices.I2C.I2CImplementation;
import devices.sensors.Magnetometer;
import devices.sensors.dataTypes.TimestampedData3D;

import java.io.IOException;

/**
 * RPITank - devices.sensorImplementations.MPU9250
 * Created by MAWood on 18/07/2016.
 */
public class AK8963 extends Magnetometer
{
    private final I2CImplementation ak8963;
    private static final MagScale magScale = MagScale.MFS_16BIT;
    private static final MagMode magMode = MagMode.MAG_MODE_100HZ;

    public AK8963(I2CImplementation ak8963, int sampleRate,int sampleSize) throws IOException, InterruptedException
    {
        super(sampleRate,sampleSize);
        this.ak8963 = ak8963;

        this.setUnitCorrectionOffset(0);
        this.setUnitCorrectionScale(magScale.getRes());

        init();
        calibrateMag();
    }

    private void init() throws IOException, InterruptedException
    {
        // First extract the factory calibration for each magnetometer axis

        ak8963.write(Registers.AK8963_CNTL.getAddress(),(byte) 0x00); // Power down magnetometer
        Thread.sleep(10);
        ak8963.write(Registers.AK8963_CNTL.getAddress(), (byte)0x0F); // Enter Fuse ROM access mode
        Thread.sleep(10);
        byte rawData[] = ak8963.read(Registers.AK8963_ASAX.getAddress(), 3);  // Read the x-, y-, and z-axis calibration values
        this.setxScaling((float)(rawData[0] - 128)/256f + 1f);   // Return x-axis sensitivity adjustment values, etc.
        this.setyScaling((float)(rawData[1] - 128)/256f + 1f);
        this.setzScaling((float)(rawData[2] - 128)/256f + 1f);
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
        int mag_bias[] = {0, 0, 0}, mag_scale[] = {0, 0, 0};
        int mag_max[] = {0x8000, 0x8000, 0x8000}, mag_min[] = {0x7FFF, 0x7FFF, 0x7FFF}, mag_temp[] = {0, 0, 0};

        System.out.println("Mag Calibration: Wave device in a figure eight until done!");
        Thread.sleep(4000);

        // shoot for ~fifteen seconds of mag data
        if(magMode == MagMode.MAG_MODE_8HZ) sample_count = 128;  // at 8 Hz ODR, new mag data is available every 125 ms
        if(magMode == MagMode.MAG_MODE_100HZ) sample_count = 1500;  // at 100 Hz ODR, new mag data is available every 10 ms
        for(int ii = 0; ii < sample_count; ii++) {
            updateData();  // Read the mag data
            mag_temp[0] = (int)rawXVals.get(0).getX();
            mag_temp[1] = (int)rawYVals.get(0).getX();
            mag_temp[2] = (int)rawZVals.get(0).getX();
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

        this.setxBias((float) mag_bias[0]*magScale.getRes()* this.getxScaling());  // save mag biases in G for main program
        this.setyBias((float) mag_bias[1]*magScale.getRes()* this.getyScaling());
        this.setzBias((float) mag_bias[2]*magScale.getRes()* this.getzScaling());

        // Get soft iron correction estimate
        mag_scale[0]  = (mag_max[0] - mag_min[0])/2;  // get average x axis max chord length in counts
        mag_scale[1]  = (mag_max[1] - mag_min[1])/2;  // get average y axis max chord length in counts
        mag_scale[2]  = (mag_max[2] - mag_min[2])/2;  // get average z axis max chord length in counts

        float avg_rad = mag_scale[0] + mag_scale[1] + mag_scale[2];
        avg_rad /= 3.0;

        this.setxScaling(avg_rad/((float)mag_scale[0]));
        this.setyScaling(avg_rad/((float)mag_scale[1]));
        this.setzScaling(avg_rad/((float)mag_scale[2]));

        System.out.println("Mag Calibration done!");
    }

    @Override
    protected void updateData() throws IOException
    {
        int newMagData = (ak8963.read(Registers.AK8963_ST1.getAddress()) & 0x01);
        if (newMagData == 0) return;
        byte[] buffer = ak8963.read(Registers.AK8963_ST1.getAddress(), 7);

        byte c = buffer[6];
        if((c & 0x08) == 0)
        { // Check if magnetic sensor overflow set, if not then report data
            float x,y,z;
            x = (buffer[1] << 8) | buffer[0];
            y = (buffer[3] << 8) | buffer[2];
            z = (buffer[5] << 8) | buffer[4];
            this.addValue(new TimestampedData3D(x,y,z));
        }
        ak8963.read(0x09);
    }
}
