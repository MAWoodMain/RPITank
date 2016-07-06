import com.pi4j.io.gpio.*;
import driveAssembly.DriveAssembly;
import driveAssembly.TankDriveAssembly;
import motors.DCMotor;
import motors.Motor;

/**
 * RPITank
 * Created by MAWood on 06/07/2016.
 */
public class DriveAssemblyTest
{
    public static final Pin LEFT_MOTOR_A = RaspiPin.GPIO_00;
    public static final Pin LEFT_MOTOR_B = RaspiPin.GPIO_01;
    public static final Pin RIGHT_MOTOR_A = RaspiPin.GPIO_02;
    public static final Pin RIGHT_MOTOR_B = RaspiPin.GPIO_03;


    public static void main(String[] args) throws InterruptedException
    {
        GpioController gpio = GpioFactory.getInstance();
        Motor leftMotor = new DCMotor(gpio.provisionDigitalOutputPin(LEFT_MOTOR_A, PinState.LOW),
                gpio.provisionDigitalOutputPin(LEFT_MOTOR_B, PinState.LOW));
        Motor rightMotor = new DCMotor(gpio.provisionDigitalOutputPin(RIGHT_MOTOR_A, PinState.LOW),
                gpio.provisionDigitalOutputPin(RIGHT_MOTOR_B, PinState.LOW));

        DriveAssembly tankDriveAssembly = new TankDriveAssembly(leftMotor,rightMotor);

        while(true)
        {
            tankDriveAssembly.setDirection(0f);
            cycleSpeed(tankDriveAssembly,100);
            tankDriveAssembly.setDirection(180f);
            cycleSpeed(tankDriveAssembly,100);
        }
    }

    public static void cycleSpeed(DriveAssembly assembly, int delay) throws InterruptedException
    {

        for(int i = 10; i<100; i++)
        {
            assembly.setSpeed((float)i/100);
            Thread.sleep(delay);
        }
        for(int i = 100; i>10; i--)
        {
            assembly.setSpeed((float)i/100);
            Thread.sleep(delay);
        }
    }
}
