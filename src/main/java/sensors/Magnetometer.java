package sensors;

import javafx.geometry.Point3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Magnetometer
{
    float getHeading();
    Data3D getLatestGaussianData();
    Data3D getGaussianData(int i);
    int getReadingCount();
    float getMaxGauss();
    float getMinGauss();
}
