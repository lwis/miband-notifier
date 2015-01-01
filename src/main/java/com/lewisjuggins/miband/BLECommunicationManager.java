package com.lewisjuggins.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import com.lewisjuggins.miband.bluetooth.AsyncBluetoothGatt;

import java.util.Set;
import java.util.UUID;

/**
 * Created by Lewis on 01/01/15.
 */
public class BLECommunicationManager
{
	private String TAG = this.getClass().getSimpleName();

	private int attempts = 0;

	private String mDeviceAddress;

	private AsyncBluetoothGatt mGatt;

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

		mGatt = new AsyncBluetoothGatt(mBluetoothMi, mContext, true);

		try
		{
			mGatt.connect().waitSafely(10000);
			mGatt.discoverServices().waitSafely(10000);
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
			mGatt.writeCharacteristic(characteristic).waitSafely();
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
}
