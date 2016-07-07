package sensors;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public abstract class InertialMeasurementUnit
{
    public abstract float getHeading();
    public abstract float getYaw();
    public abstract float getRoll();
}
