

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
        int i = 0;
           // System.out.println("Gyro: " + mpu9250.getGyro());

            i++;
            Point3D gyro = mpu9250.getGyro();
            Point3D mag = mpu9250.getMag();
            Point3D accel = mpu9250.getAccel();
            /*System.out.println(i + ", " + gyro.getX() + ", " + gyro.getY() + ", " + gyro.getZ() + ", " +
            mag.getX() + ", " + mag.getY() + ", " + mag.getZ() + ", " +
            accel.getX() + ", " + accel.getY() + ", " + accel.getZ());*/
            System.out.println(mag);
            Thread.sleep(100);
    }
}
