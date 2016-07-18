package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors.dataTypes
 * Created by MAWood on 18/07/2016.
 */
public abstract class Sensor2D extends Sensor1D
{
    protected final CircularArrayRing<Data1D> rawYVals;
    protected float yBias;
    protected float yScaling;

    public Sensor2D()
    {
        rawYVals = new CircularArrayRing<>();
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

    protected void addValue(TimestampedData2D value)
    {
        rawXVals.add(new TimestampedData1D(value.getX(),value.nanoTime));
        rawYVals.add(new Data1D(value.getY()));
    }

}
