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
                    100,                                    // sample rate
                    100); 									// sample size
            System.out.println("MPU9250 created");
            Thread sensor = new Thread(mpu9250);
            sensor.start();

            Thread.sleep(10000);
            System.out.println("back from sleep");
            sensor.interrupt();
            int ac = mpu9250.getAccelerometerReadingCount();
            System.out.println("AccReadingCount "+ac);
            for(int i = ac -1; i>0; i--)
            {
                System.out.println(" A: " + mpu9250.getAcceleration(i).toString());
            }
            
            int gc = mpu9250.getGyroscopeReadingCount();
            System.out.println("GyroReadingCount "+gc);
            for(int i = gc -1; i>0; i--)
            {
                System.out.println("G: " + mpu9250.getRotationalAcceleration(i).toString());
            }
            
            int mc = mpu9250.getMagnetometerReadingCount();
            System.out.println("MagReadingCount "+mc);
            for(int i = mc -1; i>0; i--)
            {
               System.out.println(" M: " + mpu9250.getGaussianData(i).toString());
            }
        } catch (I2CFactory.UnsupportedBusNumberException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

}
