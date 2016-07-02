

import com.pi4j.io.gpio.*;
import driveAssembly.DriveAssembly;
import driveAssembly.TankDriveAssembly;
import motors.*;

/**
 * RPITank
 * Created by MAWood on 01/07/2016.
 */
public class Main
{
    public static void main(String[] args) throws InterruptedException
    {
        System.out.println("Starting");

        final GpioController gpio = GpioFactory.getInstance();

        final Motor left = new DCMotor(
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "LeftMotorA", PinState.LOW),
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "LeftMotorB", PinState.LOW));
        final Motor right = new DCMotor(
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "RightMotorA", PinState.LOW),
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "RightMotorB", PinState.LOW));

        DriveAssembly driveAssembly = new TankDriveAssembly(left,right);

        driveAssembly.setDirection(0f);
        driveAssembly.setSpeed(0f);

        for(int i=10; i<=100; i++)
        {
            driveAssembly.setSpeed(((float)i)/100);
            Thread.sleep(200);
        }
        for(int i=100; i>=10; i--)
        {
            driveAssembly.setSpeed(((float)i)/100);
            Thread.sleep(200);
        }

        driveAssembly.setDirection(180f);

        for(int i=10; i<=100; i++)
        {
            driveAssembly.setSpeed(((float)i)/100);
            Thread.sleep(200);
        }
        for(int i=100; i>=10; i--)
        {
            driveAssembly.setSpeed(((float)i)/100);
            Thread.sleep(200);
        }
    }
}
