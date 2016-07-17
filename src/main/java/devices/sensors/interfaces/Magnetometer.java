package devices.sensors.interfaces;

import devices.sensors.dataTypes.TimestampedData3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Magnetometer
{
    TimestampedData3D getLatestGaussianData();
    TimestampedData3D getGaussianData(int i);
    int getMagnetometerReadingCount();

    void updateMagnetometerData() throws Exception;
}
