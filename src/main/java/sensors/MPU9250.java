package sensors;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import javafx.geometry.Point3D;

import java.io.IOException;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public class MPU9250
{
    private Point3D accel;

    private Point3D gyro;

    private Point3D mag;

    private float temp;

    private final I2CDevice mpu9250;

    public MPU9250(int address) throws I2CFactory.UnsupportedBusNumberException, IOException
    {
        // get device
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
        mpu9250 = bus.getDevice(address);

       mpu9250.read(0);

        // configure device
        mpu9250.write(27,(byte)0x18); // set gyroscope to full scale
        mpu9250.write(28,(byte)0x18); // set accelerometer to full scale
        mpu9250.write(55,(byte)0x00); // set pass mode for magnetometer
        mpu9250.write(10,(byte)0x06); // set magnetometer mode 2:continuous 1 6: continuous 2 1: single
        //mpu9250.write(11,(byte)0x00); // disable reset
    }

    private void updateAccel()
    {
        try
        {
            this.accel = new Point3D(getData(59),getData(61),getData(63));
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private void updateGyro()
    {
        try
        {
            this.gyro = new Point3D(getData(67),getData(69),getData(71));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void updateMag()
    {
        try
        {
            /*for(int i = 73; i<=96; i++)
            {
                System.out.println(i + " : " + Integer.toBinaryString(mpu9250.read(i)));
            }
            System.out.println("55 : " + Integer.toBinaryString(mpu9250.read(55)));*/
            this.mag = new Point3D(getData(5),getData(3),getData(7));
            mpu9250.read(9);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void updateTemp()
    {
        try
        {
            temp = getData(65);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private float getData(int address) throws IOException
    {
        byte high = (byte)mpu9250.read(address);
        byte low = (byte)mpu9250.read(address);
        return ((float)(high<<8 &0xFF00 | low&0xFF))/100; // construct 16 bit integer from two bytes
    }

    public Point3D getAccel()
    {
        updateAccel();
        return accel;
    }

    public Point3D getGyro()
    {
        updateGyro();
        return gyro;
    }

    public Point3D getMag()
    {
        updateMag();
        return mag;
    }

    public float getTemp()
    {
        updateTemp();
        return temp;
    }
}
