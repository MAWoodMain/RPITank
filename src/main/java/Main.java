

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

        final GpioPinDigitalOutput motor1A = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Motor1A", PinState.LOW);
        final GpioPinDigitalOutput motor1B = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Motor1B", PinState.LOW);

        final GpioPinDigitalOutput motor2A = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Motor2A", PinState.LOW);
        final GpioPinDigitalOutput motor2B = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Motor2B", PinState.LOW);

        StepperMotor s1 = new StepperMotor(new GpioPinDigitalOutput[]{motor1A, motor1B, motor2A, motor2B}, 2048);
        s1.rotate(360);
        s1.rotate(-360);
    }
}
