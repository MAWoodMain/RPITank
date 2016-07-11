package sensors;

/**
 * RPITank - sensors
 * Created by matthew on 10/07/16.
 */
public class Data3D<T> {

    private T x;
    private T y;
    private T z;

    public Data3D(T x, T y, T z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public T getX() {
        return x;
    }

    public T getY() {
        return y;
    }

    public T getZ() {
        return z;
    }

	public void setX(T x) {
		this.x = x;
	}

	public void setY(T y) {
		this.y = y;
	}

	public void setZ(T z) {
		this.z = z;
	}
}
