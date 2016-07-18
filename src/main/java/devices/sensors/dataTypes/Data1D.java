package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors.dataTypes
 * Created by MAWood on 18/07/2016.
 */
public class Data1D
{
    protected float x;

    public Data1D(float x) {
        this.x = x;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void scale(float xScale)
    {
        x *= xScale;
    }

    public void offset(float xOffset)
    {
        x += xOffset;
    }

    public String toString()
    {
        final String format = "%+04.3f";
        return 	"x: " + String.format(format,x);
    }
    public Data1D clone()
    {
        return new Data1D(x);
    }
}
