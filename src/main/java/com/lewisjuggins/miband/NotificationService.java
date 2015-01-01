package com.lewisjuggins.miband;

import android.app.Notification;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
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

		final Intent serviceIntent = new Intent(this, MiBandCommunicationService.class);
		startService(serviceIntent);

		Log.d(TAG, "Started service.");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
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
		final boolean rNIResult = !requiresNonInteractive || !((PowerManager) getSystemService(Context.POWER_SERVICE)).isInteractive();
		return timeResult && rNIResult;
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn)
	{
		super.onNotificationPosted(sbn);

		final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

		final BluetoothManager mBluetoothManager = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));

		if(mBluetoothManager != null)
		{
			PowerManager.WakeLock wl = null;
			try
			{
				wl = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService");
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
						lbm.sendBroadcastSync(new Intent("connect"));

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
								final Intent vibrateIntent = new Intent("vibrate");
								vibrateIntent.putExtra("duration", application != null ? application.getmVibrateDuration() : 250L);
								lbm.sendBroadcastSync(vibrateIntent);
							}
						}

						for(int i = 1; i <= (application != null ? application.getmBandColourTimes() : 1); i++)
						{
							final Intent vibrateIntent = new Intent("flashBandLights");
							vibrateIntent.putExtra("flashColour", application != null ? application.getmBandColour() : 0xFFFFFFFF);
							vibrateIntent.putExtra("bandColour", userPreferences.getmBandColour());
							vibrateIntent.putExtra("flashDuration", application != null ? application.getmBandColourDuration() : 250L);
							lbm.sendBroadcastSync(vibrateIntent);
						}
					}
				}
			}
			catch(Exception e)
			{
				Log.e(TAG, e.toString());
			}
			finally
			{
				lbm.sendBroadcast(new Intent("disconnect"));
				if(wl != null)
				{
					wl.release();
				}
				Log.d(TAG, "Processed notification.");
			}
		}
	}
}


