package sensors.interfaces;

import sensors.dataTypes.TimestampedData3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Magnetometer
{
    float getHeading();
    TimestampedData3D getLatestGaussianData();
    TimestampedData3D getGaussianData(int i);
    int getMagnetometerReadingCount();
    float getMaxGauss();
    float getMinGauss();
    void updateMagnetometerData() throws Exception;
}
