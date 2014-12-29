package com.lewisjuggins.miband;

import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;

import android.widget.TextView;
import com.lewisjuggins.miband.colorpicker.ColorPickerDialog;
import com.lewisjuggins.miband.model.MiBand;

public class MiOverviewActivity extends Activity implements Observer
{
	private final MiBand mMiBand = MiBand.getInstance();

	private View.OnClickListener mVibrateButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			long duration = Long.parseLong(((EditText) findViewById(R.id.durationField)).getText().toString());
			vibrate(duration);
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

	private void vibrate(long duration) {
		Intent intent = new Intent("vibrate");
		intent.putExtra("duration", duration);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void reboot() {
		Intent intent = new Intent("reboot");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void setColour(int r, int g, int b) {
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

	@Override
	public void update(Observable observable, Object data) {
		runOnUiThread(new Runnable()
		{
			@Override
			public void run() {
				if(mMiBand.isConnected())
				{
					((TextView) findViewById(R.id.connectedLabel)).setTextColor(Color.GREEN);
				}
				else
				{
					((TextView) findViewById(R.id.connectedLabel)).setTextColor(Color.RED);
				}
			}
		});
	}

}
