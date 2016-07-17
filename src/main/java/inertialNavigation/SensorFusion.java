package inertialNavigation;

import devices.sensors.dataTypes.Data3D;
import devices.sensors.dataTypes.Quaternion;
import devices.sensors.dataTypes.TimestampedData3D;

public class SensorFusion {

	private static final float[] eInt = new float[]{0,0,0}; // vector to hold integral error for Mahony method
	private static final Quaternion q = new Quaternion(1,0,0,0);  // vector to hold quaternion
	

	// global constants for 9 DoF fusion and AHRS (Attitude and Heading Reference System)
	private static final float GYRO_MEASUREMENT_ERROR = (float)Math.PI * (40.0f / 180.0f);   // gyroscope measurement error in rads/s (start at 40 deg/s)
	private static final float GYRO_MEASUREMENT_DRIFT = (float)Math.PI  * (0.0f  / 180.0f);   // gyroscope measurement drift in rad/s/s (start at 0.0 deg/s/s)
	// There is a tradeoff in the BETA parameter between accuracy and response speed.
	// In the original Madgwick study, BETA of 0.041 (corresponding to GYRO_MEASUREMENT_ERROR of 2.7 degrees/s) was found to give optimal accuracy.
	// However, with this value, the LSM9SD0 response time is about 10 seconds to a stable initial quaternion.
	// Subsequent changes also require a longish lag time to a stable output, not fast enough for a quadcopter or robot car!
	// By increasing BETA (GYRO_MEASUREMENT_ERROR) by about a factor of fifteen, the response time constant is reduced to ~2 sec
	// I haven't noticed any reduction in solution accuracy. This is essentially the I coefficient in a PID control sense; 
	// the bigger the feedback coefficient, the faster the solution converges, usually at the expense of accuracy. 
	// In any case, this is the free parameter in the Madgwick filtering and fusion scheme.
	private static final float BETA = (float)Math.sqrt(3.0f / 4.0f) * GYRO_MEASUREMENT_ERROR;   // compute BETA
	private static final float ZETA = (float)Math.sqrt(3.0f / 4.0f) * GYRO_MEASUREMENT_DRIFT;   // compute ZETA, the other free parameter in the Madgwick scheme usually set to a small or zero value
	private static final float KP = 2.0f * 5.0f; // these are the free parameters in the Mahony filter and fusion scheme, KP for proportional feedback, KI for integral
	private static final float KI = 0.0f;

    // With these settings the filter is updating at a ~145 Hz rate using the Madgwick scheme and 
    // >200 Hz using the Mahony scheme even though the display refreshes at only 2 Hz.
    // The filter update rate is determined mostly by the mathematical steps in the respective algorithms, 
    // the processor speed (8 MHz for the 3.3V Pro Mini), and the magnetometer ODR:
    // an ODR of 10 Hz for the magnetometer produce the above rates, maximum magnetometer ODR of 100 Hz produces
    // filter update rates of 36 - 145 and ~38 Hz for the Madgwick and Mahony schemes, respectively. 
    // This is presumably because the magnetometer read takes longer than the gyro or accelerometer reads.
    // This filter update rate should be fast enough to maintain accurate platform orientation for 
    // stabilization control of a fast-moving robot or quadcopter. Compare to the update rate of 200 Hz
    // produced by the on-board Digital Motion Processor of Invensense's MPU6050 6 DoF and MPU9150 9DoF sensors.
    // The 3.3 V 8 MHz Pro Mini is doing pretty well!
	
	// Examples of calling the filters, READ BEFORE USING!!		!!!
	// sensors x (y)-axis of the accelerometer is aligned with the y (x)-axis of the magnetometer;
	// the magnetometer z-axis (+ down) is opposite to z-axis (+ up) of accelerometer and gyro!
	// We have to make some allowance for this orientation mismatch in feeding the output to the quaternion filter.
	// For the MPU-9250, we have chosen a magnetic rotation that keeps the sensor forward along the x-axis just like
	// in the LSM9DS0 sensor. This rotation can be modified to allow any convenient orientation convention.
	// This is ok by aircraft orientation standards!  
	// Pass gyro rate as rad/s
	//  MadgwickQuaternionUpdate(ax, ay, az, gx*PI/180.0f, gy*PI/180.0f, gz*PI/180.0f,  my,  mx, mz);
	//  MahonyQuaternionUpdate(ax, ay, az, gx*PI/180.0f, gy*PI/180.0f, gz*PI/180.0f, my, mx, mz);

	
	
