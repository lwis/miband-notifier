package com.lewisjuggins.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

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

	private int attempts = 0;

	private String mDeviceAddress;

	private CountDownLatch mConnectionCountDownLatch;

	private CountDownLatch mWriteCountdownLatch;

	private BluetoothGatt mGatt;

	public boolean mDeviceConnected = false;

	private final Context mContext;

	public boolean mBluetoothAdapterStatus = false;

	public boolean setupComplete = false;

	private BluetoothDevice mBluetoothMi;

	private BluetoothGattCharacteristic mControlPointChar;


	public BLECommunicationManager(final Context context)
	{
		this.mContext = context;
		setupBluetooth();
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

	public synchronized void connectGatt()
		throws MiBandConnectFailureException
	{
		Log.d(TAG, "Establishing connection to gatt");

		mConnectionCountDownLatch = new CountDownLatch(2);
		mGatt = mBluetoothMi.connectGatt(mContext, true, mGattCallback);
		mGatt.connect();

		try
		{
			mConnectionCountDownLatch.await(10, TimeUnit.SECONDS);
		}
		catch(InterruptedException e)
		{
			Log.d(TAG, "Failed to connect to gatt");
			throw new MiBandConnectFailureException("Failed to connect");
		}

		Log.d(TAG, "Established connection to gatt");
	}

	public synchronized void disconnectGatt()
	{
		if(mGatt != null)
		{
			mGatt.disconnect();
			mGatt.close();
		}
	}

	public synchronized void write(final BluetoothGattCharacteristic characteristic)
	{
		try
		{
			mWriteCountdownLatch = new CountDownLatch(1);
			mGatt.writeCharacteristic(characteristic);
			mWriteCountdownLatch.await();
		}
		catch(InterruptedException e)
		{
			Log.i(TAG, e.toString());
			write(characteristic);
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
				mConnectionCountDownLatch.countDown();
			}
		}

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
		{
			switch(newState)
			{
				case BluetoothProfile.STATE_CONNECTED:
					Log.d(TAG, "Gatt state: connected");

					gatt.discoverServices();
					mDeviceConnected = true;
					mConnectionCountDownLatch.countDown();
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
			mWriteCountdownLatch.countDown();
		}
	};
}
