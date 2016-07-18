package devices.sensors;

import devices.sensors.dataTypes.CircularArrayRing;
import devices.sensors.dataTypes.TimestampedData1D;

/**
 * RPITank - devices.sensors
 * Created by MAWood on 18/07/2016.
 */
public abstract class Sensor1D
{
    private final CircularArrayRing<TimestampedData1D> rawVals;
    private float valBias;
    private float valScaling;
    private float unitCorrectionScale;
    private float unitCorrectionOffset;

    public Sensor1D()
    {
        rawVals = new CircularArrayRing<>();
        valBias = 0;
        valScaling = 1;
        unitCorrectionOffset = 0;
        unitCorrectionScale = 1;
    }

    public TimestampedData1D getLatestValue()
    {
        return this.getValue(0);
    }

    public TimestampedData1D getValue(int i)
    {
        TimestampedData1D actual = rawVals.get(i).clone();
        actual.scale(valScaling);
        actual.offset(valBias);
        actual.scale(unitCorrectionScale);
        actual.offset(unitCorrectionOffset);
        return actual;
    }

    protected void addValue(TimestampedData1D value)
    {
        rawVals.add(value);
    }

    public int getReadingCount()
    {
        return rawVals.size();
    }

    public void setValBias(float valBias)
    {
        this.valBias = valBias;
    }

    public void setValScaling(float valScaling)
    {
        this.valScaling = valScaling;
    }

    public float getValBias()
    {
        return valBias;
    }

    public float getValScaling()
    {
        return valScaling;
    }

    public float getUnitCorrectionScale()
    {
        return unitCorrectionScale;
    }

    public void setUnitCorrectionScale(float unitCorrectionScale)
    {
        this.unitCorrectionScale = unitCorrectionScale;
    }

    public float getUnitCorrectionOffset()
    {
        return unitCorrectionOffset;
    }

    public void setUnitCorrectionOffset(float unitCorrectionOffset)
    {
        this.unitCorrectionOffset = unitCorrectionOffset;
    }
}
