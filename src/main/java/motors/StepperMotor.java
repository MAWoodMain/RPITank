package motors;

import com.pi4j.io.gpio.PinState;

/**
 * RPITank
 * Created by MAWood on 02/07/2016.
 */
public class StepperMotor implements Motor
{
    private byte[] stepSequence;
    private PinState[] states;

    public StepperMotor()
    {
        states = new PinState[]{PinState.LOW, PinState.HIGH};
        stepSequence = new byte[4];
        stepSequence[0] = (byte) 0b0011;
        stepSequence[1] = (byte) 0b0110;
        stepSequence[2] = (byte) 0b1100;
        stepSequence[3] = (byte) 0b1001;
    }

    @Override
    public void setSpeed(float speed)
    {

    }

    @Override
    public float getSpeed()
    {
        return 0;
    }

    @Override
    public void stop()
    {

    }
}
