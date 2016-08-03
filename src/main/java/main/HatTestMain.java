package main;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import devices.I2C.I2CImplementation;
import devices.I2C.Pi4jI2CDevice;
import devices.sensorImplementations.senseHat.HTS221;

/**
 * RPITank - main
 * Created by matthew on 03/08/16.
 */
public class HatTestMain
{

    public static void main(String[] args) throws Exception
    {
        I2CImplementation hts221 =
                new Pi4jI2CDevice(
                        I2CFactory.getInstance(I2CBus.BUS_1).getDevice(0x5F));
        new HTS221(hts221);
    }
}