	// Implementation of Sebastian Madgwick's
	// "...efficient orientation filter for... inertial/magnetic sensor arrays"
	// (see http://www.x-io.co.uk/category/open-source/ for examples and more
	// details)
	// which fuses acceleration, rotation rate, and magnetic moments to produce
	// a quaternion-based estimate of absolute
	// device orientation -- which can be converted to yaw, pitch, and roll.
	// Useful for stabilizing quadcopters, etc.
	// The performance of the orientation filter is at least as good as
	// conventional Kalman-based filtering algorithms
	// but is much less computationally intensive---it can be performed on a 3.3
	// V Pro Mini operating at 8 MHz!

	public float[] geteInt() {
		return eInt;
	}

	public Quaternion getQ() {
		return q;
	}


	public static float getGyroMeasurementError() {
		return GYRO_MEASUREMENT_ERROR;
	}

	public static float getGyroMeasurementDrift() {
		return GYRO_MEASUREMENT_DRIFT;
	}

	public static float getBeta() {
		return BETA;
	}

	public static float getZeta() {
		return ZETA;
	}

	public static float getKp() {
		return KP;
	}

	public static float getKi() {
		return KI;
	}

	
	public static void MadgwickQuaternionUpdate(TimestampedData3D acc, TimestampedData3D grav, TimestampedData3D mag, float deltat) //delta t in seconds

