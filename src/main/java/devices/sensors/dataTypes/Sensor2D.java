package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors.dataTypes
 * Created by MAWood on 18/07/2016.
 */
public abstract class Sensor2D extends Sensor1D
{
    protected final CircularArrayRing<Data1D> rawYVals;
    float yBias;
    float yScaling;

    public Sensor2D(int sampleRate, int sampleSize)
    {
        super(sampleRate,sampleSize);
        rawYVals = new CircularArrayRing<>(sampleSize);
        yBias = 0;
        yScaling = 1;
    }

    public TimestampedData1D getLatestY()
    {
        return this.getY(0);
    }

    public TimestampedData1D getY(int i)
    {
        Data1D actual = rawYVals.get(i).clone();
        actual.scale(yScaling);
        actual.offset(yBias);
        actual.scale(unitCorrectionScale);
        actual.offset(unitCorrectionOffset);
        return new TimestampedData1D(actual.getX(),rawXVals.get(i).nanoTime);
    }
    public TimestampedData1D getAvgY()
    {
    	int n = rawYVals.size();
    	double sum = 0;
    	Data1D value;
    	for (int i=0; i<n; i++  )
    	{
    		value = rawYVals.get(i).clone();
    		value.scale(yScaling);
    		value.offset(yBias);
    		value.scale(unitCorrectionScale);
    		value.offset(unitCorrectionOffset);
    		sum += value.getX();
    	}
    	TimestampedData1D actual = rawXVals.get(n-1).clone();
    	actual.setX((float)(sum/n));
        return actual;
    }

    public TimestampedData2D getLatestXY()
    {
        return this.getXY(0);
    }

    public TimestampedData2D getXY(int i)
    {
        TimestampedData2D actual = new TimestampedData2D(
                rawXVals.get(i).getX(), // X value
                rawYVals.get(i).getX(), // Y value
                rawXVals.get(i).nanoTime); // time (taken from X)
        actual.scale(xScaling,yScaling);
        actual.offset(xBias,yBias);
        actual.scale(unitCorrectionScale,unitCorrectionScale);
        actual.offset(unitCorrectionOffset,unitCorrectionOffset);
        return actual;
    }

    public TimestampedData2D getAvgXY()
    {
    	TimestampedData1D x = this.getAvgX();
    	return new TimestampedData2D(x.getX(),this.getAvgY().getX(),x.nanoTime);
    }

    protected void addValue(TimestampedData2D value)
    {
        rawXVals.add(new TimestampedData1D(value.getX(),value.nanoTime));
        rawYVals.add(new Data1D(value.getY()));
    }

    public void setyBias(float yBias)
    {
        this.yBias = yBias;
    }

    public void setyScaling(float yScaling)
    {
        this.yScaling = yScaling;
    }

    public float getyBias()
    {
        return yBias;
    }

    public float getyScaling()
    {
        return yScaling;
    }
}
