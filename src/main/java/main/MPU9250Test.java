package main;

import java.io.IOException;

//import com.pi4j.io.gpio.GpioController;
//import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import devices.I2C.Pi4jI2CDevice;
import devices.sensorImplementations.MPU9250.MPU9250;

public class MPU9250Test {

    public static void main(String[] args)
    {
    	System.out.println("Start MPU9250Test main()");
        I2CBus bus = null;
    	//System.out.println("Attempt to get Bus 1");
        try {
        	//final GpioController gpio = GpioFactory.getInstance();
            bus = I2CFactory.getInstance(I2CBus.BUS_1); 
            System.out.println("Bus acquired");
            MPU9250 mpu9250 = new MPU9250(
                    new Pi4jI2CDevice(bus.getDevice(0x68)), // MPU9250 I2C device
                    new Pi4jI2CDevice(bus.getDevice(0x0C)), // ak8963 I2C 
                    10,                                     // sample rate per second
                    100); 									// sample size
            System.out.println("MPU9250 created");
            Thread sensor = new Thread(mpu9250);
            sensor.start();

            Thread.sleep(15000);
            System.out.println("back from sleep");
            int ac = mpu9250.getAccelerometerReadingCount();
            System.out.println("AccReadingCount "+ac);
            for(int i = ac -1; i>=0; i--)
            {
                System.out.println(" A: " + mpu9250.getAcceleration(i).toString());
            }
            System.out.println("Average Acceleration " + mpu9250.getAvgAcceleration().toString());
            
            int gc = mpu9250.getGyroscopeReadingCount();
            System.out.println("GyroReadingCount "+gc);
            for(int i = gc -1; i>=0; i--)
            {
                System.out.println("G: " + mpu9250.getRotationalAcceleration(i).toString());
            }
            System.out.println("Average Rotation " + mpu9250.getAvgRotationalAcceleration().toString());
            
            int mc = mpu9250.getMagnetometerReadingCount();
            System.out.println("MagReadingCount "+mc);
            for(int i = mc -1; i>=0; i--)
            {
               System.out.println(" M: " + mpu9250.getGaussianData(i).toString());
            }
            System.out.println("Average Gauss " + mpu9250.getAvgGauss().toString());
            
            int tc = mpu9250.getThermometerReadingCount();
            System.out.println("ThermReadingCount "+tc);
            System.out.println(" Average Temperature: " + mpu9250.getAvgTemperature() + " F");
            
           Thread.sleep(1000);
            sensor.interrupt();
            Thread.sleep(1000);
            bus.close();
        } catch (I2CFactory.UnsupportedBusNumberException | InterruptedException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

}
