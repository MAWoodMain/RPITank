package sensors;

import sensors.interfaces.SensorUpdateListener;

import java.util.ArrayList;

/**
 * Created by MAWood on 17/07/2016.
 */
public abstract class Sensor implements Runnable
{

    private final int sampleRate;

    private boolean paused;

    private ArrayList<SensorUpdateListener> listeners;

    Sensor(int sampleRate)
    {
        this.sampleRate = sampleRate;

        paused = false;

        listeners = new ArrayList<>();
    }

    public void pause()
    {
        paused = true;
    }

    public void unpause()
    {
        paused = false;
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
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    abstract void updateData();

    public void registerInterest(SensorUpdateListener listener)
    {
        listeners.add(listener);
    }
}
