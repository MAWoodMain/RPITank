package inertialNavigation;

import sensors.dataTypes.TimestampedData3D;

public class TrapezoidalRule {

	  /**********************************************************************
	   * Standard normal distribution density function.
	   * Replace with any sufficiently smooth function.
	   **********************************************************************/
	   static float f(float x) {
	      return (float)Math.exp(- x * x / 2) / (float)Math.sqrt(2 * Math.PI);
	   }

	  /**********************************************************************
	   * Integrate f from a to b using the trapezoidal rule.
	   * Increase deltaT for more precision.
	   **********************************************************************/
/*	   static float integrate(float a, float b, int n) {
	      float h = (b - a) / deltaT;          // step size
	      float sum = 0.5f * (f(a) + f(b));    // area
	      for (int i = 1; i < n; i++) {
	         float x = a + h * i;
	         sum = sum + f(x);
	      }

	      return sum * h;
	   }*/
	   static TimestampedData3D integrate(TimestampedData3D sample1, TimestampedData3D sample2 ){
		
		   return sample1;
	   }
	}
