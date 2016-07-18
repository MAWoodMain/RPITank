package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors
 * Created by MAWood on 18/07/2016.
 */
public abstract class Sensor1D
{
    protected final CircularArrayRing<TimestampedData1D> rawXVals;
    protected float xBias;
    protected float xScaling;
    protected float unitCorrectionScale;
    protected float unitCorrectionOffset;

    public Sensor1D()
    {
        rawXVals = new CircularArrayRing<>();
        xBias = 0;
        xScaling = 1;
        unitCorrectionOffset = 0;
        unitCorrectionScale = 1;
    }

    public TimestampedData1D getLatestX()
    {
        return this.getX(0);
    }

    public TimestampedData1D getX(int i)
    {
        TimestampedData1D actual = rawXVals.get(i).clone();
        actual.scale(xScaling);
        actual.offset(xBias);
        actual.scale(unitCorrectionScale);
        actual.offset(unitCorrectionOffset);
        return actual;
    }

    protected void addValue(TimestampedData1D value)
    {
        rawXVals.add(value);
    }

    protected void setxBias(float xBias)
    {
        this.xBias = xBias;
    }

    protected void setxScaling(float xScaling)
    {
        this.xScaling = xScaling;
    }

    protected void setUnitCorrectionScale(float unitCorrectionScale)
    {
        this.unitCorrectionScale = unitCorrectionScale;
    }

    protected void setUnitCorrectionOffset(float unitCorrectionOffset)
    {
        this.unitCorrectionOffset = unitCorrectionOffset;
    }
}
