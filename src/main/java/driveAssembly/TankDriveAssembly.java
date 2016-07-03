package driveAssembly;

import motors.Motor;

/**
 * RPITank
 * Created by MAWood on 02/07/2016.
 */
public class TankDriveAssembly implements DriveAssembly
{
    private final Motor left;
    private final Motor right;

    private float angle;
    private float speed;

    public TankDriveAssembly(Motor left, Motor right)
    {
        this.left = left;
        this.right = right;
        this.angle = 0;
        this.speed = 0;
    }

    @Override
    public void setSpeed(float speed)
    {
        if(speed < 0) speed = 0;
        if(speed > 1) speed = 1;
        this.speed = speed;
        updateCourse();
    }

    @Override
    public float getSpeed()
    {
        return speed;
    }

    @Override
    public void setDirection(float angle)
    {
        if(angle<0) angle+=360;
        if(angle<0) angle = 0;
        if(angle>360) angle = 0;
        this.angle = angle;
        updateCourse();
    }

    @Override
    public float getDirection()
    {
        return angle;
    }

    @Override
    public void stop()
    {
        this.speed = 0;
    }

    private void updateCourse()
    {
        if(speed == 0)
        {
            left.stop();
            right.stop();
        } else
        {
            float leftCoefficient = 1;
            float rightCoefficient = 1;
            float adjustedDirection = this.getDirection();

            if(this.getDirection() > 90 && this.getDirection() < 270)
                adjustedDirection = (adjustedDirection * -1) + 540;
            if(adjustedDirection>=270) adjustedDirection -= 360;

            if(adjustedDirection > 0)
            {
                leftCoefficient = 1;
                rightCoefficient = 1 - ((Math.abs(adjustedDirection) / 90) * 2);
            } else
            {
                leftCoefficient = 1 - ((Math.abs(adjustedDirection) / 90) * 2);
                rightCoefficient = 1;
            }


            if(this.getDirection() > 90 && this.getDirection() < 270)
            {
                leftCoefficient *= -1;
                rightCoefficient *= -1;
            }

            left.setSpeed(leftCoefficient * this.getSpeed());
            System.out.println("Left : " + leftCoefficient * this.getSpeed());
            right.setSpeed(rightCoefficient * this.getSpeed());
            System.out.println("Right: " + rightCoefficient * this.getSpeed());
        }
    }
}
