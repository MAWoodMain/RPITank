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

        // configure device
        mpu9250.write(27,(byte)0x18); // set gyroscope to full scale
        mpu9250.write(28,(byte)0x18); // set accelerometer to full scale
        mpu9250.write(55,(byte)0x02); // set pass mode for magnetometer
        mpu9250.write(10,(byte)0x01); // request first magnetometer measurement
    }

    private void updateAccel()
    {
        try
        {
            byte[] buffer = new byte[6];
            mpu9250.read(59,buffer,0,6); // read gyro and acc data
            int accX,accY,accZ;
            accX = getInt(buffer,0);
            accY = getInt(buffer,2);
            accZ = getInt(buffer,4);
            this.accel = new Point3D(accX,accY,accZ);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    private void updateGyro()
    {
        try
        {
            byte[] buffer = new byte[6];
            mpu9250.read(67,buffer,0,6); // read gyro and acc data
            int gyroX,gyroY,gyroZ;
            gyroX = getInt(buffer,0);
            gyroY = getInt(buffer,2);
            gyroZ = getInt(buffer,4);
            this.gyro = new Point3D(gyroX,gyroY,gyroZ);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void updateMag()
    {
        try
        {
            byte[] buffer = new byte[7];
            mpu9250.read(3,buffer,0,7);

            int magX,magY,magZ;
            magX = buffer[3]<<8 &0xFF00 | buffer[2]&0xFF; // constructs 16 bit integer from two bytes
            magY = buffer[1]<<8 &0xFF00 | buffer[0]&0xFF;
            magZ = buffer[5]<<8 &0xFF00 | buffer[4]&0xFF;
            this.mag = new Point3D(magX,magY,magZ);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void updateTemp()
    {

    }

    private int getInt(byte[] arr, int offset)
    {
        return arr[offset]<<8 &0xFF00 | arr[offset+1]&0xFF; // construct 16 bit integer from two bytes
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
