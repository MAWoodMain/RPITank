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
		} catch (UnsupportedBusNumberException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args)throws IOException, I2CFactory.UnsupportedBusNumberException, InterruptedException {
		// TODO Auto-generated method stub
        
        String format = "%+04.3f";

	}
    @Override
    public void run()
    {
        while(!Thread.interrupted())
        {
            if(!paused) mpu9250.updateData();
            try
            {    
            	//Data3D mag = mpu9250.getLatestGaussianData();
                //Data3D acc = mpu9250.getLatestAcceleration();
                //Data3D gyr = mpu9250.getLatestRotationalAcceleration();
                //System.out.print(  "acc: " + String.format(format,acc.getX()) + ", " + String.format(format,acc.getY()) + ", " + String.format(format,acc.getZ()) + " ");
                //System.out.print(  "gyr: " + String.format(format,gyr.getX()) + ", " + String.format(format,gyr.getY()) + ", " + String.format(format,gyr.getZ()) + " ");
                //System.out.println("mag: " + String.format(format,mag.getX()) + ", " + String.format(format,mag.getY()) + ", " + String.format(format,mag.getZ()) + " ");
                //System.out.println(mpu9250.getHeading());

                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            
        }
    }
}