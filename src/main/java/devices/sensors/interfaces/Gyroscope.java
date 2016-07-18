package devices.sensors.interfaces;

import devices.sensors.dataTypes.TimestampedData3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Gyroscope
{
    TimestampedData3D getLatestRotationalAcceleration();
    TimestampedData3D getRotationalAcceleration(int i);
    int getGyroscopeReadingCount();

    void updateGyroscopeData() throws Exception;
}
