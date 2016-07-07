package sensors;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public class MPU9250
{
    private double accelX;
    private double accelY;
    private double accelZ;

    private double gyroX;
    private double gyroY;
    private double gyroZ;

    private double magX;
    private double magY;
    private double magZ;

    private final I2CDevice mpu9250;

    public MPU9250(int address) throws I2CFactory.UnsupportedBusNumberException, IOException
    {
        // get device
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
        mpu9250 = bus.getDevice(address);

        // configure device

    }

    public double getAccelX()
    {
        return accelX;
    }

    public double getAccelY()
    {
        return accelY;
    }

    public double getAccelZ()
    {
        return accelZ;
    }

    public double getGyroX()
    {
        return gyroX;
    }

    public double getGyroY()
    {
        return gyroY;
    }

    public double getGyroZ()
    {
        return gyroZ;
    }

    public double getMagX()
    {
        return magX;
    }

    public double getMagY()
    {
        return magY;
    }

    public double getMagZ()
    {
        return magZ;
    }
}
