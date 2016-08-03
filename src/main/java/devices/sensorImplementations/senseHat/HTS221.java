package devices.sensorImplementations.senseHat;

import devices.I2C.I2CImplementation;


/**
 * RPITank - devices.sensorImplementations.senseHat
 * Created by matthew on 03/08/16.
 */
public class HTS221
{

    private final I2CImplementation hts221;

    public HTS221(I2CImplementation hts221) throws Exception {
        this.hts221 = hts221;

        int avConf = 27; // 00011011

        int reg1 = 130; // 10000010 active mode, update continuous 7Hz data
        int reg2 = 0; // 00000000 normal mode, heater off, one shot off
        int reg3 = 0; // 00000000 DR high, push pull, DR disabled

        hts221.write(0x10, (byte)avConf); // write AV_CONF

        hts221.write(0x20, (byte)reg1); // write CTRL_REG1
        hts221.write(0x21, (byte)reg2); // write CTRL_REG2
        hts221.write(0x22, (byte)reg3); // write CTRL_REG3

        Thread.sleep(1000);

        System.out.print("Status: ");
        System.out.println(Integer.toHexString(hts221.read(0x27)));

        for(int x = 0; x<10; x++)
        {
            System.out.println();

            System.out.print("Humidity: ");
            System.out.println((short) ((hts221.read(0x29) << 8) | hts221.read(0x28))/256f);

            System.out.print("Temperature: ");
            System.out.println((short) ((hts221.read(0x2B) << 8) | hts221.read(0x2A))/64f);
            Thread.sleep(500);
        }
    }

}
