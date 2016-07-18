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
        return 	" t: " + String.format(format,(float)(nanoTime/NANOS_PER_SEC)) +
                " x: " + String.format(format,this.getX()) +
                " y: " + String.format(format,this.getY()) +
                " z: " + String.format(format,this.getZ());
    }

    public static TimestampedData3D integrate(TimestampedData3D sampleT, TimestampedData3D sampleTm1 )
    {
        float deltaT = (float)(sampleT.nanoTime-sampleTm1.nanoTime)/(float)TimestampedData3D.NANOS_PER_SEC; // time difference between samples in seconds

        TimestampedData3D integral = new TimestampedData3D(sampleT); // preserve timestamp in result

        integral.setX((sampleT.getX()+sampleTm1.getX())/2f*deltaT);  //Trapezoidal area, average height X deltaT
        integral.setY((sampleT.getY()+sampleTm1.getY())/2f*deltaT);
        integral.setZ((sampleT.getZ()+sampleTm1.getZ())/2f*deltaT);

        return integral;
    }

    public TimestampedData3D integrate(TimestampedData3D sampleTm1 )
    {
        float deltaT = (float)(this.nanoTime-sampleTm1.nanoTime)/(float)TimestampedData3D.NANOS_PER_SEC; // time difference between samples in seconds

        TimestampedData3D integral = new TimestampedData3D(this); // preserve timestamp in result

        integral.setX((this.getX()+sampleTm1.getX())/2f*deltaT);  //Trapezoidal area, average height X deltaT
        integral.setY((this.getY()+sampleTm1.getY())/2f*deltaT);
        integral.setZ((this.getZ()+sampleTm1.getZ())/2f*deltaT);

        return integral;
    }

}