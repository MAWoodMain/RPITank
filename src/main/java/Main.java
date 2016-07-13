

import com.pi4j.io.i2c.I2CFactory;
import sensors.MPU9250.MPU9250;
import sensors.MPU9250.MPU9250_Oracle;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
class Main
{
    public static void main(String[] args) throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException
    {
        MPU9250_Oracle mpu9250 = new MPU9250_Oracle(100,100);
        Thread sensor = new Thread(mpu9250);
        sensor.start();

        Thread.sleep(5000);

        sensor.interrupt();
    }
}
