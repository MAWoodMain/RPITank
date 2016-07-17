package devices.motors;

/**
 * RPITank
 * Created by MAWood on 02/07/2016.
 */
public interface Motor
{
    void setSpeed(float speed);
    float getSpeed();

    void stop();
}
