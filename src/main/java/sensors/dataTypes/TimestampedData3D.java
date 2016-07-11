package sensors.dataTypes;

/**
 * RPITank
 * Created by MAWood on 11/07/2016.
 */
public class TimestampedData3D extends Data3D
{
    public final long nanoTime;

    public TimestampedData3D(float x, float y, float z, long nanoTime)
    {
        super(x, y, z);
        this.nanoTime = nanoTime;
    }
    public TimestampedData3D(float x, float y, float z)
    {
        this(x, y, z, System.nanoTime());
    }
}
