

import com.pi4j.io.i2c.I2CFactory;
import javafx.geometry.Point3D;
import sensors.MPU9250;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
public class Main
{
    public static void main(String[] args) throws IOException, I2CFactory.UnsupportedBusNumberException
    {
        MPU9250 mpu9250 = new MPU9250(68);

        System.out.println(mpu9250.getGyro());
    }
}
