package motors;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.wiringpi.SoftPwm;

/**
 * RPITank
 * Created by MAWood on 02/07/2016.
 */
public class DCMotor implements Motor
{
    private float speed;

    private GpioPinDigitalOutput pinA;
    private GpioPinDigitalOutput pinB;


    public DCMotor(GpioPinDigitalOutput pinA, GpioPinDigitalOutput pinB)
    {
        this.pinA = pinA;
        this.pinB = pinB;
        this.pinA.setShutdownOptions(true, PinState.LOW);
        this.pinB.setShutdownOptions(true, PinState.LOW);
        SoftPwm.softPwmCreate(this.pinA.getPin().getAddress(), 0, 100);
        SoftPwm.softPwmCreate(this.pinB.getPin().getAddress(), 0, 100);
    }

    @Override
    public void setSpeed(float speed)
    {
        if(speed > 1) speed = 1;
        if(speed < -1) speed = -1;
        this.speed = speed;

        if(speed > 0)
        {
            SoftPwm.softPwmWrite(this.pinA.getPin().getAddress(), Math.round(speed * 100));
            SoftPwm.softPwmWrite(this.pinB.getPin().getAddress(), 0);
        } else if(speed < 0)
        {
            SoftPwm.softPwmWrite(this.pinA.getPin().getAddress(), 0);
            SoftPwm.softPwmWrite(this.pinB.getPin().getAddress(), Math.round(Math.abs(speed) * 100));
        } else
        {
            SoftPwm.softPwmWrite(this.pinA.getPin().getAddress(), 0);
            SoftPwm.softPwmWrite(this.pinB.getPin().getAddress(), 0);
        }
    }

    @Override
    public float getSpeed()
    {
        return speed;
    }

    @Override
    public void stop()
    {
        this.setSpeed(0);
    }
}
