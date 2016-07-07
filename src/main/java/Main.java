

import com.pi4j.io.i2c.I2CFactory;
import javafx.geometry.Point3D;
import sensors.HMC5883L;
import sensors.MPU9250;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
public class Main
{
    public static void main(String[] args) throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException
    {
        MPU9250 mpu9250 = new MPU9250(104);

        while(true)
        {
           /* System.out.println("Gyro: " + mpu9250.getGyro());
            System.out.println("Acce: " + mpu9250.getAccel());
            System.out.println("magn: " + mpu9250.getMag());*/
            System.out.println("temp: " + mpu9250.getTemp());
            Thread.sleep(500);
        }
    }
}
