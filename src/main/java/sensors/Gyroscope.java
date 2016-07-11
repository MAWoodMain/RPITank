package sensors;

import javafx.geometry.Point3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Gyroscope
{
    Data3D<Float> getLatestRotationalAcceleration();
    Data3D<Float> getRotationalAcceleration(int i);
    int getReadingCount();
    float getMaxRotationalAcceleration();
    float getMinRotationalAcceleration();
}
