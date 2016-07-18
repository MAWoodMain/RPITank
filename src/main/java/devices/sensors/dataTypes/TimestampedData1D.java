package devices.sensors.dataTypes;

/**
 * RPITank - devices.sensors.dataTypes
 * Created by MAWood on 18/07/2016.
 */
public class TimestampedData1D extends Data1D
{
    public static final long NANOS_PER_SEC = 1000000000;
    public final long nanoTime;

    public TimestampedData1D(float x, long nanoTime)
    {
        super(x);
        this.nanoTime = nanoTime;
    }

    public TimestampedData1D(float x)
    {
        this(x, System.nanoTime());
    }

    public TimestampedData1D(Data3D data)
    {
        this(data.getX());
    }

    public String toString()
    {
        String format = "%+04.4f";
        return 	" t: " + String.format(format,(float)(nanoTime/NANOS_PER_SEC)) +
                " x: " + String.format(format,this.getX());
    }

    public static TimestampedData1D integrate(TimestampedData1D sampleT, TimestampedData1D sampleTm1 )
    {
        final float deltaT = (float)(sampleT.nanoTime-sampleTm1.nanoTime)/(float)TimestampedData3D.NANOS_PER_SEC; // time difference between samples in seconds

        return new TimestampedData1D(
                (sampleT.getX()+sampleTm1.getX())/2f*deltaT,//Trapezoidal area, average height X deltaT
                sampleT.nanoTime); // preserve timestamp in result
    }

    public TimestampedData1D integrate(TimestampedData1D sampleTm1)
    {
        return integrate(sampleTm1,this);
    }
}
