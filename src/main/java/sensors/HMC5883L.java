package sensors; /**
 * RPITank
 * Created by MAWood on 03/07/2016.
 */

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import javafx.geometry.Point3D;

import java.io.IOException;

/*
 * 3 Axis compass
 */
public class HMC5883L implements Magnetometer
{
    private final static int HMC5883L_ADDRESS                       = 0x1E;

    private final static int HMC5883L_CONTINUOUS_SAMPLING           = 0x00;
    private final static int HMC5883L_13_GAIN_LSB_GAUSS_1090        = 0x20; // 1.3 gain LSb / Gauss 1090 (default)
    private final static int HMC5883L_8_SAMPLES_15HZ                = 0x70; // Set to 8 samples @ 15Hz.

    private final static int HMC5883L_X_ADR                         = 0x03;
    private final static int HMC5883L_Y_ADR                         = 0x07;
    private final static int HMC5883L_Z_ADR                         = 0x05;

    private final static float SCALE = 0.92f;

    private I2CBus bus;
    private I2CDevice hcm5883l;

    public HMC5883L() throws I2CFactory.UnsupportedBusNumberException
    {
        this(HMC5883L_ADDRESS);
    }

    public HMC5883L(int address) throws I2CFactory.UnsupportedBusNumberException
    {
        try
        {
            // Get i2c bus
            bus = I2CFactory.getInstance(I2CBus.BUS_1); // Depends onthe RasPI version
            // Get device itself
            hcm5883l = bus.getDevice(address);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // Complement to 2
    private short readWord_2C(int reg) throws IOException
    {
        short w = 0;

        byte high = (byte)(hcm5883l.read(reg) & 0xFF);
        byte low  = (byte)(hcm5883l.read(reg + 1) & 0xFF);

        w = (short)(((high << 8) + low) & 0xFFFF); // Little endian

        if (w >= 0x8000)
            w = (short) -((0xFFFF - w) + 1);

        return w;
    }

    public float getHeading()
    {
        double heading = 0f;

        byte[] w = new byte[] { (byte)HMC5883L_8_SAMPLES_15HZ,
                (byte)HMC5883L_13_GAIN_LSB_GAUSS_1090,
                (byte)HMC5883L_CONTINUOUS_SAMPLING };
        try
        {
            hcm5883l.write(w, 0, 3); // BeginTrans, write 3 bytes, EndTrans.

            double xOut = readWord_2C(HMC5883L_X_ADR) * SCALE;
            double yOut = readWord_2C(HMC5883L_Y_ADR) * SCALE;
            double zOut = readWord_2C(HMC5883L_Z_ADR) * SCALE;

            heading = Math.atan2(yOut, xOut);
            if (heading < 0)
                heading += (2 * Math.PI);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return (float) Math.toDegrees(heading);
    }

    @Override
    public float getMaxGauss()
    {
        return 0;
    }

    @Override
    public float getMinGauss()
    {
        return 0;
    }

    @Override
    public Point3D getGaussianData()
    {
        try
        {
            return new Point3D(
                    readWord_2C(HMC5883L_X_ADR) * SCALE,
                    readWord_2C(HMC5883L_Y_ADR) * SCALE,
                    readWord_2C(HMC5883L_Z_ADR) * SCALE);
        } catch (IOException e)
        {
            e.printStackTrace();
            return new Point3D(0,0,0);
        }
    }

    public void close()
    {
        try { this.bus.close(); }
        catch (IOException ioe) { ioe.printStackTrace(); }
    }

    protected static void waitfor(long howMuch)
    {
        try
        {
            Thread.sleep(howMuch);
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
    }
}