	{
		float q1 = q.a, q2 = q.b, q3 = q.c, q4 = q.d; // short name local
														// variable for
														// readability
		float norm;
		float hx, hy, _2bx, _2bz;
		float s1, s2, s3, s4;
		float qDot1, qDot2, qDot3, qDot4;

		// Auxiliary variables to avoid repeated arithmetic
		float _2q1mx;
		float _2q1my;
		float _2q1mz;
		float _2q2mx;
		float _4bx;
		float _4bz;
		float _2q1 = 2.0f * q1;
		float _2q2 = 2.0f * q2;
		float _2q3 = 2.0f * q3;
		float _2q4 = 2.0f * q4;
		float _2q1q3 = 2.0f * q1 * q3;
		float _2q3q4 = 2.0f * q3 * q4;
		float q1q1 = q1 * q1;
		float q1q2 = q1 * q2;
		float q1q3 = q1 * q3;
		float q1q4 = q1 * q4;
		float q2q2 = q2 * q2;
		float q2q3 = q2 * q3;
		float q2q4 = q2 * q4;
		float q3q3 = q3 * q3;
		float q3q4 = q3 * q4;
		float q4q4 = q4 * q4;

		
		acc.normalize(); // Normalise accelerometer measurement
		mag.normalize(); // Normalise magnetometer measurement


		// Reference direction of Earth's magnetic field
		_2q1mx = 2.0f * q1 * mag.getX();
		_2q1my = 2.0f * q1 *mag.getY();
		_2q1mz = 2.0f * q1 *mag.getZ();
		_2q2mx = 2.0f * q2 * mag.getX();
		hx = mag.getX() * q1q1 - _2q1my * q4 + _2q1mz * q3 + mag.getX() * q2q2 + _2q2 *mag.getY() * q3
				+ _2q2 *mag.getZ() * q4 - mag.getX() * q3q3 - mag.getX() * q4q4;
		hy = _2q1mx * q4 +mag.getY() * q1q1 - _2q1mz * q2 + _2q2mx * q3 -mag.getY() * q2q2
				+mag.getY() * q3q3 + _2q3 *mag.getZ() * q4 -mag.getY() * q4q4;
		_2bx = (float) Math.sqrt(hx * hx + hy * hy);
		_2bz = -_2q1mx * q3 + _2q1my * q2 +mag.getZ() * q1q1 + _2q2mx * q4 -mag.getZ() * q2q2
				+ _2q3 *mag.getY() * q4 -mag.getZ() * q3q3 +mag.getZ() * q4q4;
		_4bx = 2.0f * _2bx;
		_4bz = 2.0f * _2bz;

		// Gradient decent algorithm corrective step
		s1 = -_2q3 * (2.0f * q2q4 - _2q1q3 - acc.getX()) + _2q2
				* (2.0f * q1q2 + _2q3q4 - acc.getY()) - _2bz * q3
				* (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mag.getX())
				+ (-_2bx * q4 + _2bz * q2)
				* (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) -mag.getY()) + _2bx
				* q3
				* (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) -mag.getZ());
		s2 = _2q4 * (2.0f * q2q4 - _2q1q3 - acc.getX()) + _2q1
				* (2.0f * q1q2 + _2q3q4 - acc.getY()) - 4.0f * q2
				* (1.0f - 2.0f * q2q2 - 2.0f * q3q3 - acc.getZ()) + _2bz * q4
				* (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mag.getX())
				+ (_2bx * q3 + _2bz * q1)
				* (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) -mag.getY())
				+ (_2bx * q4 - _4bz * q2)
				* (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) -mag.getZ());
		s3 = -_2q1 * (2.0f * q2q4 - _2q1q3 - acc.getX()) + _2q4
				* (2.0f * q1q2 + _2q3q4 - acc.getY()) - 4.0f * q3
				* (1.0f - 2.0f * q2q2 - 2.0f * q3q3 - acc.getZ())
				+ (-_4bx * q3 - _2bz * q1)
				* (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mag.getX())
				+ (_2bx * q2 + _2bz * q4)
				* (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) -mag.getY())
				+ (_2bx * q1 - _4bz * q3)
				* (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) -mag.getZ());
		s4 = _2q2 * (2.0f * q2q4 - _2q1q3 - acc.getX()) + _2q3
				* (2.0f * q1q2 + _2q3q4 - acc.getY()) + (-_4bx * q4 + _2bz * q2)
				* (_2bx * (0.5f - q3q3 - q4q4) + _2bz * (q2q4 - q1q3) - mag.getX())
				+ (-_2bx * q1 + _2bz * q3)
				* (_2bx * (q2q3 - q1q4) + _2bz * (q1q2 + q3q4) -mag.getY()) + _2bx
				* q2
				* (_2bx * (q1q3 + q2q4) + _2bz * (0.5f - q2q2 - q3q3) -mag.getZ());
		
		norm = (float) Math.sqrt(s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4); // normalise step magnitude
		norm = 1.0f / norm;
		s1 *= norm;
		s2 *= norm;
		s3 *= norm;
		s4 *= norm;

		// Compute rate of change of quaternion
		qDot1 = 0.5f * (-q2 * grav.getX() - q3 * grav.getY() - q4 * grav.getZ()) - BETA * s1;
		qDot2 = 0.5f * (q1 * grav.getX() + q3 * grav.getZ() - q4 * grav.getY()) - BETA * s2;
		qDot3 = 0.5f * (q1 * grav.getY() - q2 * grav.getZ() + q4 * grav.getX()) - BETA * s3;
		qDot4 = 0.5f * (q1 * grav.getZ() + q2 * grav.getY() - q3 * grav.getX()) - BETA * s4;

		// Integrate to yield quaternion
		q1 += qDot1 * deltat;
		q2 += qDot2 * deltat;
		q3 += qDot3 * deltat;
		q4 += qDot4 * deltat;
		q.setAll(q1, q2, q3, q4);
		q.normalize();// Normalise quaternion
		Instruments.updateYawPitchRoll(q);

	}

	// Similar to Madgwick scheme but uses proportional and integral filtering
	// on the error between estimated reference vectors and
	// measured ones.
	public static void MahonyQuaternionUpdate(Data3D acc, Data3D grav, Data3D mag, float deltat) //delta t in seconds

	{
		float q1 = q.a, q2 = q.b, q3 = q.c, q4 = q.d; // short name local
														// variable for
														// readability
		float hx, hy, bx, bz;
		float vx, vy, vz, wx, wy, wz;
		float ex, ey, ez;
		float pa, pb, pc;

		// Auxiliary variables to avoid repeated arithmetic
		float q1q1 = q1 * q1;
		float q1q2 = q1 * q2;
		float q1q3 = q1 * q3;
		float q1q4 = q1 * q4;
		float q2q2 = q2 * q2;
		float q2q3 = q2 * q3;
		float q2q4 = q2 * q4;
		float q3q3 = q3 * q3;
		float q3q4 = q3 * q4;
		float q4q4 = q4 * q4;

		
		acc.normalize();// Normalise accelerometer measurement
		mag.normalize();// Normalise magnetometer measurement
		
		// Reference direction of Earth's magnetic field
		hx = 2.0f * mag.getX() * (0.5f - q3q3 - q4q4) + 2.0f *mag.getY() * (q2q3 - q1q4)
				+ 2.0f *mag.getZ() * (q2q4 + q1q3);
		hy = 2.0f * mag.getX() * (q2q3 + q1q4) + 2.0f *mag.getY() * (0.5f - q2q2 - q4q4)
				+ 2.0f *mag.getZ() * (q3q4 - q1q2);
		bx = (float) Math.sqrt((hx * hx) + (hy * hy));
		bz = 2.0f * mag.getX() * (q2q4 - q1q3) + 2.0f *mag.getY() * (q3q4 + q1q2) + 2.0f *mag.getZ()
				* (0.5f - q2q2 - q3q3);

		// Estimated direction of gravity and magnetic field
		vx = 2.0f * (q2q4 - q1q3);
		vy = 2.0f * (q1q2 + q3q4);
		vz = q1q1 - q2q2 - q3q3 + q4q4;
		wx = 2.0f * bx * (0.5f - q3q3 - q4q4) + 2.0f * bz * (q2q4 - q1q3);
		wy = 2.0f * bx * (q2q3 - q1q4) + 2.0f * bz * (q1q2 + q3q4);
		wz = 2.0f * bx * (q1q3 + q2q4) + 2.0f * bz * (0.5f - q2q2 - q3q3);

		// Error is cross product between estimated direction and measured
		// direction of gravity
		ex = (acc.getY() * vz - acc.getZ() * vy) + (mag.getY() * wz -mag.getZ() * wy);
		ey = (acc.getZ() * vx - acc.getX() * vz) + (mag.getZ() * wx -mag.getX() * wz);
		ez = (acc.getX() * vy - acc.getY() * vx) + (mag.getX() * wy -mag.getY() * wx);
		if (KI > 0.0f) {
			eInt[0] += ex; // accumulate integral error
			eInt[1] += ey;
			eInt[2] += ez;
		} else {
			eInt[0] = 0.0f; // prevent integral wind up
			eInt[1] = 0.0f;
			eInt[2] = 0.0f;
		}

		// Apply feedback terms
		grav.setX( grav.getX() + KP * ex + KI * eInt[0]);
		grav.setY( grav.getY() + KP * ey + KI * eInt[1]);
		grav.setZ( grav.getZ() + KP * ez + KI * eInt[2]);

		// Integrate rate of change of quaternion
		pa = q2;
		pb = q3;
		pc = q4;
		q1 = q1 + (-q2 * grav.getX() - q3 * grav.getY() - q4 * grav.getZ()) * (0.5f * deltat);
		q2 = pa + (q1 * grav.getX() + pb * grav.getZ() - pc * grav.getY()) * (0.5f * deltat);
		q3 = pb + (q1 * grav.getY() - pa * grav.getZ() + pc * grav.getX()) * (0.5f * deltat);
		q4 = pc + (q1 * grav.getZ() + pa * grav.getY() - pb * grav.getX()) * (0.5f * deltat);

		q.setAll(q1, q2, q3, q4);
		q.normalize();// Normalise quaternion
		Instruments.updateYawPitchRoll(q);
	}
}
