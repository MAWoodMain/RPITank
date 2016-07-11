package sensors.MPU9250;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import sensors.dataTypes.CircularArrayRing;
import sensors.dataTypes.Data3D;
import sensors.dataTypes.I2CWriteOperation;
import sensors.dataTypes.TimestampedData3D;
import sensors.interfaces.Accelerometer;
import sensors.interfaces.Gyroscope;
import sensors.interfaces.Magnetometer;
import sensors.interfaces.Thermometer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * RPITank
 * Created by MAWood on 07/07/2016.
 */
public class MPU9250 implements Accelerometer, Gyroscope, Magnetometer, Thermometer, Runnable
{
    private static final MagScale magScale = MagScale.MFS_16BIT;
    private static final GyrScale gyrScale = GyrScale.GFS_2000DPS;
    private static final AccScale accScale = AccScale.AFS_4G;

    private CircularArrayRing<TimestampedData3D> accel;
    private CircularArrayRing<TimestampedData3D> gyro;
    private CircularArrayRing<TimestampedData3D> mag;
    private CircularArrayRing<Float> temp;

    private boolean paused;

    private final I2CDevice mpu9250;

    public MPU9250(int address) throws I2CFactory.UnsupportedBusNumberException, IOException, InterruptedException
    {
        paused = true;
        accel = new CircularArrayRing<>();
        gyro = new CircularArrayRing<>();
        mag = new CircularArrayRing<>();
        temp = new CircularArrayRing<>();
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

        operations.add(new I2CWriteOperation(MPU9250Registers.GYRO_CONFIG.getValue(),gyrScale.getValue())); // set gyro resolution

        operations.add(new I2CWriteOperation(MPU9250Registers.ACCEL_CONFIG.getValue(),accScale.getValue())); // set accelerometer resolution

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
        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_DO.getValue(),magScale.getValue())); // register value to continuous measurement 16 bit
        operations.add(new I2CWriteOperation(MPU9250Registers.I2C_SLV0_CTRL.getValue(),0x81)); // Enable I2C and set 1 byte

        operations.add(new I2CWriteOperation(MPU9250Registers.GYRO_CONFIG.getValue(),gyrScale.getValue())); // set gyro resolution
        operations.add(new I2CWriteOperation(MPU9250Registers.ACCEL_CONFIG.getValue(),accScale.getValue())); // set accelerometer resolution

        executeOperations(operations);
        paused = false;
    }

    private void updateData()
    {

    }

    private int getData(int address) throws IOException
    {
        byte high = (byte)mpu9250.read(address);
        byte low = (byte)mpu9250.read(address + 1);
        return (high<<8 | low); // construct 16 bit integer from two bytes
    }

    private void executeOperations(List<I2CWriteOperation> operations) throws IOException
    {
        for(I2CWriteOperation operation:operations)
        {
            mpu9250.write(operation.getAddress(), operation.getEncodedValue());
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run()
    {
        while(!Thread.interrupted())
        {
            if(!paused) updateData();
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void pause()
    {
        paused = true;
    }

    public void unpause()
    {
        paused = false;
    }

    @Override
    public Data3D getLatestAcceleration()
    {
        return accel.get(0);
    }

    @Override
    public Data3D getAcceleration(int i)
    {
        return accel.get(i);
    }

    @Override
    public int getReadingCount()
    {
        return 0;
    }

    @Override
    public Data3D getLatestRotationalAcceleration()
    {
        return gyro.get(0);
    }

    @Override
    public Data3D getRotationalAcceleration(int i)
    {
        return gyro.get(i);
    }

    @Override
    public Data3D getLatestGaussianData()
    {
        return mag.get(0);
    }

    @Override
    public Data3D getGaussianData(int i)
    {
        return mag.get(i);
    }


    @Override
    public float getLatestTemperature()
    {
        return temp.get(0);
    }

    @Override
    public float getTemperature(int i)
    {
        return temp.get(i);
    }

    @Override
    public float getHeading()
    {
        //TODO: derive heading from Gaussian data
        return 0;
    }

    @Override
    public float getMaxGauss()
    {
        return magScale.getMinMax();
    }

    @Override
    public float getMinGauss()
    {
        return magScale.getMinMax();
    }

    @Override
    public float getMaxRotationalAcceleration()
    {
        return gyrScale.getMinMax();
    }

    @Override
    public float getMinRotationalAcceleration()
    {
        return gyrScale.getMinMax();
    }

    @Override
    public float getMaxAcceleration()
    {
        return accScale.getMinMax();
    }

    @Override
    public float getMinAcceleration()
    {
        return accScale.getMinMax();
    }
}
