package devices.sensors;

import devices.sensors.dataTypes.CircularArrayRing;

/**
 * RPITank - devices.sensors
 * Created by GJWood on 18/07/2016.
 */
public abstract class Sensor <T>
{
    protected final CircularArrayRing<T> vals;
    protected float valBias;
    protected float valScaling;

    public Sensor()
    {
        vals = new CircularArrayRing<T>();
        valBias = 0;
        valScaling = 1;
    }

    public T getLatestValue()
    {
        return vals.get(0);
    }

    public T getValue(int i)
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

    protected void addValue(T value)
    {
    	//TODO fix scaling etc
        //value.scale(valScaling);
        //value.offset(valBias);
        vals.add(value);
    }
}
