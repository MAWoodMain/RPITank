

import com.pi4j.io.gpio.*;
import com.pi4j.io.serial.*;
import motors.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

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

        final GpioPinDigitalOutput motor1A = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Motor1A", PinState.LOW);
        final GpioPinDigitalOutput motor1B = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Motor1B", PinState.LOW);

        final GpioPinDigitalOutput motor2A = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Motor2A", PinState.LOW);
        final GpioPinDigitalOutput motor2B = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Motor2B", PinState.LOW);

        Motor left = new DCMotor(motor1A,motor1B);
        Motor right = new DCMotor(motor2A,motor2B);

        DriveAssembly mainDriveAssembly = new TankDriveAssembly(left,right);
        mainDriveAssembly.setDirection(90f);
        mainDriveAssembly.setSpeed(.5f);
    }

    public static int getBit(byte b, int position)
    {
        return (b >> 3-position) & 1;
    }
}
