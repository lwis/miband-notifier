package com.lewisjuggins.miband;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.lewisjuggins.miband.bluetooth.BLEAction;
import com.lewisjuggins.miband.bluetooth.BLETask;
import com.lewisjuggins.miband.bluetooth.WaitAction;
import com.lewisjuggins.miband.bluetooth.WriteAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lewis on 29/12/14.
 */
public class MiBandCommunicationService extends Service
{
	private final String TAG = this.getClass().getSimpleName();

	private static final WriteAction startVibrate = new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ (byte) 8, (byte) 1 });

	private static final WriteAction stopVibrate = new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ (byte) 19 });

	private static final WriteAction reboot = new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ 12 });

	private BLECommunicationManager mBLEComms;

	private BroadcastReceiver mVibrateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, final Intent intent)
		{
			final long duration = intent.getLongExtra("duration", 100);

			vibrate(duration);
		}
	};

	private BroadcastReceiver mRebootReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			reboot();
		}
	};

	private BroadcastReceiver mColourReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			final int red = intent.getIntExtra("red", 6);
			final int green = intent.getIntExtra("green", 6);
			final int blue = intent.getIntExtra("blue", 6);

			setColor((byte) red, (byte) green, (byte) blue, true);
		}
	};

	private BroadcastReceiver mBandNotificationReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			final int vibrateTimes = intent.getIntExtra("vibrateTimes", 1);
			final long vibrateDuration = intent.getLongExtra("vibrateDuration", 250L);
			final int flashTimes = intent.getIntExtra("flashTimes", 1);
			final int flashColour = intent.getIntExtra("flashColour", 0xFFFFFFFF);
			final int originalColour = intent.getIntExtra("originalColour", 0xFFFFFFFF);
			final long flashDuration = intent.getLongExtra("flashDuration", 250L);

			notifyBand(vibrateDuration, vibrateTimes, flashTimes, flashColour, originalColour, flashDuration);
		}
	};

	private final BroadcastReceiver bluetoothStatusChangeReceiver = new BroadcastReceiver()
	{
		public void onReceive(Context context, Intent intent)
		{
			final String action = intent.getAction();

			if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
			{
				if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF)
				{
					Log.d(TAG, "Bluetooth switched off");
					mBLEComms.mBluetoothAdapterStatus = false;
					mBLEComms.setupComplete = false;
				}
				else if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON)
				{
					Log.d(TAG, "Bluetooth switched on, initialising");
					mBLEComms.mBluetoothAdapterStatus = true;
					mBLEComms.setupBluetooth();
				}
			}
		}
	};

	@Override public void onCreate()
	{
		super.onCreate();
		mBLEComms = new BLECommunicationManager(this);

		// Register to receive messages.
		LocalBroadcastManager.getInstance(this).registerReceiver(mVibrateReceiver, new IntentFilter("vibrate"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mRebootReceiver, new IntentFilter("reboot"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mColourReceiver, new IntentFilter("colour"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mBandNotificationReceiver, new IntentFilter("notifyBand"));

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(bluetoothStatusChangeReceiver, filter);
	}

	@Override public void onDestroy()
	{
		super.onDestroy();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mVibrateReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mRebootReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mColourReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBandNotificationReceiver);

		unregisterReceiver(bluetoothStatusChangeReceiver);

		mBLEComms.disconnectGatt();
	}

	@Override public IBinder onBind(final Intent intent)
	{
		return null;
	}

	private void vibrate(final long duration)
	{
		final List<BLEAction> list = new ArrayList<>();

		list.add(startVibrate);
		list.add(new WaitAction(duration));
		list.add(stopVibrate);

		final BLETask task = new BLETask(list);
		mBLEComms.queueTask(task);
	}

	private void reboot()
	{
		final List<BLEAction> list = new ArrayList<>();

		list.add(reboot);

		final BLETask task = new BLETask(list);
		mBLEComms.queueTask(task);
	}

	private void setColor(byte r, byte g, byte b, boolean display)
	{
		final List<BLEAction> list = new ArrayList<>();

		list.add(new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ 14, r, g, b, display ? (byte) 1 : (byte) 0 }));

		final BLETask task = new BLETask(list);
		mBLEComms.queueTask(task);
	}

	private byte[] convertRgb(int rgb)
	{
		final int red = ((rgb >> 16) & 0x0ff) / 42;
		final int green = ((rgb >> 8) & 0x0ff) / 42;
		final int blue = ((rgb) & 0x0ff) / 42;

		return new byte[]{ (byte) red, (byte) green, (byte) blue };
	}

	private synchronized void notifyBand(long vibrateDuration, int vibrateTimes, int flashTimes, int flashColour, int originalColour, long flashDuration)
	{
		final List<BLEAction> list = new ArrayList<>();

		final byte[] flashColours = convertRgb(flashColour);
		final byte[] originalColours = convertRgb(originalColour);

		for(int i = 1; i <= vibrateTimes; i++)
		{
			list.add(startVibrate);
			list.add(new WaitAction(vibrateDuration));
			list.add(stopVibrate);
		}
		for(int i = 1; i <= flashTimes; i++)
		{
			list.add(new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ 14, flashColours[0], flashColours[1], flashColours[2], (byte) 1}));
			list.add(new WaitAction(flashDuration));
			list.add(new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ 14, originalColours[0], originalColours[1], originalColours[2], (byte) 0}));
			list.add(new WaitAction(500L));
		}

		final BLETask task = new BLETask(list);
		mBLEComms.queueTask(task);
	}
}
