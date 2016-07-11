package sensors.interfaces;

import sensors.dataTypes.Data3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Gyroscope
{
    Data3D getLatestRotationalAcceleration();
    Data3D getRotationalAcceleration(int i);
    int getReadingCount();
    float getMaxRotationalAcceleration();
    float getMinRotationalAcceleration();
}