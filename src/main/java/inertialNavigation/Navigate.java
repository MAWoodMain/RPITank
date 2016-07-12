package inertialNavigation;

import java.io.IOException;

import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import sensors.MPU9250.MPU9250;
import sensors.interfaces.Accelerometer;
import sensors.interfaces.Gyroscope;
import sensors.interfaces.Magnetometer;
import sensors.interfaces.SensorUpdateListener;

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
			MPU9250 mpu9250  = new MPU9250(SAMPLE_RATE, SAMPLE_SIZE); //sample at 100 Hertz
			mpu9250.registerInterest(this);
			new Thread(mpu9250).start();
			acc = mpu9250;
			mag = mpu9250;
			gyr = mpu9250;
			
		} catch (UnsupportedBusNumberException | IOException | InterruptedException e) {
			// TODO Auto-generated catch block
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
        while(!Thread.interrupted())
        {
            if(dataReady) 
            try
            {    
        		dataReady = false;
            	Instruments.setMagnetometer( mag.getLatestGaussianData());
                Instruments.setAccelerometer(acc.getLatestAcceleration());
                Instruments.setGyroscope(gyr.getLatestRotationalAcceleration());
                Instruments.setHeading(mag.getHeading());
                SensorFusion.MadgwickQuaternionUpdate(Instruments.getAccelerometer(),Instruments.getGyroscope(),Instruments.getMagnetometer(),(float)(DELTA_T/1000000000));
                System.out.print(  "acc: " + Instruments.getAccelerometer().toString());
                System.out.print(  "gyr: " + Instruments.getGyroscope().toString());
                System.out.println("mag: " + Instruments.getMagnetometer().toString());
                System.out.println("Yaw,Pirch & Roll: " + Instruments.getAngles().toString());
                System.out.println(mag.getHeading());

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