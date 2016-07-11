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
        //calibrateGyroAcc();
        //initMPU9250();
        //initAK8963();
        //calibrateMag();



        paused = false;
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

        System.out.println("Accelerometer accuracy:");
        System.out.println("x: " + 100.0*((float)(aSTAvg[0] - aAvg[0]))/factoryTrim[0] + "%");
        System.out.println("y: " + 100.0*((float)(aSTAvg[1] - aAvg[1]))/factoryTrim[1] + "%");
        System.out.println("z: " + 100.0*((float)(aSTAvg[2] - aAvg[2]))/factoryTrim[2] + "%");
        System.out.println("Gyroscope accuracy:");
        System.out.println("x: " + 100.0*((float)(aSTAvg[0] - aAvg[0]))/factoryTrim[3] + "%");
        System.out.println("y: " + 100.0*((float)(aSTAvg[1] - aAvg[1]))/factoryTrim[4] + "%");
        System.out.println("z: " + 100.0*((float)(aSTAvg[2] - aAvg[2]))/factoryTrim[5] + "%");
    }

    private void updateData()
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
    public int getReadingCount()
    {
        return 0;
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
