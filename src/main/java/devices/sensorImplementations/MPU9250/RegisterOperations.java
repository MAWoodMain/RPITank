/**
 * 
 */
package devices.sensorImplementations.MPU9250;

import java.io.IOException;

import devices.I2C.I2CImplementation;

/**
 * @author GJWood
 *
 */
public class RegisterOperations {
	private I2CImplementation busDevice;
	
	public RegisterOperations(I2CImplementation mpu9250)
	{
		this.busDevice = mpu9250;
	}
	
    public String byteToString(byte b)
    {
    	String s = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    	return s;  	
    }
    public void printByteRegister(Registers r)
    {
    	byte rv = 0;
    	try {
			rv = busDevice.read(r.getAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
    	System.out.format("%20s : %8s 0X%X%n",r.name(),byteToString(rv),rv);
    }
    
   public void outputConfigRegisters()
    {
    	printByteRegister(Registers.CONFIG);
    	printByteRegister(Registers.GYRO_CONFIG);
    	printByteRegister(Registers.ACCEL_CONFIG);
    	printByteRegister(Registers.ACCEL_CONFIG2);
    	printByteRegister(Registers.LP_ACCEL_ODR);
    	printByteRegister(Registers.WOM_THR);
    	printByteRegister(Registers.MOT_DUR);
    	printByteRegister(Registers.ZMOT_THR);
    	printByteRegister(Registers.FIFO_EN);
    	printByteRegister(Registers.I2C_MST_CTRL);
    	printByteRegister(Registers.I2C_MST_STATUS);
    	printByteRegister(Registers.INT_PIN_CFG);
    	printByteRegister(Registers.INT_ENABLE);
    	printByteRegister(Registers.INT_STATUS);
    	printByteRegister(Registers.I2C_MST_DELAY_CTRL);
    	printByteRegister(Registers.SIGNAL_PATH_RESET);
    	printByteRegister(Registers.MOT_DETECT_CTRL);
    	printByteRegister(Registers.USER_CTRL);
    	printByteRegister(Registers.PWR_MGMT_1);
    	printByteRegister(Registers.PWR_MGMT_2);
    	printByteRegister(Registers.WHO_AM_I_MPU9250);
    	printByteRegister(Registers.SMPLRT_DIV);
    }
   /**
    * Reads the specified number of 16 bit Registers from a given device and address
    * @param address 	- the start address for the read
    * @param regCount 	- number of 16 bit registers to be read
    * @return 			- an array of shorts (16 bit signed values) holding the registers
    * Each registers is constructed from reading and combining 2 bytes, the first byte forms the more significant part of the register 
    */
   short[] read16BitRegisters(Registers r, int regCount)
   {
       byte rawData[] = null;
		try {
			rawData = busDevice.read(r.getAddress(), regCount*2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
       short[] registers = new short[regCount];
       for (int i=0;i<regCount;i++)		
       {
       	registers[i] = (short) (((short)rawData[i*2] << 8) | rawData[(i*2)+1]) ;  // Turn the MSB and LSB into a signed 16-bit value
       }
   	return registers;
   }

   byte readByteRegister(Registers r)
   {
	   try {
		return busDevice.read(r.getAddress());
	   } catch (IOException e) {
		   e.printStackTrace();
		   return (byte)0xFF;
	   }
	
   }
   void writeByteRegister(Registers r, byte rv)
   {
       try {
		busDevice.write(r.getAddress(),rv);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} // Set gyro sample rate to 1 kHz
       try {
		Thread.sleep(2); // delay to allow register to settle
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

   }
}
