package com.lewisjuggins.miband;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
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

	private boolean isInPeriod(Date startTime, Date endTime)
	{
		try
		{
			//Clean the dates so they are only times, for comparison.
			final DateFormat f = new SimpleDateFormat("HH:mm");
			final Date now = f.parse(f.format(new Date()));
			startTime = f.parse(f.format(startTime));
			endTime = f.parse(f.format(endTime));
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

		final boolean priorityActive = ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getMode() == AudioManager.RINGER_MODE_SILENT;

		Log.i(TAG, Boolean.toString(timeResult && rNIResult));
		return timeResult && rNIResult;
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn)
	{
		super.onNotificationPosted(sbn);

		final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

		final PowerManager.WakeLock wl = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService");
		wl.acquire(20000);

		if(BluetoothAdapter.getDefaultAdapter().isEnabled())
		{
			try
			{

				final UserPreferences userPreferences = UserPreferences.getInstance();
				final Application application = userPreferences.getApp(sbn.getPackageName());

				if((application != null && isAllowedNow(application.getmStartPeriod(), application.getmEndPeriod(), application.ismUserPresent(),
						application.ismLightsOnlyOutsideOfPeriod())))
				{
					Log.d(TAG, "Processing notification.");

					if(sbn.isClearable())
					{
						final int vibrateTimes = !isInPeriod(application.getmStartPeriod(), application.getmEndPeriod()) && application.ismLightsOnlyOutsideOfPeriod() ? 0 : application.getmVibrateTimes();
						final long vibrateDuration = application.getmVibrateDuration();
						final int flashTimes = application.getmBandColourTimes();
						final int flashColour = application.getmBandColour();
						final int originalColour = userPreferences.getmBandColour();
						final long flashDuration = application.getmBandColourDuration();

						final Intent notifyIntent = new Intent("notifyBand");
						notifyIntent.putExtra("vibrateTimes", vibrateTimes);
						notifyIntent.putExtra("vibrateDuration", vibrateDuration);
						notifyIntent.putExtra("flashTimes", flashTimes);
						notifyIntent.putExtra("flashColour", flashColour);
						notifyIntent.putExtra("originalColour", originalColour);
						notifyIntent.putExtra("flashDuration", flashDuration);

						lbm.sendBroadcastSync(notifyIntent);
					}
				}
			}
			catch(Exception e)
			{
				Log.e(TAG, e.toString());
			}
			finally
			{
				if(wl.isHeld())
				{
					wl.release();
				}
				Log.d(TAG, "Processed notification.");
			}
		}
	}
}


