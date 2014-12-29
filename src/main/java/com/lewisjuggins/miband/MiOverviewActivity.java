package com.lewisjuggins.miband;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.lewisjuggins.miband.colorpicker.ColorPickerDialog;
import com.lewisjuggins.miband.model.MiBand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MiOverviewActivity extends Activity implements Observer
{
	private final String TAG = this.getClass().getSimpleName();

	private final MiBand mMiBand = MiBand.getInstance();

	private final ArrayList<String> tempArray = new ArrayList<>();

	private ArrayAdapter<String> tempAdapter;

	private View.OnClickListener mVibrateButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			vibrate(250);
		}
	};

	private View.OnClickListener mRebootButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			reboot();
		}
	};

	private View.OnClickListener mColourSetButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			new ColorPickerDialog(MiOverviewActivity.this, 1, new ColorPickerDialog.OnColorSelectedListener()
			{
				@Override public void onColorSelected(int rgb)
				{
					int red = ((rgb >> 16) & 0x0ff) / 42;
					int green = ((rgb >> 8) & 0x0ff) / 42;
					int blue = ((rgb) & 0x0ff) / 42;

					setColour(red, green, blue);
				}
			}).show();
		}
	};

	private View.OnClickListener mAddButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			AlertDialog.Builder builderSingle = new AlertDialog.Builder(
					MiOverviewActivity.this);
			builderSingle.setTitle("Select an app");

			builderSingle.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							dialog.dismiss();
						}
					});
			final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MiOverviewActivity.this, android.R.layout.simple_list_item_1, getApps());
			builderSingle.setAdapter(arrayAdapter,
					new DialogInterface.OnClickListener()
					{

						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							String strName = arrayAdapter.getItem(which);

							tempArray.add(strName);
							tempAdapter.notifyDataSetChanged();
						}
					});
			builderSingle.show();
		}
	};

	private void vibrate(long duration)
	{
		Intent intent = new Intent("vibrate");
		intent.putExtra("duration", duration);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void reboot()
	{
		Intent intent = new Intent("reboot");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void setColour(int r, int g, int b)
	{
		Intent intent = new Intent("colour");
		intent.putExtra("red", r);
		intent.putExtra("green", g);
		intent.putExtra("blue", b);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mMiBand.addObserver(this);

		getActionBar().hide();

		setContentView(R.layout.activity_mi_overview);

		findViewById(R.id.vibrateButton).setOnClickListener(mVibrateButtonListener);
		findViewById(R.id.rebootButton).setOnClickListener(mRebootButtonListener);
		findViewById(R.id.colourButton).setOnClickListener(mColourSetButtonListener);
		findViewById(R.id.addButton).setOnClickListener(mAddButtonListener);

		tempAdapter = new ArrayAdapter<>(MiOverviewActivity.this, android.R.layout.simple_list_item_1, tempArray);
		((ListView) findViewById(R.id.listView)).setAdapter(tempAdapter);
	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_overview, menu);
		return true;
	}

	private ArrayList<String> getApps()
	{
		final ArrayList toRet = new ArrayList();
		final PackageManager pm = getPackageManager();
		//get a list of installed apps.
		List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		for(ApplicationInfo packageInfo : packages)
		{
			//toRet.add(pm.getApplicationLabel(packageInfo));
			toRet.add(packageInfo.packageName);
		}

		Collections.sort(toRet);
		return toRet;
	}

	@Override
	public void update(Observable observable, Object data)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
			}
		});
	}

}
