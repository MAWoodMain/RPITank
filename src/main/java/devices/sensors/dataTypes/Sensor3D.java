package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors.dataTypes
 * Created by MAWood on 18/07/2016.
 */
public abstract class Sensor3D extends Sensor2D
{
    private final CircularArrayRing<Data1D> rawZVals;
    private float zBias;
    private float zScaling;

    public Sensor3D()
    {
        rawZVals = new CircularArrayRing<>();
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
}
