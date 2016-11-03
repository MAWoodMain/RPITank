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

////////////////////////////////////////////////////////////////////////////////
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
    public float getAvgTemperature()
    {
    	float sum = 0;
    	float avg = 0;
    	int n = therm.size();
    	if(n<=0) return avg;
    	for( int i = n-1; i>=0; i--)
    	{
    		sum += therm.get(i);
    	}
    	avg = sum/n;
    	return avg;
    }
////////////////////////////////////////////////////////////////////////////////
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
    public TimestampedData3D getAvgRotationalAcceleration()
    {
    	TimestampedData3D sum = new TimestampedData3D(0,0,0);
    	TimestampedData3D avg = new TimestampedData3D(sum);
    	int n = gyr.size();
    	if(n<=0) return avg;
    	for( int i = n-1; i>=0; i--)
    	{
    		sum.setX(sum.getX()+gyr.get(i).getX());
    		sum.setY(sum.getY()+gyr.get(i).getY());
    		sum.setZ(sum.getZ()+gyr.get(i).getZ());
    	}
    	avg.setX(sum.getX()/n);
    	avg.setY(sum.getY()/n);
    	avg.setZ(sum.getZ()/n);
    	return avg;
    }
////////////////////////////////////////////////////////////////////////////////
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
    
    public TimestampedData3D getAvgAcceleration()
    {
    	TimestampedData3D sum = new TimestampedData3D(0,0,0);
    	TimestampedData3D avg = new TimestampedData3D(sum);
    	int n = acc.size();
    	if(n<=0) return avg;
    	avg = acc.get(n-1); //set timestamp correctly
    	for( int i = n-1; i>=0; i--)
    	{
    		sum.setX(sum.getX()+acc.get(i).getX());
    		sum.setY(sum.getY()+acc.get(i).getY());
    		sum.setZ(sum.getZ()+acc.get(i).getZ());
    		//System.out.println(i + " : " +acc.get(i).getZ()+ "," + sum.getZ());
    	}
    	avg.setX(sum.getX()/n);
    	avg.setY(sum.getY()/n);
    	avg.setZ(sum.getZ()/n);
    	return avg;
    }
////////////////////////////////////////////////////////////////////////////////
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
    public TimestampedData3D getAvgGauss()
    {
    	TimestampedData3D sum = new TimestampedData3D(0,0,0);
    	TimestampedData3D avg = new TimestampedData3D(sum);
    	int n = mag.size();
    	if(n<=0) return avg;
    	for( int i = n-1; i>=0; i--)
    	{
    		sum.setX(sum.getX()+mag.get(i).getX());
    		sum.setY(sum.getY()+mag.get(i).getY());
    		sum.setZ(sum.getZ()+mag.get(i).getZ());
    	}
    	avg.setX(sum.getX()/n);
    	avg.setY(sum.getY()/n);
    	avg.setZ(sum.getZ()/n);
    	return avg;
    }
////////////////////////////////////////////////////////////////////////////////
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
