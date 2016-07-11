package sensors;

/**
 * RPITank - sensors
 * Created by matthew on 10/07/16.
 */
public class Data3D {

    private float x;
    private float y;
    private float z;

    public Data3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void setZ(float z) {
		this.z = z;
	}
	public void normalize(){
		float norm;
		// Normalise accelerometer measurement
		norm = (float)Math.sqrt(x*x + y*y + z*z);
		if (norm == 0.0f)
			return; // handle NaN
		norm = 1.0f / norm;
		x *= norm;
		y *= norm;
		z *= norm;
		
	}
}
