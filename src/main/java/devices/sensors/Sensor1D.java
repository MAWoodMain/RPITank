package devices.sensors;

import devices.sensors.dataTypes.CircularArrayRing;
import devices.sensors.dataTypes.TimestampedData1D;

/**
 * RPITank - devices.sensors
 * Created by MAWood on 18/07/2016.
 */
public abstract class Sensor1D
{
    protected final CircularArrayRing<TimestampedData1D> vals;
    protected float valBias;
    protected float valScaling;

    public Sensor1D()
    {
        vals = new CircularArrayRing<>();
        valBias = 0;
        valScaling = 1;
    }

    public TimestampedData1D getLatestValue()
    {
        return vals.get(0);
    }

    public TimestampedData1D getValue(int i)
    {
        return vals.get(i);
    }

    public int getReadingCount()
    {
        return vals.size();
    }

    public void setValBias(float valBias)
    {
        this.valBias = valBias;
    }

    public void setValScaling(float valScaling)
    {
        this.valScaling = valScaling;
    }

    protected void addValue(TimestampedData1D value)
    {
        value.scale(valScaling);
        value.offset(valBias);
        vals.add(value);
    }
}
