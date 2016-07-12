package sensors.interfaces;

import sensors.dataTypes.TimestampedData3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Accelerometer
{
    TimestampedData3D getLatestAcceleration();
    TimestampedData3D getAcceleration(int i);
    int getAccelerometerReadingCount();
    float getMaxAcceleration();
    float getMinAcceleration();
    void updateAccelerometerData() throws Exception;
}
