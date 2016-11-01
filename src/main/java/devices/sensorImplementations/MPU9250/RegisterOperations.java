/**
 * 
 */
package devices.sensorImplementations.MPU9250;

import java.io.IOException;

import devices.I2C.I2CImplementation;

/**
 * Register Operations
 * 
 * @author GJWood
 * @version 1.0
 * 
 * The module hides all details about manipulating the registers of a device and
 * provides a simple interface to access them, and display them for debugging.
 */
public class RegisterOperations {
	private I2CImplementation busDevice;
	
	public RegisterOperations(I2CImplementation device)
	{
		this.busDevice = device; // the device on the IC2 bus that the registers belong to
	}
	
	/**
	 * produces a binary representation of a byte
	 * @param b		- the byte
	 * @return		- A string containing the binary representation
	 */
    public String byteToString(byte b)
    {
    	String s = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    	return s;  	
    }
    
    /**
     * Prints the name and contents of the register in binary and Hex
     * @param r		- the register to be printed
     */
    public void printByteRegister(Registers r)
    {
    	byte rv = 0;
    	try {
			rv = busDevice.read(r.getAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
    	System.out.format("%20s : %8s 0x%X%n",r.name(),byteToString(rv),rv);
    }
   
   /**
    * Reads the specified byte register from the device this class is associated with
    * @param r		- the register to be read
    * @return		- the value of the register
    */
   byte readByteRegister(Registers r)
   {
	   try {
		return busDevice.read(r.getAddress());
	   } catch (IOException e) {
		   e.printStackTrace();
		   return (byte)0xFF;
	   }
   }
   
   /**
    * Reads the specified number of byte Registers from the device this class is associated with
    * @param r 			- the first register to be read
    * @param byteCount 	- number of bytes to be read
    * @return			- an array of the bytes read from the registers
    */
   byte[] readByteRegisters(Registers r, int byteCount)
   {
	   try {
		return busDevice.read(r.getAddress(),byteCount);
	   } catch (IOException e) {
		   e.printStackTrace();
		   return null;
	   }
   }

   /**
    * Reads the specified number of 16 bit Registers from the device this class is associated with
    * @param register 	- the register to be read (name of first byte)
    * @param regCount 	- number of 16 bit registers to be read
    * @return 			- an array of shorts (16 bit signed values) holding the registers
    * Each registers is constructed from reading and combining 2 bytes, the first byte forms the more significant part of the register 
    */
   short[] read16BitRegisters(Registers r, int regCount)
   {
       byte[] rawData = readByteRegisters(r, regCount*2);
       short[] registers = new short[regCount];
       for (int i=0;i<regCount;i++)		
       {
       	registers[i] = (short) (((short)rawData[i*2] << 8) | rawData[(i*2)+1]) ;  // Turn the MSB and LSB into a signed 16-bit value
       }
       return registers;
   }
   
   /**
    * Writes a byte to the specified byte register from the device this class is associated with
    * @param r		- the register to be read
    * @param rv		- the value to be written to the register
    */
   void writeByteRegister(Registers r, byte rv)
   {
	   System.out.print("Before - ");
	   printByteRegister(r);
       try {
		busDevice.write(r.getAddress(),rv);
       } catch (IOException e) {
		e.printStackTrace();
       }
       try {
		Thread.sleep(2); // delay to allow register to settle
       } catch (InterruptedException e) {
		e.printStackTrace();
       }
	   System.out.print("After  - ");
	   printByteRegister(r);
   }
   
  /**
   * Prints the contents of pre-selected registers 
   */
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
}