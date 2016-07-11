package sensors;

import javafx.geometry.Point3D;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public interface Magnetometer
{
    float getHeading();
    Data3D<Float> getGaussianData();
    float getMaxGauss();
    float getMinGauss();
}
