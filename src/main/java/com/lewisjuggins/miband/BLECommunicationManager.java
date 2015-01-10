package com.lewisjuggins.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.lewisjuggins.miband.bluetooth.BLETask;
import com.lewisjuggins.miband.bluetooth.QueueConsumer;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Lewis on 01/01/15.
 */
public class BLECommunicationManager
{
	private String TAG = this.getClass().getSimpleName();

	/*
		Read as:    cc 01 f4 01 00 00 f4 01 f2 01 60 09
		Written as: cc 01 f4 01 00 00 f4 01 00 00 00 00
		connIntMin: 575ms
		connIntMax: 625ms
		latency: 0ms
		timeout: 5000ms
		connInt: 622ms
		advInt: 1500ms


		Read as:    27 00 31 00 00 00 f4 01 30 00 60 09
		Written as: 27 00 31 00 00 00 f4 01 00 00 00 00
		connIntMin: 48ms
		connIntMax: 61ms
		latency: 0ms
		timeout: 5000ms
		connInt: 60ms
		advInt: 1500ms

		Read as:    06 00 50 00 02 00 d0 07 63 00 60 09
		Written as:
		connIntMin: 7ms
		connIntMax: 100ms
		latency: 2ms
		timeout: 20000ms
		connInt: 123ms
		advInt: 1500ms
	 */

	public byte[] mCurrentLeParams;

	public static final byte[] mLowLatencyLeParams = new byte[]{0x27, 0x00, 0x31, 0x00, 0x00, 0x00, (byte)0xf4, 0x01, 0x00, 0x00, 0x00, 0x00};

	public static final byte[] mHighLatencyLeParams = new byte[]{(byte)0xcc, 0x01, (byte)0xf4, 0x01, 0x00, 0x00, (byte)0xf4, 0x01, 0x00, 0x00, 0x00, 0x00};

	private int attempts = 0;

	private String mDeviceAddress;

	private BluetoothGatt mGatt;

	private boolean mDeviceConnected = false;

	private final Context mContext;

	public boolean mBluetoothAdapterStatus = false;

	public boolean setupComplete = false;

	private BluetoothDevice mBluetoothMi;

	private BluetoothGattCharacteristic mControlPointChar;

	private CountDownLatch mWriteLatch;

	private CountDownLatch mConnectionLatch;

	private QueueConsumer mQueueConsumer;

	public BLECommunicationManager(final Context context)
	{
		this.mContext = context;
		setupBluetooth();
		mQueueConsumer = new QueueConsumer(this);
		Thread t = new Thread(mQueueConsumer);
		t.start();
	}

	public void queueTask(final BLETask task)
	{
		mQueueConsumer.add(task);
	}

	public void setupBluetooth()
	{
		Log.d(TAG, "Initialising Bluetooth connection");

		if(BluetoothAdapter.getDefaultAdapter().isEnabled())
		{
			attempts += 1;
			final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

			for(BluetoothDevice pairedDevice : pairedDevices)
			{
				if(pairedDevice.getName().equals("MI") && pairedDevice.getAddress().startsWith(MiBandConstants.MAC_ADDRESS_FILTER))
				{
					mDeviceAddress = pairedDevice.getAddress();
				}
			}

			if(mDeviceAddress != null)
			{
				mBluetoothMi = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
				attempts = 0;
				setupComplete = true;
				mBluetoothAdapterStatus = true;

				try
				{
					connectGatt();
				}
				catch(MiBandConnectFailureException e)
				{
					Log.w(TAG, "Could not connect to Mi Band");
				}

				Log.d(TAG, "Initialising Bluetooth connection complete");
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
	}

	private synchronized void connectGatt()
		throws MiBandConnectFailureException
	{
		Log.d(TAG, "Establishing connection to gatt");

		mGatt = mBluetoothMi.connectGatt(mContext, true, mGattCallback);

		//TODO: Register for connection state change.
		try
		{
			mConnectionLatch = new CountDownLatch(2);
			mGatt.connect();
			mConnectionLatch.await(5, TimeUnit.SECONDS);
			Log.d(TAG, "Established connection to gatt");
		}
		catch(InterruptedException e)
		{
			mDeviceConnected = false;
			Log.i(TAG, "Failed to connect to gatt, timeout");
			throw new MiBandConnectFailureException("Failed to connect");
		}
	}

	public synchronized void disconnectGatt()
	{
		if(mGatt != null)
		{
			new Handler(Looper.getMainLooper()).post(new Runnable()
			{
				@Override public void run()
				{
					mGatt.disconnect();
					mGatt.close();
					mGatt = null;
				}
			});
		}
	}

	public synchronized void write(final BluetoothGattCharacteristic characteristic)
		throws MiBandConnectFailureException
	{
		if(BluetoothAdapter.getDefaultAdapter().isEnabled())
		{
			if(mDeviceConnected)
			{
				try
				{
					Log.d(TAG, "Writing!");
					mWriteLatch = new CountDownLatch(1);
					mGatt.writeCharacteristic(characteristic);
					mWriteLatch.await(2, TimeUnit.SECONDS);
				}
				catch(InterruptedException e)
				{
					Log.i(TAG, "Failed to write, timeout");
				}
			}
			else
			{
				Log.d(TAG, "Device not connected, connecting");
				connectGatt();
				write(characteristic);
			}
		}
		else
		{
			Log.d(TAG, "Bluetooth not enabled, write failed");
		}
	}

	private void setLowLatency()
			throws MiBandConnectFailureException
	{
		final BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_LE_PARAMS);
		characteristic.setValue(mLowLatencyLeParams);
		write(characteristic);
		//TODO: This currently doesn't work, as the device doesn't react fast enough to change the connection before the next write.
	}

	public void setHighLatency()
	{
		try
		{
			final BluetoothGattCharacteristic characteristic = getCharacteristic(MiBandConstants.UUID_CHARACTERISTIC_LE_PARAMS);
			characteristic.setValue(mHighLatencyLeParams);
			write(characteristic);
		}
		catch(MiBandConnectFailureException ignored)
		{

		}
	}

	private BluetoothGattService getMiliService()
	{
		return mGatt.getService(MiBandConstants.UUID_SERVICE_MILI_SERVICE);
	}

	public BluetoothGattCharacteristic getCharacteristic(UUID uuid)
	{
		if(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT.equals(uuid) && mControlPointChar != null)
		{
			return mControlPointChar;
		}
		else if(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT.equals(uuid) && mControlPointChar == null)
		{
			mControlPointChar = getMiliService().getCharacteristic(uuid);
			return mControlPointChar;
		}

		return getMiliService().getCharacteristic(uuid);
	}

	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
	{
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{
			if(status == BluetoothGatt.GATT_SUCCESS)
			{
				mConnectionLatch.countDown();
			}
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
		{
			mGatt = gatt;

			switch(newState)
			{
				case BluetoothProfile.STATE_CONNECTED:
					Log.d(TAG, "Gatt state: connected");
					gatt.discoverServices();
					mDeviceConnected = true;
					mConnectionLatch.countDown();
					break;
				default:
					Log.d(TAG, "Gatt state: not connected");
					mDeviceConnected = false;
					break;
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
		{
			Log.d(TAG, "Write successful: " + Arrays.toString(characteristic.getValue()));
			mWriteLatch.countDown();
		}
	};
}
