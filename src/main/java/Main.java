

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

        final GpioPinDigitalOutput motor1A = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Motor1A", PinState.LOW);
        motor1A.setShutdownOptions(true, PinState.LOW);
        final GpioPinDigitalOutput motor1B = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Motor1B", PinState.LOW);
        motor1B.setShutdownOptions(true, PinState.LOW);

        final GpioPinDigitalOutput motor2A = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Motor2A", PinState.LOW);
        motor2A.setShutdownOptions(true, PinState.LOW);
        final GpioPinDigitalOutput motor2B = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Motor2B", PinState.LOW);
        motor2B.setShutdownOptions(true, PinState.LOW);

        final Motor left = new DCMotor(motor1A, motor1B);
        final Motor right = new DCMotor(motor2A, motor2B);

        DriveAssembly driveAssembly = new TankDriveAssembly(left,right);

        driveAssembly.setDirection(-90f);

        for(int i=0; i<101; i++)
        {
            driveAssembly.setSpeed(((float)i)/100);
            Thread.sleep(200);
        }

    }
}
