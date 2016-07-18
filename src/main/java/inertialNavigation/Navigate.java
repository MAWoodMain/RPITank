package inertialNavigation;

import java.io.IOException;

import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import devices.sensorImplimentations.MPU9250.MPU9250_Pi4j;
import devices.sensors.interfaces.Accelerometer;
import devices.sensors.interfaces.Gyroscope;
import devices.sensors.interfaces.Magnetometer;
import devices.sensors.interfaces.SensorUpdateListener;
import devices.sensors.dataTypes.*;


public class Navigate implements Runnable, SensorUpdateListener{
	
	private static final int SAMPLE_RATE = 100; //sample at 100 Hertz
	private static final int SAMPLE_SIZE = 100; //sample at 100 Hertz
	private static final long DELTA_T = 1000000000L/SAMPLE_RATE; // average time difference in between readings in nano seconds
	private Boolean dataReady;
	
	private Accelerometer acc;
	private Magnetometer mag;
	private Gyroscope gyr;
	
	public static int getSampleRate() {
		return SAMPLE_RATE;
	}
	public static long getDeltaT() {
		return DELTA_T;
	}
	public Navigate()
	{
		dataReady  = false;
		try {
			MPU9250_Pi4j mpu9250  = new MPU9250_Pi4j(SAMPLE_RATE, SAMPLE_SIZE); //sample at 100 Hertz
			mpu9250.registerInterest(this);
			new Thread(mpu9250).start();
			acc = mpu9250;
			mag = mpu9250;
			gyr = mpu9250;
			
		} catch (UnsupportedBusNumberException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
    }
	public static void main(String[] args)throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException
	{
        new Navigate();
	}
    @Override
    public void run()
    {
    	TimestampedData3D ajustedGyr, ajustedMag;
    	while(!Thread.interrupted())
        {
            if(dataReady) 
            try
            {    
        		dataReady = false;
            	Instruments.setMagnetometer( mag.getLatestGaussianData());
                Instruments.setAccelerometer(acc.getLatestAcceleration());
                Instruments.setGyroscope(gyr.getLatestRotationalAcceleration());
                
            	// Examples of calling the filters, READ BEFORE USING!!		!!!
            	// sensors x (y)-axis of the accelerometer is aligned with the y (x)-axis of the magnetometer;
            	// the magnetometer z-axis (+ down) is opposite to z-axis (+ up) of accelerometer and gyro!
            	// We have to make some allowance for this orientation mismatch in feeding the output to the quaternion filter.
            	// For the MPU-9250, we have chosen a magnetic rotation that keeps the sensor forward along the x-axis just like
            	// in the LSM9DS0 sensor. This rotation can be modified to allow any convenient orientation convention.
            	// This is ok by aircraft orientation standards!  
            	// Pass gyro rate as rad/s
            	//  MadgwickQuaternionUpdate(ax, ay, az, gx*PI/180.0f, gy*PI/180.0f, gz*PI/180.0f,  my,  mx, mz);

                ajustedGyr = new TimestampedData3D(Instruments.getGyroscope());
                ajustedGyr.setX(Instruments.getGyroscope().getX()*(float)Math.PI/180.0f); //Pass gyro rate as rad/s
                ajustedGyr.setY(Instruments.getGyroscope().getY()*(float)Math.PI/180.0f);
                ajustedGyr.setZ(Instruments.getGyroscope().getZ()*(float)Math.PI/180.0f);
                ajustedMag = new TimestampedData3D(Instruments.getMagnetometer());
                ajustedMag.setX(Instruments.getMagnetometer().getY()); //swap X and Y, Z stays the same
                ajustedMag.setY(Instruments.getMagnetometer().getX());

                SensorFusion.MadgwickQuaternionUpdate(Instruments.getAccelerometer(),ajustedGyr,ajustedMag,(float)(DELTA_T/TimestampedData3D.NANOS_PER_SEC));
                System.out.print(  "acc: " + Instruments.getAccelerometer().toString());
                System.out.print(  "gyr: " + Instruments.getGyroscope().toString());
                System.out.println("mag: " + Instruments.getMagnetometer().toString());
                System.out.println("Yaw,Pirch & Roll: " + Instruments.getAngles().toString());

                Thread.sleep(1);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            
        }
    }
	@Override
	public void dataUpdated() {
		dataReady = true;
		
	}
}