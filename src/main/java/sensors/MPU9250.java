package sensors;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.sun.deploy.util.ArrayUtil;
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

    public MPU9250(int address) throws I2CFactory.UnsupportedBusNumberException, IOException, InterruptedException
    {
        // get device
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
        mpu9250 = bus.getDevice(address);

        mpu9250.write(0x6B,(byte)0x80); // reset device
        Thread.sleep(10);
        mpu9250.write(0x6B,(byte)0x01); // clock source
        Thread.sleep(10);
        mpu9250.write(0x6C,(byte)0x00); // enable acc and gyro
        Thread.sleep(10);
        mpu9250.write(0x1A,(byte)0x01); // use DLPF set gyroscope bandwidth 184Hz
        Thread.sleep(10);
        mpu9250.write(0x1B,(byte)0x18); // +-2000dps
        Thread.sleep(10);
        mpu9250.write(0x1C,(byte)0x08); // +-4G
        Thread.sleep(10);
        mpu9250.write(0x1D,(byte)0x09); // set acc data rates, enable acc LPF, bandwidth 184Hz
        Thread.sleep(10);
        mpu9250.write(0x37,(byte)0x30); //
        Thread.sleep(10);
        mpu9250.write(0x6A,(byte)0x20); // I2C master mode
        Thread.sleep(10);
        mpu9250.write(0x24,(byte)0x0D); // I2C configuration multi-master IIC 400KHz
        Thread.sleep(10);

        mpu9250.write(0x25,(byte)0x0C); // set the I2C slave address of AK8963
        Thread.sleep(10);

        mpu9250.write(0x26,(byte)0x0B); // I2C slave 0 register address from where to begin
        Thread.sleep(10);
        mpu9250.write(0x63,(byte)0x01); // reset AK8963
        Thread.sleep(10);
        mpu9250.write(0x27,(byte)0x81); // Enable I2C and set 1 byte
        Thread.sleep(10);

        mpu9250.write(0x26,(byte)0x0A); // I2C slave 0 register address from where to begin
        Thread.sleep(10);
        mpu9250.write(0x63,(byte)0x12); // register value to continuous measurement 16 bit
        Thread.sleep(10);
        mpu9250.write(0x27,(byte)0x81); // Enable I2C and set 1 byte
        Thread.sleep(10);

        mpu9250.write(0x1B, (byte)0x18); // +-2000dps
        Thread.sleep(10);
        mpu9250.write(0x1C, (byte)0x08); // +-4G
        Thread.sleep(100);
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

    private void updateMag() throws InterruptedException
    {
        try
        {
            mpu9250.write(0x25,(byte)((byte)0x0c|(byte)0x80));
            mpu9250.write(0x26, (byte)0x03);
            mpu9250.write(0x27, (byte)0x87);

            Thread.sleep(10);

            byte[] data = new byte[7];
            mpu9250.read(0x49,data,0,7);
            this.mag = new Point3D(
                    (float)(data[0]<<8 | data[1]),
                    (float)(data[2]<<8 | data[3]),
                    (float)(data[4]<<8 | data[5]));
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
        byte low = (byte)mpu9250.read(address + 1);
        return ((float)(high<<8 | low)); // construct 16 bit integer from two bytes
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
        try
        {
            updateMag();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        return mag;
    }

    public float getTemp()
    {
        updateTemp();
        return temp;
    }

    public float getHeading()
    {
        double heading = 0f;
        heading = Math.atan2(mag.getY(), mag.getX());
        if (heading < 0) heading += (2 * Math.PI);
        return (float) Math.toDegrees(heading);
    }
}
