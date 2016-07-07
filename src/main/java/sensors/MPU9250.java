package sensors;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import javafx.geometry.Point3D;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public class MPU9250
{
    private Point3D accel;

    private Point3D gyro;

    private Point3D mag;

    private final I2CDevice mpu9250;

    public MPU9250(int address) throws I2CFactory.UnsupportedBusNumberException, IOException
    {
        // get device
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
        mpu9250 = bus.getDevice(address);

        // configure device

    }

    private void updateAccel()
    {

    }

    private void updateGyro()
    {

    }

    private void updateMag()
    {

    }

    public Point3D getAccelX()
    {
        updateAccel();
        return accel;
    }

    public Point3D getGyroX()
    {
        updateGyro();
        return gyro;
    }

    public Point3D getMagX()
    {
        updateMag();
        return mag;
    }
}
