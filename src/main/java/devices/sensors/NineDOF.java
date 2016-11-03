package devices.sensors;

import devices.sensors.dataTypes.CircularArrayRing;
import devices.sensors.dataTypes.TimestampedData3D;
import devices.sensors.interfaces.Accelerometer;
import devices.sensors.interfaces.Gyroscope;
import devices.sensors.interfaces.Magnetometer;
import devices.sensors.interfaces.Thermometer;

/**
 * Created by MAWood on 17/07/2016.
 */
public abstract class NineDOF extends SensorPackage implements Accelerometer, Gyroscope, Magnetometer, Thermometer
{

    protected final CircularArrayRing<TimestampedData3D> acc;
    protected final CircularArrayRing<TimestampedData3D> gyr;
    protected final CircularArrayRing<TimestampedData3D> mag;
    protected final CircularArrayRing<Float> therm;

    protected float[] accBias;
    protected float[] gyrBias;
    protected float[] magBias;
    protected float thermBias;

    protected float[] accScaling;
    protected float[] gyrScaling;
    protected float[] magScaling;
    protected float thermScaling;

    protected NineDOF(int sampleRate, int sampleSize)
    {
        super(sampleRate);

        acc = new CircularArrayRing<>(sampleSize);
        gyr = new CircularArrayRing<>(sampleSize);
        mag = new CircularArrayRing<>(sampleSize);
        therm = new CircularArrayRing<>(sampleSize);

        accBias = new float[]{0,0,0};
        gyrBias = new float[]{0,0,0};
        magBias = new float[]{0,0,0};
        thermBias = 0f;

        accScaling = new float[]{1,1,1};
        gyrScaling = new float[]{1,1,1};
        magScaling = new float[]{1,1,1};
        thermScaling = 1f;
    }


    @Override
    public float getLatestTemperature()
    {
        return getTemperature(0);
    }

    @Override
    public float getTemperature(int i)
    {
        return therm.get(i);
    }

    @Override
    public int getThermometerReadingCount()
    {
        return therm.size();
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
            updateThermometerData();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
