package sensors.MPU9250;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.sun.deploy.util.ArrayUtil;
import com.sun.deploy.util.SystemUtils;
import javafx.geometry.Point3D;

import java.io.IOException;
import java.util.ArrayList;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public class MPU9250
{
    private class I2CWriteOperation
    {
        private final int address;
        private final int value;
        private I2CWriteOperation(int address, int value)
        {
            this.address = address;
            this.value = value;
        }

        private int getAddress()
        {
            return address;
        }

        private byte getEncodedValue()
        {
            return (byte) value;
        }
    }

    private Point3D accel;
    private Point3D gyro;
    private Point3D mag;
    private float temp;

    private final I2CDevice mpu9250;

    private static final double ACCEL_SCALE = 4.0/32768.0;

    private static final double GYRO_SCALE = 2000.0/32768.0;

    private static final double MAG_SCALE = 10.0*4912.0/32760.0;

    public MPU9250(int address) throws I2CFactory.UnsupportedBusNumberException, IOException, InterruptedException
    {
        // get device
        I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
        mpu9250 = bus.getDevice(address);

        ArrayList<I2CWriteOperation> operations = new ArrayList<>();

        //operations.add(new I2CWriteOperation(MPU9250Registers.PWR_MGMT_1.getValue(),0x80)); // reset device
        operations.add(new I2CWriteOperation(MPU9250Registers.PWR_MGMT_1.getValue(),0x01)); // reset device

        operations.add(new I2CWriteOperation(MPU9250Registers.PWR_MGMT_1.getValue(),0x01)); // clock source
        operations.add(new I2CWriteOperation(MPU9250Registers.PWR_MGMT_2.getValue(),0x00)); // enable acc and gyro

        //operations.add(new I2CWriteOperation(MPU9250Registers.CONFIG.getValue(),0x01)); // use DLPF set gyroscope bandwidth 184Hz
        operations.add(new I2CWriteOperation(MPU9250Registers.CONFIG.getValue(),0x03));
        operations.add(new I2CWriteOperation(MPU9250Registers.SMPLRT_DIV.getValue(), 0x04));

        //operations.add(new I2CWriteOperation(MPU9250Registers.GYRO_CONFIG.getValue(),0x18)); // +-2000dps
        operations.add(new I2CWriteOperation(MPU9250Registers.GYRO_CONFIG.getValue(),0x18));

        operations.add(new I2CWriteOperation(MPU9250Registers.ACCEL_CONFIG.getValue(),0x08)); // +-4G

        //operations.add(new I2CWriteOperation(MPU9250Registers.ACCEL_CONFIG2.getValue(),0x09)); // set acc data rates, enable acc LPF, bandwidth 184Hz
        operations.add(new I2CWriteOperation(MPU9250Registers.ACCEL_CONFIG2.getValue(),0x15)); // set acc data rates, enable acc LPF, bandwidth 184Hz
        operations.add(new I2CWriteOperation(MPU9250Registers.INT_PIN_CFG.getValue(),0x30));

        operations.add(new I2CWriteOperation(MPU9250Registers.USER_CTRL.getValue(),0x20)); // I2C master mode
        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_MST_CTRL.getValue(),0x0D)); // I2C configuration multi-master IIC 400KHz

        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_ADDR.getValue(),0x0C)); // set the I2C slave address of AK8963

        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_REG.getValue(),0x0B)); // I2C slave 0 register address from where to begin
        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_DO.getValue(),0x01)); // reset AK8963
        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_CTRL.getValue(),0x81)); // Enable I2C and set 1 byte

        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_REG.getValue(),0x0A)); // I2C slave 0 register address from where to begin
        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_DO.getValue(),0x12)); // register value to continuous measurement 16 bit
        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_CTRL.getValue(),0x81)); // Enable I2C and set 1 byte

        operations.add(new I2CWriteOperation(MPU9250Registers.GYRO_CONFIG.getValue(),0x18)); // +-2000dps
        operations.add(new I2CWriteOperation(MPU9250Registers.ACCEL_CONFIG.getValue(),0x08)); // +-4G

        for(I2CWriteOperation operation:operations)
        {
            mpu9250.write(operation.getAddress(), operation.getEncodedValue());
            Thread.sleep(100);
        }
    }

    private void updateAccel()
    {
        try
        {
            while(!isDataReady()) Thread.sleep(1);
            Point3D raw = new Point3D(
                    getData(MPU9250Registers.ACCEL_XOUT_H.getValue()),
                    getData(MPU9250Registers.ACCEL_YOUT_H.getValue()),
                    getData(MPU9250Registers.ACCEL_ZOUT_H.getValue()));
            this.accel = new Point3D(
                    raw.getX()*ACCEL_SCALE,
                    raw.getY()*ACCEL_SCALE,
                    raw.getZ()*ACCEL_SCALE);
        } catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }

    }

    private void updateGyro()
    {
        try
        {
            while(!isDataReady()) Thread.sleep(1);
            Point3D raw = new Point3D(
                    getData(MPU9250Registers.GYRO_XOUT_H.getValue()),
                    getData(MPU9250Registers.GYRO_YOUT_H.getValue()),
                    getData(MPU9250Registers.GYRO_ZOUT_H.getValue()));
            this.gyro = new Point3D(
                    raw.getX()*GYRO_SCALE,
                    raw.getY()*GYRO_SCALE,
                    raw.getZ()*GYRO_SCALE);
        } catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void updateMag()
    {
        try
        {
            while(!isDataReady()) Thread.sleep(1);
            mpu9250.write(MPU9250Registers.I2C_SLV0_ADDR.getValue(),(byte)((byte)0x0c|(byte)0x80));
            mpu9250.write(MPU9250Registers.I2C_SLV0_REG.getValue(), (byte)0x03);
            mpu9250.write(MPU9250Registers.I2C_SLV0_CTRL.getValue(), (byte)0x87);

            Thread.sleep(10);

            byte[] data = new byte[7];
            mpu9250.read(MPU9250Registers.EXT_SENS_DATA_00.getValue(),data,0,7);
            Point3D raw = new Point3D(
                    (double)(data[1]<<8 | data[0]),
                    (double)(data[3]<<8 | data[2]),
                    (double)(data[5]<<8 | data[4]));
            this.mag = new Point3D(
                    raw.getX()*MAG_SCALE,
                    raw.getY()*MAG_SCALE,
                    raw.getZ()*MAG_SCALE);
        } catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void updateTemp()
    {
        try
        {
            while(!isDataReady()) Thread.sleep(1);
            temp = getData(MPU9250Registers.TEMP_OUT_H.getValue());
        } catch (IOException | InterruptedException e)
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
        updateMag();
        return mag;
    }

    public float getTemp()
    {
        updateTemp();
        return temp;
    }

    private boolean isDataReady() throws IOException
    {
        return true;
        //return (mpu9250.read(MPU9250Registers.INT_STATUS.getValue()) & 0x01) == 0;//readByte(MPU9250_ADDRESS, INT_STATUS) & 0x01
    }

    public float getHeading()
    {
        double heading = 0f;
        heading = Math.atan2(mag.getY(), mag.getX());
        if (heading < 0) heading += (2 * Math.PI);
        return (float) Math.toDegrees(heading);
    }
}
