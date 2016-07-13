package sensors.dataTypes;

/**
 * RPITank
 * Created by MAWood on 11/07/2016.
 */
public class TimestampedData3D extends Data3D
{    
	public static final long NANOS_PER_SEC = 1000000000;
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

    public TimestampedData3D(Data3D data)
    {
        this(data.getX(),data.getY(),data.getZ());
    }
    
	public String toString()
	{
		String format = "%+04.4f";
		return 	" t: " + String.format(format,(float)(nanoTime/NANOS_PER_SEC)) +
				" x: " + String.format(format,this.getX()) +
				" y: " + String.format(format,this.getY()) +
				" z: " + String.format(format,this.getZ());
	}
}