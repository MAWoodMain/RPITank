package sensors.interfaces;

import sensors.dataTypes.TimestampedData3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Gyroscope
{
    TimestampedData3D getLatestRotationalAcceleration();
    TimestampedData3D getRotationalAcceleration(int i);
    int getGyroscopeReadingCount();
    float getMaxRotationalAcceleration();
    float getMinRotationalAcceleration();
    void updateGyroscopeData() throws Exception;
}
