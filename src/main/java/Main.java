

import com.pi4j.io.i2c.I2CFactory;
import sensors.MPU9250.MPU9250;
import sensors.dataTypes.Data3D;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
public class Main
{
    public static void main(String[] args) throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException
    {
        MPU9250 mpu9250 = new MPU9250(100,100);
        new Thread(mpu9250).start();
        String format = "%+04.3f";

        Thread.sleep(100);

        for(int x=0; x<1000; x++)
        {
            Data3D mag = mpu9250.getLatestGaussianData();
            Data3D acc = mpu9250.getLatestAcceleration();
            Data3D gyr = mpu9250.getLatestRotationalAcceleration();
            System.out.print(  "acc: " + acc.toString());
            System.out.print(  "gyr: " + gyr.toString());
            System.out.println("mag: " + mag.toString());
            Thread.sleep(10);
        }
    }
}
