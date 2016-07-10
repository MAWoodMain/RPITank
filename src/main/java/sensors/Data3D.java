package sensors;

/**
 * RPITank - sensors
 * Created by matthew on 10/07/16.
 */
public class Data3D<T> {

    public final T x;
    public final T y;
    public final T z;

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
}
