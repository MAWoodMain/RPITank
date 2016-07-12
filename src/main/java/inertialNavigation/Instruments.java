package inertialNavigation;

import sensors.dataTypes.Data3D;
import sensors.dataTypes.Quaternion;

public class Instruments {
	
	private static float yaw = 0;
	private static float pitch = 0;
	private static float roll = 0;
	
	private static float speed = 0;
	private static Data3D heading = new Data3D(0,0,0);
	private static Data3D position = new Data3D(0,0,0);
	
	public static float getYaw() {
		return yaw;
	}

	public static float getPitch() {
		return pitch;
	}

	public static float getRoll() {
		return roll;
	}
	public static float getSpeed() {
		return speed;
	}

	public static Data3D getHeading() {
		return heading;
	}

	public static Data3D getPosition() {
		return position;
	}

	public static void updateYawPitchRoll(Quaternion q)
	{
		  // Define output variables from updated quaternion---these are Tait-Bryan angles, commonly used in aircraft orientation.
		  // In this coordinate system, the positive z-axis is down toward Earth. 
		  // Yaw is the angle between Sensor x-axis and Earth magnetic North (or true North if corrected for local declination, looking down on the sensor positive yaw is counterclockwise.
		  // Pitch is angle between sensor x-axis and Earth ground plane, toward the Earth is positive, up toward the sky is negative.
		  // Roll is angle between sensor y-axis and Earth ground plane, y-axis up is positive roll.
		  // These arise from the definition of the homogeneous rotation matrix constructed from quaternions.
		  // Tait-Bryan angles as well as Euler angles are non-commutative; that is, the get the correct orientation the rotations must be
		  // applied in the correct order which for this configuration is yaw, pitch, and then roll.
		  // For more see http://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles which has additional links.
		    yaw   = (float)Math.atan2(2.0f * (q.b * q.c + q.a * q.d), q.a * q.a + q.b * q.b - q.c * q.c - q.d * q.d);   
		    pitch = -(float)Math.asin(2.0f * (q.b * q.d - q.a * q.c));
		    roll  = (float)Math.atan2(2.0f * (q.a * q.b + q.c * q.d), q.a * q.a - q.b * q.b - q.c * q.c + q.d * q.d);
		    pitch *= 180.0f / (float)Math.PI;
		    yaw   *= 180.0f / (float)Math.PI; 
		    //yaw   -= 13.8; // Declination at Danville, California is 13 degrees 48 minutes and 47 seconds on 2014-04-04
		    yaw   -= -44f/60f; // Declination at Letchworth England is minus O degrees and 44 Seconds on 2016-07-11
		    roll  *= 180.0f / (float)Math.PI;
	}

}