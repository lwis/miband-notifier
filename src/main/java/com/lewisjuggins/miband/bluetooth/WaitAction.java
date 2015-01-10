package com.lewisjuggins.miband.bluetooth;

/**
 * Created by Lewis on 10/01/15.
 */
public class WaitAction implements BLEAction
{
	private final long duration;

	public WaitAction(final long duration)
	{
		this.duration = duration;
	}

	public void run()
	{
		threadWait(duration);
	}

	private void threadWait(final long duration)
	{
		try
		{
			Thread.sleep(duration);
		}
		catch(InterruptedException e)
		{
			threadWait(duration);
		}
	}
}
