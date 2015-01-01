package com.lewisjuggins.miband;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.graphics.Palette;
import android.util.Log;
import com.lewisjuggins.miband.preferences.Application;
import com.lewisjuggins.miband.preferences.UserPreferences;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Lewis on 28/12/14.
 */
public class NotificationService extends NotificationListenerService
{
	private String TAG = this.getClass().getSimpleName();

	private BLECommunicationManager mBLEComms;

	private PowerManager pm;

	private BroadcastReceiver mVibrateReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Long duration = intent.getLongExtra("duration", 100);
			try
			{
				mBLEComms.connect();
				vibrate(duration);
				mBLEComms.disconnectGatt();
			}
			catch(MiBandConnectFailureException e)
			{
				//ignore.
			}
		}
	};

	private BroadcastReceiver mRebootReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				mBLEComms.connect();
				reboot();
				mBLEComms.disconnectGatt();

			}
			catch(MiBandConnectFailureException e)
			{
				//ignore.
			}
		}
	};

	private BroadcastReceiver mColourReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			int red = intent.getIntExtra("red", 6);
			int green = intent.getIntExtra("green", 6);
			int blue = intent.getIntExtra("blue", 6);

			try
			{
				mBLEComms.connect();
				setColor((byte) red, (byte) green, (byte) blue, true);
				mBLEComms.disconnectGatt();
			}
			catch(MiBandConnectFailureException e)
			{
				//ignore.
			}
		}
	};

	private final BroadcastReceiver bluetoothStatusChangeReceiver = new BroadcastReceiver()
	{
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
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

	@Override
	public IBinder onBind(Intent intent)
	{
		Log.i(TAG, "Binding to intent.");
		return super.onBind(intent);
	}

	@Override
	public boolean bindService(Intent service, ServiceConnection conn, int flags)
	{
		return super.bindService(service, conn, flags);
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.d(TAG, "Starting service.");

		try
		{
			UserPreferences.loadPreferences(openFileInput(UserPreferences.FILE_NAME));
		}
		catch(FileNotFoundException e)
		{
			try
			{
				new UserPreferences().savePreferences(openFileOutput(UserPreferences.FILE_NAME, Context.MODE_PRIVATE));
			}
			catch(FileNotFoundException e1)
			{

			}
		}

		mBLEComms = new BLECommunicationManager(this);

		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		// Register to receive messages.
		LocalBroadcastManager.getInstance(this).registerReceiver(mVibrateReceiver, new IntentFilter("vibrate"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mRebootReceiver, new IntentFilter("reboot"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mColourReceiver, new IntentFilter("colour"));

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(bluetoothStatusChangeReceiver, filter);

		Log.d(TAG, "Started service.");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mVibrateReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mRebootReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mColourReceiver);
		unregisterReceiver(bluetoothStatusChangeReceiver);

		mBLEComms.disconnectGatt();
	}

	private boolean isInPeriod(final Date startTime, final Date endTime)
	{
		try
		{
			final DateFormat f = new SimpleDateFormat("HH:mm:ss");
			final Date now = f.parse(f.format(new Date()));
			return ((now.after(startTime) && now.before(endTime)) || now.equals(startTime) || now.equals(endTime));
		}
		catch(ParseException e)
		{
			Log.e(TAG, e.toString());
		}
		return false;
	}

	private boolean isAllowedNow(final Date startTime, final Date endTime, final boolean requiresNonInteractive, final boolean lightsOutsidePeriod)
	{
		final boolean timeResult = isInPeriod(startTime, endTime) || lightsOutsidePeriod;
		final boolean rNIResult = !requiresNonInteractive || !pm.isInteractive();
		return timeResult && rNIResult;
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn)
	{
		super.onNotificationPosted(sbn);

		if(mBLEComms.mBluetoothAdapterStatus)
		{
			PowerManager.WakeLock wl = null;
			try
			{
				wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService");
				wl.acquire();

				final UserPreferences userPreferences = UserPreferences.getInstance();
				final Application application = userPreferences.getApp(sbn.getPackageName());

				if(application != null && isAllowedNow(application.getmStartPeriod(), application.getmEndPeriod(), application.ismUserPresent(),
						application.ismLightsOnlyOutsideOfPeriod())
						|| userPreferences.ismNotifyAllApps())
				{
					Log.d(TAG, "Processing notification.");

					if(sbn.isClearable())
					{
						mBLEComms.connect();

						Notification mNotification = sbn.getNotification();
						Bundle extras = mNotification.extras;

						Bitmap bitmap = (Bitmap) extras.get(Notification.EXTRA_LARGE_ICON);
						if(bitmap != null)
						{
							Palette palette = Palette.generate(bitmap, 1);
							Log.i(TAG, Integer.toString(palette.getVibrantSwatch().getRgb()));
						}

						boolean vibrate = true;

						if(application != null && !isInPeriod(application.getmStartPeriod(), application.getmEndPeriod()) && application.ismLightsOnlyOutsideOfPeriod())
						{
							vibrate = false;
						}

						if(vibrate)
						{
							for(int i = 1; i <= (application != null ? application.getmVibrateTimes() : 1); i++)
							{
								vibrate(application != null ? application.getmVibrateDuration() : 250);
							}
						}

						for(int i = 1; i <= (application != null ? application.getmBandColourTimes() : 1); i++)
						{
							flashBandLights(application != null ? application.getmBandColour() : 0xFFFFFFFF, userPreferences.getmBandColour(),
									application != null ? application.getmBandColourDuration() : 250);

						}
					}
				}
			}
			catch(MiBandConnectFailureException ignored)
			{
				//We couldn't connect to the band for some reason, continue quietly.
			}
			catch(Exception e)
			{
				Log.e(TAG, e.toString());
			}
			finally
			{
				mBLEComms.disconnectGatt();
				if(wl != null)
				{
					wl.release();
				}
				Log.d(TAG, "Processed notification.");
			}
		}
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

	private long startVibrate()
	{
		final long start = System.currentTimeMillis();

		final BluetoothGattCharacteristic controlPoint = mBLEComms.getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT);
		controlPoint.setValue(new byte[]{ (byte) 8, (byte) 2 });
		mBLEComms.write(controlPoint);

		final long end = System.currentTimeMillis();
		final long timeTaken = (end - start);
		return timeTaken;
	}

	private long stopVibrate()
	{
		final long start = System.currentTimeMillis();

		final BluetoothGattCharacteristic controlPoint = mBLEComms.getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT);
		controlPoint.setValue(new byte[]{ (byte) 19 });
		mBLEComms.write(controlPoint);

		final long end = System.currentTimeMillis();
		final long timeTaken = (end - start);
		return timeTaken;
	}

	private void vibrate(final long duration)
			throws MiBandConnectFailureException
	{
		startVibrate();
		threadWait(duration);
		stopVibrate();
	}

	private void reboot()
			throws MiBandConnectFailureException
	{
		final BluetoothGattCharacteristic controlPoint = mBLEComms.getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT);
		controlPoint.setValue(new byte[]{ 12 });
		mBLEComms.write(controlPoint);
	}

	private void setColor(byte r, byte g, byte b, boolean display)
			throws MiBandConnectFailureException
	{
		final BluetoothGattCharacteristic controlPoint = mBLEComms.getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT);
		controlPoint.setValue(new byte[]{ 14, r, g, b, display ? (byte) 1 : (byte) 0 });
		mBLEComms.write(controlPoint);
	}

	private byte[] convertRgb(int rgb)
	{
		int red = ((rgb >> 16) & 0x0ff) / 42;
		int green = ((rgb >> 8) & 0x0ff) / 42;
		int blue = ((rgb) & 0x0ff) / 42;

		return new byte[]{ (byte) red, (byte) green, (byte) blue };
	}

	private void flashBandLights(int flashColour, int originalColour, int duration)
			throws MiBandConnectFailureException
	{
		final byte[] flashColours = convertRgb(flashColour);
		final byte[] originalColours = convertRgb(originalColour);

		setColor(flashColours[0], flashColours[1], flashColours[2], true);
		threadWait(duration);
		setColor(originalColours[0], originalColours[1], originalColours[2], false);
	}
}


