

import com.pi4j.io.i2c.I2CFactory;
import sensors.MPU9250.MPU9250;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
public class Main
{
    public static void main(String[] args) throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException
    {
        MPU9250 mpu9250 = new MPU9250();
        String format = "%+04.3f";

        for(int x=0; x<1000; x++)
        {
            //Data3D mag = mpu9250.getLatestGaussianData();
            //Data3D acc = mpu9250.getLatestAcceleration();
            //Data3D gyr = mpu9250.getLatestRotationalAcceleration();
            //System.out.print(  "acc: " + String.format(format,acc.getX()) + ", " + String.format(format,acc.getY()) + ", " + String.format(format,acc.getZ()) + " ");
            //System.out.print(  "gyr: " + String.format(format,gyr.getX()) + ", " + String.format(format,gyr.getY()) + ", " + String.format(format,gyr.getZ()) + " ");
            //System.out.println("mag: " + String.format(format,mag.getX()) + ", " + String.format(format,mag.getY()) + ", " + String.format(format,mag.getZ()) + " ");
            //System.out.println(mpu9250.getHeading());
            Thread.sleep(100);
        }
    }
}
