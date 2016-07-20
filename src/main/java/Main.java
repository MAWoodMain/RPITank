import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import devices.sensorImplementations.MPU9250.MPU9250;
import devices.I2C.Pi4jI2CDevice;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
class Main
{
    public static void main(String[] args)
    {
        I2CBus bus = null;
        try {
            bus = I2CFactory.getInstance(I2CBus.BUS_0);

            MPU9250 mpu9250 = new MPU9250(
                    new Pi4jI2CDevice(bus.getDevice(0x68)), // MPU9250 I2C device
                    new Pi4jI2CDevice(bus.getDevice(0x0C)), // ak8963 I2C device
                    100,                                    // sample rate
                    100);                                   // sample size
            Thread sensor = new Thread(mpu9250);
            sensor.start();

            Thread.sleep(10000);

            sensor.interrupt();

            for(int i = mpu9250.getAccelerometerReadingCount() -1; i>0; i--)
            {
                System.out.print("G: " + mpu9250.getRotationalAcceleration(i).toString());
                System.out.print(" A: " + mpu9250.getAcceleration(i).toString());
                System.out.println(" M: " + mpu9250.getGaussianData(i).toString());
            }
        } catch (I2CFactory.UnsupportedBusNumberException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
