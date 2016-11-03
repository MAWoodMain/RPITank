package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors.dataTypes
 * Created by MAWood on 18/07/2016.
 */
public abstract class Sensor3D extends Sensor2D
{
    protected final CircularArrayRing<Data1D> rawZVals;
    private float zBias;
    private float zScaling;

    public Sensor3D(int sampleRate, int sampleSize)
    {
        super(sampleRate,sampleSize);
        rawZVals = new CircularArrayRing<>(sampleSize);
        zBias = 0;
        zScaling = 1;
    }

    public TimestampedData1D getLatestZ()
    {
        return this.getZ(0);
    }

    public TimestampedData1D getZ(int i)
    {
        Data1D actual = rawZVals.get(i).clone();
        actual.scale(zScaling);
        actual.offset(zBias);
        actual.scale(unitCorrectionScale);
        actual.offset(unitCorrectionOffset);
        return new TimestampedData1D(actual.getX(),rawXVals.get(i).nanoTime);
    }
    
    public TimestampedData1D getAvgZ()
    {
    	int n = rawZVals.size();
    	double sum = 0;
    	Data1D value;
    	for (int i=0; i<n; i++  )
    	{
    		value = rawZVals.get(i).clone();
    		value.scale(zScaling);
    		value.offset(zBias);
    		value.scale(unitCorrectionScale);
    		value.offset(unitCorrectionOffset);
    		sum += value.getX();
    	}
    	TimestampedData1D actual = rawXVals.get(n-1).clone();
    	actual.setX((float)(sum/n));
        return actual;
    }

    public TimestampedData3D getLatestXYZ()
    {
        return this.getXYZ(0);
    }

    public TimestampedData3D getXYZ(int i)
    {
        TimestampedData3D actual = new TimestampedData3D(
                rawXVals.get(i).getX(), // X value
                rawYVals.get(i).getX(), // Y value
                rawZVals.get(i).getX(), // Z value
                rawXVals.get(i).nanoTime); // time (taken from X)
        actual.scale(xScaling,yScaling,zScaling);
        actual.offset(xBias,yBias,zBias);
        actual.scale(unitCorrectionScale,unitCorrectionScale,unitCorrectionScale);
        actual.offset(unitCorrectionOffset,unitCorrectionOffset,unitCorrectionOffset);
        return actual;
    }
    
    public TimestampedData3D getAvgXYZ()
    {
    	TimestampedData1D x = this.getAvgX();
    	return new TimestampedData3D(x.getX(),this.getAvgY().getX(),this.getAvgZ().getX(),x.nanoTime);
    }

    protected void addValue(TimestampedData3D value)
    {
        rawXVals.add(new TimestampedData1D(value.getX(),value.nanoTime));
        rawYVals.add(new Data1D(value.getY()));
        rawZVals.add(new Data1D(value.getZ()));
    }

    public void setzBias(float zBias)
    {
        this.zBias = zBias;
    }

    public void setzScaling(float zScaling)
    {
        this.zScaling = zScaling;
    }

    public float getzBias()
    {
        return zBias;
    }

    public float getzScaling()
    {
        return zScaling;
    }
}
