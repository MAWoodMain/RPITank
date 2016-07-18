package devices.sensors.dataTypes;

/**
 * RPITank - sensors
 * Created by matthew on 10/07/16.
 */
public class Data3D extends Data2D{

    private float z;

    public Data3D(float x, float y, float z) {
    	super(x,y);
        this.z = z;
    }

    public float getZ() {
        return z;
    }

	public void setZ(float z) {
		this.z = z;
	}

    public void scale(float xScale,float yScale,float zScale)
    {
        super.scale(xScale,yScale);
        z *= zScale;
    }

    public void offset(float xOffset,float yOffset,float zOffset)
    {
        super.offset(xOffset,yOffset);
        z += zOffset;
    }

    public void normalize(){
		float norm;
		// Normalise measurements
		norm = (float)Math.sqrt(x*x + y*y + z*z);
		if (norm == 0.0f)
			throw new ArithmeticException(); // handle NaN
		norm = 1f / norm;
		x *= norm;
		y *= norm;
		z *= norm;
		
	}

	public String toString()
	{
		final String format = "%+04.3f";
		return 	super.toString() + " z: " + String.format(format,z);
	}
}
