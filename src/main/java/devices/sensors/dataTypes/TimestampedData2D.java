package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors.dataTypes
 * Created by MAWood on 18/07/2016.
 */
public class TimestampedData2D extends Data2D
{
    public static final long NANOS_PER_SEC = 1000000000;
    public final long nanoTime;

    public TimestampedData2D(float x, float y, long nanoTime)
    {
        super(x, y);
        this.nanoTime = nanoTime;
    }

    public TimestampedData2D(float x, float y)
    {
        this(x, y, System.nanoTime());
    }

    public TimestampedData2D(Data2D data)
    {
        this(data.getX(),data.getY());
    }

    public TimestampedData2D(TimestampedData2D data)
    {
        super(data.getX(),data.getY());
        this.nanoTime = data.nanoTime;
    }

    public String toString()
    {
        String format = "%+04.4f";
        return 	" t: " + String.format(format,(float)(nanoTime/NANOS_PER_SEC)) +
                " " + super.toString();
    }

    public static TimestampedData2D integrate(TimestampedData2D sampleT, TimestampedData2D sampleTm1 )
    {
        final float deltaT = (float)(sampleT.nanoTime-sampleTm1.nanoTime)/(float)TimestampedData3D.NANOS_PER_SEC; // time difference between samples in seconds

        return new TimestampedData2D(
                (sampleT.getX()+sampleTm1.getX())/2f*deltaT,//Trapezoidal area, average height X deltaT
                (sampleT.getY()+sampleTm1.getY())/2f*deltaT,//Trapezoidal area, average height Y deltaT
                sampleT.nanoTime); // preserve timestamp in result
    }

    public TimestampedData2D integrate(TimestampedData2D sampleTm1 )
    {
        return integrate(sampleTm1,this);
    }
}
