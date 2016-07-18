package devices.sensors;

import devices.sensors.dataTypes.CircularArrayRing;
import devices.sensors.dataTypes.TimestampedData3D;
import devices.sensors.interfaces.*;

/**
 * Created by MAWood on 17/07/2016.
 */
public abstract class NineDOF extends SensorPackage implements Accelerometer, Gyroscope, Magnetometer, Thermometer
{

    protected final CircularArrayRing<TimestampedData3D> acc;
    protected final CircularArrayRing<TimestampedData3D> gyr;
    protected final CircularArrayRing<TimestampedData3D> mag;
    protected final CircularArrayRing<Float> tmp;

    protected float[] accBias;
    protected float[] gyrBias;
    protected float[] magBias;
    protected float tmpBias;

    protected float[] accScaling;
    protected float[] gyrScaling;
    protected float[] magScaling;
    protected float tmpScaling;

    protected NineDOF(int sampleRate, int sampleSize)
    {
        super(sampleRate);

        acc = new CircularArrayRing<>(sampleSize);
        gyr = new CircularArrayRing<>(sampleSize);
        mag = new CircularArrayRing<>(sampleSize);
        tmp = new CircularArrayRing<>(sampleSize);

        accBias = new float[]{0,0,0};
        gyrBias = new float[]{0,0,0};
        magBias = new float[]{0,0,0};
        tmpBias = 0f;

        accScaling = new float[]{1,1,1};
        gyrScaling = new float[]{1,1,1};
        magScaling = new float[]{1,1,1};
        tmpScaling = 1f;
    }


    @Override
    public float getLatestTemperature()
    {
        return getTemperature(0);
    }

    @Override
    public float getTemperature(int i)
    {
        return tmp.get(i);
    }

    @Override
    public int getThermometerReadingCount()
    {
        return tmp.size();
    }

    @Override
    public TimestampedData3D getLatestRotationalAcceleration()
    {
        return getRotationalAcceleration(0);
    }

    @Override
    public TimestampedData3D getRotationalAcceleration(int i)
    {
        return gyr.get(i);
    }

    @Override
    public int getGyroscopeReadingCount()
    {
        return gyr.size();
    }

    @Override
    public TimestampedData3D getLatestAcceleration()
    {
        return getAcceleration(0);
    }

    @Override
    public TimestampedData3D getAcceleration(int i)
    {
        return acc.get(i);
    }

    @Override
    public int getAccelerometerReadingCount()
    {
        return acc.size();
    }

    @Override
    public TimestampedData3D getLatestGaussianData()
    {
        return getGaussianData(0);
    }

    @Override
    public TimestampedData3D getGaussianData(int i)
    {
        return mag.get(i);
    }

    @Override
    public int getMagnetometerReadingCount()
    {
        return mag.size();
    }

    public void updateData()
    {
        try
        {
            updateAccelerometerData();
            updateGyroscopeData();
            updateMagnetometerData();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
