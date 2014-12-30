package com.lewisjuggins.miband;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import com.lewisjuggins.miband.model.MiBand;
import com.lewisjuggins.miband.preferences.UserPreferences;

import java.io.FileNotFoundException;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Lewis on 28/12/14.
 */
public class NotificationService extends NotificationListenerService implements Observer
{
	private String TAG = this.getClass().getSimpleName();

	private final MiBand mMiBand = MiBand.getInstance();

	// BLUETOOTH
	private String mDeviceAddress;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothMi;
	private BluetoothGatt mGatt;

	private CountDownLatch countDownLatch = new CountDownLatch(1);

	private PowerManager pm;

	//Required due to serial Bluetooth writes.
	private Object bleLock = new Object();

	private BroadcastReceiver mVibrateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Long duration = intent.getLongExtra("duration", 100);
			try
			{
				connect();
				vibrate(duration);
				disconnect();
			}
			catch(MiBandConnectFailureException e)
			{
				//ignore.
			}
		}
	};

	public static void main(String[] args)
	{
		System.out.println(Float.parseFloat("0t"));
	}

	private BroadcastReceiver mRebootReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try
			{
				connect();
				reboot();
				disconnect();
			}
			catch(MiBandConnectFailureException e)
			{
				//ignore.
			}
		}
	};

	private BroadcastReceiver mColourReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int red = intent.getIntExtra("red", 6);
			int green = intent.getIntExtra("green", 6);
			int blue = intent.getIntExtra("blue", 6);

			try
			{
				connect();
				setColor((byte) red, (byte) green, (byte) blue, true);
				disconnect();
			}
			catch(MiBandConnectFailureException e)
			{
				//ignore.
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "Binding to intent.");
		return super.onBind(intent);
	}

	@Override
	public boolean bindService(Intent service, ServiceConnection conn, int flags)
	{
		return super.bindService(service, conn, flags);
	}

	public boolean isBtEnabled() {
		final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		if(manager == null) return false;

		final BluetoothAdapter adapter = manager.getAdapter();
		if(adapter == null) return false;

		return adapter.isEnabled();
	}

	private int attempts = 0;
	private void setupBluetooth()
	{
		attempts += 1;
		mBluetoothManager = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

		for (BluetoothDevice pairedDevice : pairedDevices) {
			if (pairedDevice.getName().equals("MI") && pairedDevice.getAddress().startsWith(MiBandConstants.MAC_ADDRESS_FILTER)) {
				mDeviceAddress = pairedDevice.getAddress();
			}
		}

		if(mDeviceAddress != null)
		{
			mBluetoothMi = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
			attempts = 0;
		}
		else
		{
			//Wait 10 seconds and try again, sometimes the Bluetooth adapter takes a while.
			if(attempts <= 10)
			{
				try
				{
					Thread.sleep(10000);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				setupBluetooth();
			}
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.i(TAG, "Starting service.");

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

		setupBluetooth();

		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		// Register to receive messages.
		LocalBroadcastManager.getInstance(this).registerReceiver(mVibrateReceiver, new IntentFilter("vibrate"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mRebootReceiver, new IntentFilter("reboot"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mColourReceiver, new IntentFilter("colour"));

		Log.i(TAG, "Started service.");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mVibrateReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mRebootReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mColourReceiver);
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn)
	{
		super.onNotificationPosted(sbn);
		Log.i(TAG, "Processing notification.");

		PowerManager.WakeLock wl = null;
		try
		{
			if(sbn.isClearable())
			{
				wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "NotificationService");

				wl.acquire();
				connect();
				Notification mNotification = sbn.getNotification();
				Bundle extras = mNotification.extras;

				Bitmap bitmap = (Bitmap) extras.get(Notification.EXTRA_LARGE_ICON);
				if(bitmap != null)
				{
					Palette palette = Palette.generate(bitmap, 1);
					Log.i(TAG, Integer.toString(palette.getVibrantSwatch().getRgb()));
				}

				vibrate(100);
				setColor((byte) 6, (byte) 6, (byte) 6, true);
				//this can be used to set a delay. setColor((byte)0, (byte)0, (byte)0, false);

				//TODO: need to retrieve the previous colour to restore.
			}
		}
		catch(MiBandConnectFailureException e)
		{
			//Connection failed.
		}
		catch(Exception e)
		{
			Log.e(TAG, e.toString());
		}
		finally
		{
			Log.i(TAG, "Processed notification.");
			disconnect();
			if(wl != null)
				wl.release();
		}
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn)
	{
		Log.i(TAG, "Notification removed.");
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

	private void connect()
			throws MiBandConnectFailureException
	{
		try
		{
			countDownLatch = new CountDownLatch(2);
			mGatt = mBluetoothMi.connectGatt(this, false, mGattCallback);
			mGatt.connect();
			final boolean result = countDownLatch.await(10, TimeUnit.SECONDS);
			if(!result)
			{
				throw new MiBandConnectFailureException("Failed to connect to BLE");
			}
		}
		catch(InterruptedException e)
		{
			connect();
		}
	}

	private void disconnect()
	{
		mGatt.disconnect();
		mGatt.close();
	}

	private void write(final BluetoothGattCharacteristic characteristic)
	{
		try
		{
			countDownLatch = new CountDownLatch(1);
			mGatt.writeCharacteristic(characteristic);
			countDownLatch.await();
		}
		catch(InterruptedException e)
		{
			Log.i(TAG, e.toString());
			write(characteristic);
		}
	}

	private BluetoothGattCharacteristic getCharacteristic(UUID uuid)
	{
		return getMiliService().getCharacteristic(uuid);
	}

	private long startVibrate()
	{
		final long start = System.currentTimeMillis();

		final BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT);
		controlPoint.setValue(new byte[]{ (byte) 8, (byte) 1 });
		write(controlPoint);

		final long end = System.currentTimeMillis();
		final long timeTaken = (end - start);
		return timeTaken;
	}

	private long stopVibrate()
	{
		final long start = System.currentTimeMillis();

		final BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT);
		controlPoint.setValue(new byte[]{ (byte) 19 });
		write(controlPoint);

		final long end = System.currentTimeMillis();
		final long timeTaken = (end - start);
		return timeTaken;
	}

	private void vibrate(final long duration)
			throws MiBandConnectFailureException
	{
		synchronized(bleLock)
		{
			startVibrate();
			threadWait(duration);
			stopVibrate();
		}
	}

	private void reboot()
		throws MiBandConnectFailureException
	{
		synchronized(bleLock)
		{
			final BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT);
			controlPoint.setValue(new byte[]{ 12 });
			write(controlPoint);
		}
	}

	private void setColor(byte r, byte g, byte b, boolean display)
		throws MiBandConnectFailureException
	{
		synchronized(bleLock)
		{
			final BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT);
			controlPoint.setValue(new byte[]{ 14, r, g, b, display ? (byte) 1 : (byte) 0 });
			write(controlPoint);
		}
	}

	private BluetoothGattService getMiliService()
	{
		return mGatt.getService(MiBandConstants.UUID_SERVICE_MILI_SERVICE);
	}

	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
	{

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{
			if(status == BluetoothGatt.GATT_SUCCESS)
			{
				countDownLatch.countDown();
			}
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState)
		{
			mGatt = gatt;
			if(newState == BluetoothProfile.STATE_CONNECTED)
			{
				gatt.discoverServices();
				mMiBand.setConnected(true);
				countDownLatch.countDown();
			}
			else if(newState == BluetoothProfile.STATE_DISCONNECTED)
			{
				mMiBand.setConnected(false);
			}
			else if(newState == BluetoothProfile.STATE_CONNECTING)
			{
				mMiBand.setConnected(false);
			}
			else if(newState == BluetoothProfile.STATE_DISCONNECTING)
			{
				mMiBand.setConnected(false);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status)
		{
			countDownLatch.countDown();
		}
	};

	@Override public void update(Observable observable, Object data)
	{

	}
}


