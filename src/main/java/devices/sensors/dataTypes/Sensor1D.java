package devices.sensors.dataTypes;

import devices.sensors.interfaces.SensorUpdateListener;

import java.io.IOException;
import java.util.ArrayList;

/**
 * RPITank - devices.sensors
 * Created by MAWood on 18/07/2016.
 */
public abstract class Sensor1D implements Runnable
{
    protected final CircularArrayRing<TimestampedData1D> rawXVals;
    float xBias;
    float xScaling;
    float unitCorrectionScale;
    float unitCorrectionOffset;


    private final int sampleRate;

    private boolean paused;

    private ArrayList<SensorUpdateListener> listeners;

    public Sensor1D(int sampleRate, int sampleSize)
    {
        this.sampleRate = sampleRate;
        listeners = new ArrayList<>();
        paused = false;

        rawXVals = new CircularArrayRing<>(sampleSize);
        xBias = 0;
        xScaling = 1;
        unitCorrectionOffset = 0;
        unitCorrectionScale = 1;
    }

    public TimestampedData1D getLatestX()
    {
        return this.getX(0);
    }

    public TimestampedData1D getX(int i)
    {
        TimestampedData1D actual = rawXVals.get(i).clone();
        actual.scale(xScaling);
        actual.offset(xBias);
        actual.scale(unitCorrectionScale);
        actual.offset(unitCorrectionOffset);
        return actual;
    }
    
    public TimestampedData1D getAvgX()
    {
    	int n = rawXVals.size();
    	double sum = 0;
    	TimestampedData1D value;
    	for (int i=0; i<n; i++  )
    	{
    		value = rawXVals.get(i).clone();
    		value.scale(xScaling);
    		value.offset(xBias);
    		value.scale(unitCorrectionScale);
    		value.offset(unitCorrectionOffset);
    		sum += value.getX();
    	}
    	TimestampedData1D actual = rawXVals.get(n-1).clone();
    	actual.setX((float)(sum/n));
        return actual;
    }

    @Override
    public void run()
    {
        long lastTime;
        final long waitTime = 1000000000L /sampleRate;
        while(!Thread.interrupted())
        {
            if(!paused)
            {
                try
                {
                    lastTime = System.nanoTime();
                    updateData();
                    Thread.sleep(100);


                    for(SensorUpdateListener listener:listeners) listener.dataUpdated();

                    while(System.nanoTime() - lastTime < waitTime);
                } catch (Exception ignored)
                {
                }
            }
        }
    }

    protected abstract void updateData() throws IOException;

    protected void addValue(TimestampedData1D value)
    {
        rawXVals.add(value);
    }

    protected void setxBias(float xBias)
    {
        this.xBias = xBias;
    }

    protected void setxScaling(float xScaling)
    {
        this.xScaling = xScaling;
    }

    protected void setUnitCorrectionScale(float unitCorrectionScale)
    {
        this.unitCorrectionScale = unitCorrectionScale;
    }

    protected void setUnitCorrectionOffset(float unitCorrectionOffset)
    {
        this.unitCorrectionOffset = unitCorrectionOffset;
    }

    public float getxBias()
    {
        return xBias;
    }

    public float getxScaling()
    {
        return xScaling;
    }

    public void pause()
    {
        paused = true;
    }

    public void unpause()
    {
        paused = false;
    }

    public void registerInterest(SensorUpdateListener listener)
    {
        listeners.add(listener);
    }
}
