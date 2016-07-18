package devices.motors;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;

/**
 * RPITank
 * Created by MAWood on 02/07/2016.
 */
public class StepperMotor
{
    private enum Direction
    {
        forward,
        backwards
    }

    private final byte[] stepSequence;
    private final PinState[] states;
    private final GpioPinDigitalOutput[] pins;
    private final int STEPS_PER_ROTATION;
    private final int STEP_DELAY_NANOS = 700000;
    private int phase;

    public StepperMotor(GpioPinDigitalOutput[] pins, int steps_per_rotation)
    {
        this.pins = pins;
        this.phase = 0;
        this.STEPS_PER_ROTATION = steps_per_rotation;
        this.states = new PinState[]{PinState.LOW, PinState.HIGH};
        this.stepSequence = new byte[4];
        this.stepSequence[0] = (byte) 0b0011;
        this.stepSequence[1] = (byte) 0b0110;
        this.stepSequence[2] = (byte) 0b1100;
        this.stepSequence[3] = (byte) 0b1001;
    }

    private void step(Direction direction)
    {
        if(direction == Direction.forward)
        {
            phase = (phase + 1) % stepSequence.length;
        } else
        {
            phase = (phase + 3) % stepSequence.length;
        }

        for(int i = 0; i<4; i++)
        {
            System.out.println(phase);
            pins[i].setState(states[(stepSequence[phase] >> 3-i) & 1]);
            long start = System.nanoTime();
            while(System.nanoTime() - start < STEP_DELAY_NANOS);
        }

    }

    public void rotate(float degrees) throws InterruptedException
    {
        Direction dir = degrees > 0 ? Direction.forward : Direction.backwards;
        int steps = Math.round((Math.abs(degrees)/360) * STEPS_PER_ROTATION);
        System.out.println("Doing " + steps + " steps");
        while(steps> 0)
        {
            step(dir);
            steps--;
        }
    }
}
