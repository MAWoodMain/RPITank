package devices.driveAssembly;

/**
 * RPITank
 * Created by MAWood on 02/07/2016.
 */
public interface DriveAssembly
{
    void setSpeed(float speed);
    float getSpeed();
    void setDirection(float angle);
    float getDirection();

    void stop();

}
