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

    public void scale(float xScale,float yScale)
    {
        super.scale(xScale);
        y *= yScale;
    }

    public void offset(float xOffset,float yOffset)
    {
        super.offset(xOffset);
        y += yOffset;
    }

    public String toString()
    {
        final String format = "%+04.3f";
        return 	super.toString() + " y: " + String.format(format,y);
    }
    public Data2D clone()
    {
        return new Data2D(x,y);
    }
}
