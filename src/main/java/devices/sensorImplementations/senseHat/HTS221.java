package devices.sensorImplementations.senseHat;

import devices.I2C.I2CImplementation;

import java.io.IOException;


/**
 * RPITank - devices.sensorImplementations.senseHat
 * Created by matthew on 03/08/16.
 */
public class HTS221
{

    private final I2CImplementation hts221;
    private final short T0_degC;
    private final short T1_degC;
    private final short T0_OUT;
    private final short T1_OUT;

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

        final short T0_degC_x8_REG = 0x32;
        final short T1_degC_x8_REG = 0x33;
        final short T1T0MSB_REG = 0x35;
        final short T0_OUT_REG = 0x3C;
        final short T1_OUT_REG = 0x3E;

        byte MSB = hts221.read(T1T0MSB_REG);

        int T0_degC_x8 = (((int)(MSB & 0x03)) << 8) | ((int)hts221.read(T0_degC_x8_REG));
        T0_degC = (short)(T0_degC_x8 >> 3);

        System.out.println("T0_degC: " + T0_degC);

        int T1_degC_x8 = (((int)(MSB & 0x0C)) << 6) | ((int)hts221.read(T1_degC_x8_REG));
        T1_degC = (short)(T1_degC_x8 >> 3);

        System.out.println("T1_degC: " + T1_degC);

        T0_OUT = twoByteToSignedShort(
                new byte[]{hts221.read(T0_OUT_REG+1),hts221.read(T0_OUT_REG)});
        T1_OUT = twoByteToSignedShort(
                new byte[]{hts221.read(T1_OUT_REG+1),hts221.read(T1_OUT_REG)});
        System.out.println("T0_OUT: " + T0_OUT);
        System.out.println("T1_OUT: " + T1_OUT);

        for(int x = 0; x<50; x++)
        {
            System.out.println();
            System.out.print("Temperature: ");
            System.out.println(getTemp());
            Thread.sleep(500);
        }

    }

    private static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    private static short twoByteToSignedShort(byte[] b)
    {
        return (short) (((short)b[0] << 8) | (short)b[1]);
    }

    private float getTemp() throws IOException {

        final short T_OUT_REG = 0x2A;
        short T_OUT = twoByteToSignedShort(
                new byte[]{hts221.read(T_OUT_REG+1),hts221.read(T_OUT_REG)});

        int tmp = (T_OUT-T0_OUT)*((T1_degC-T0_degC)*10);

        return ((((float)tmp)/((float)(T1_OUT-T0_OUT)))+T0_degC*10)/10f;
    }
}
