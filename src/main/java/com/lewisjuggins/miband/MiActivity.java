package com.lewisjuggins.miband;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import java.util.List;
import java.util.Set;

public class MiActivity extends Activity {

    public static final String MAC_ADDRESS_FILTER = "88:0F:10";

    private BluetoothAdapter mBluetoothAdapter;

    private Callback mBluetoothScanCallback = new Callback();

    private TextView mTextView;

    private Handler mHandler = new Handler();

    private static final long SCAN_PERIOD = 10000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
		//startActivity(intent);

		super.onCreate(savedInstanceState);
		//getBaseContext().startService(new Intent(getBaseContext(), NotificationService.class));
		getActionBar().hide();
		setContentView(R.layout.activity_mi);
		mTextView = (TextView) findViewById(R.id.text_search);
		mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
	}

	@Override
	public void onResume() {
		super.onResume();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        for (BluetoothDevice pairedDevice : pairedDevices) {
            if (pairedDevice.getName().equals("MI") && pairedDevice.getAddress().startsWith(MAC_ADDRESS_FILTER)) {
                Intent intent = new Intent(getApplicationContext(), MiOverviewActivity.class);
                intent.putExtra("address", pairedDevice.getAddress());
                startActivity(intent);
            }
        }

		if(mBluetoothAdapter == null)
		{
			//L.toast(this, "Please turn Bluetooth on!");
		}
		else
		{
			scanLeDevice(true);
		}
	}

	private void scanLeDevice(final boolean enable) {
		if (enable)
        {
			mTextView.setText(R.string.looking_for_miband);
			// Stops scanning after a pre-defined scan period.
			//mHandler.postDelayed(new Runnable() {
			//	@Override
			//	public void run() {
            //       mBluetoothAdapter.getBluetoothLeScanner().startScan(mBluetoothScanCallback);
			//		mTextView.setText(R.string.not_found);
			//	}
			//}, SCAN_PERIOD);

            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mBluetoothScanCallback);
		} else {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mBluetoothScanCallback);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		scanLeDevice(false);
	}


    class Callback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    }
}
