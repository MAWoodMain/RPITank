package main;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import devices.sensorImplimentations.MPU9250.MPU9250;
import devices.I2C.Pi4jI2CDevice;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
class Main
{
    public static void main(String[] args) throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException
    {
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_0);

        MPU9250 mpu9250 = new MPU9250(
                new Pi4jI2CDevice(bus.getDevice(0x68)), // MPU9250 I2C device
                new Pi4jI2CDevice(bus.getDevice(0x0C)), // ak8963 I2C device
                100,                                    // sample rate
                100);                                   // sample size
        Thread sensor = new Thread(mpu9250);
        sensor.start();

        Thread.sleep(5000);

        sensor.interrupt();
    }
}
