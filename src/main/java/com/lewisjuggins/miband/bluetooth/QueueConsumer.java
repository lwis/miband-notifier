package com.lewisjuggins.miband.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import com.lewisjuggins.miband.BLECommunicationManager;
import com.lewisjuggins.miband.MiBandConnectFailureException;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
* Created by Lewis on 10/01/15.
*/
public class QueueConsumer implements Runnable
{
	private String TAG = this.getClass().getSimpleName();

	private BLECommunicationManager bleCommunicationManager;
	private final LinkedBlockingQueue<BLETask> queue;

	public QueueConsumer(final BLECommunicationManager bleCommunicationManager)
	{
		this.bleCommunicationManager = bleCommunicationManager;
		this.queue = new LinkedBlockingQueue<>();
	}

	public void add(final BLETask task)
	{
		queue.add(task);
	}

	@Override public void run()
	{
		while(true)
		{
			try
			{
				final BLETask task = queue.take();

				final List<BLEAction> actions = task.getActions();

				for(BLEAction action : actions)
				{
					if(action instanceof WaitAction)
					{
						action.run();
					}
					else if(action instanceof WriteAction)
					{
						try
						{
							final BluetoothGattCharacteristic characteristic = bleCommunicationManager.getCharacteristic(((WriteAction) action).getCharacteristic());
							characteristic.setValue(((WriteAction) action).getPayload());
							bleCommunicationManager.write(characteristic);
						}
						catch(MiBandConnectFailureException e)
						{
							Log.i(TAG, "Write failed");
						}
					}
				}
			}
			catch(Exception e)
			{
				Log.w(TAG, e.toString());
			}
			finally
			{
				if(queue.isEmpty())
				{
					bleCommunicationManager.setHighLatency();
				}
			}
		}
	}
}
