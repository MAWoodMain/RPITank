package inertialNavigation;

import java.io.IOException;

import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import sensors.MPU9250.MPU9250;

public class Navigate implements Runnable{
	
	private Boolean paused;
	private MPU9250 mpu9250;
	
	public Navigate()
	{
		paused  = false;
		try {
			mpu9250  = new MPU9250(104);
		} catch (UnsupportedBusNumberException | IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args)throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException {
		// TODO Auto-generated method stub
        

	}
    @Override
    public void run()
    {
        String format = "%+04.3f";
        while(!Thread.interrupted())
        {
            if(!paused) mpu9250.updateData();
            try
            {    
            	Instruments.setMagnetometer( mpu9250.getLatestGaussianData());
                Instruments.setAccelerometer(mpu9250.getLatestAcceleration());
                Instruments.setGyroscope(mpu9250.getLatestRotationalAcceleration());
                System.out.print(  "acc: " + String.format(format,Instruments.getAccelerometer().getX()) + ", " +
                		String.format(format,Instruments.getAccelerometer().getY()) + ", " + 
                		String.format(format,Instruments.getAccelerometer().getZ()) + " ");
                System.out.print(  "gyr: " + String.format(format,Instruments.getGyroscope().getX()) + ", " +
                		String.format(format,Instruments.getGyroscope().getY()) + ", " +
                		String.format(format,Instruments.getGyroscope().getZ()) + " ");
                System.out.println("mag: " + String.format(format,Instruments.getMagnetometer().getX()) + ", " + 
                		String.format(format,Instruments.getMagnetometer().getY()) + ", " + 
                		String.format(format,Instruments.getMagnetometer().getZ()) + " ");
                System.out.println(mpu9250.getHeading());

                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            
        }
    }
}