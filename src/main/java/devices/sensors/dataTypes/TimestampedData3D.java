package devices.sensors.dataTypes;

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

    public TimestampedData3D(TimestampedData3D data)
    {
        super(data.getX(),data.getY(),data.getZ());
        this.nanoTime = data.nanoTime;
    }

    public String toString()
    {
        String format = "%+04.4f";
        return 	" t: " + String.format(format,(float)(nanoTime/(float)NANOS_PER_SEC)) +
                " " + super.toString();
    }

    public static TimestampedData3D integrate(TimestampedData3D sampleT, TimestampedData3D sampleTm1 )
    {
        final float deltaT = (float)(sampleT.nanoTime-sampleTm1.nanoTime)/(float)TimestampedData3D.NANOS_PER_SEC; // time difference between samples in seconds

        return new TimestampedData3D(
                (sampleT.getX()+sampleTm1.getX())/2f*deltaT,//Trapezoidal area, average height X deltaT
                (sampleT.getY()+sampleTm1.getY())/2f*deltaT,//Trapezoidal area, average height Y deltaT
                (sampleT.getZ()+sampleTm1.getZ())/2f*deltaT,//Trapezoidal area, average height Z deltaT
                sampleT.nanoTime); // preserve timestamp in result
    }

    public TimestampedData3D integrate(TimestampedData3D sampleTm1 )
    {
        return integrate(sampleTm1,this);
    }

    public TimestampedData3D clone()
    {
        return new TimestampedData3D(x,y,z,nanoTime);
    }

}