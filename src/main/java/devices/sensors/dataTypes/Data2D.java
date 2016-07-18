package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors.dataTypes
 * Created by MAWood on 18/07/2016.
 */
public class Data2D extends Data1D
{
    protected float y;

    public Data2D(float x,float y)
    {
        super(x);
        this.y = y;
    }

    public float getY()
    {
        return y;
    }

    public void setY(float y)
    {
        this.y = y;
    }

    public String toString()
    {
        final String format = "%+04.3f";
        return 	super.toString() + " y: " + String.format(format,y);
    }
}
