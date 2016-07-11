package sensors;

import javafx.geometry.Point3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Accelerometer
{
    Data3D getLatestAcceleration();
    Data3D getAcceleration(int i);
    int getReadingCount();
    float getMaxAcceleration();
    float getMinAcceleration();
}
