package com.lewisjuggins.miband;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.lewisjuggins.miband.colorpicker.ColorPickerDialog;
import com.lewisjuggins.miband.preferences.Application;
import com.lewisjuggins.miband.preferences.UserPreferences;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Lewis on 30/12/14.
 */
public class AppPreferenceActivity extends Activity
{
	private final String TAG = this.getClass().getSimpleName();

	private Application mApplication;

	private final DateFormat formatter = new SimpleDateFormat("HH:mm:ss");

	private View.OnClickListener mDoneButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			try
			{
				final int vibrations = ((SeekBar) findViewById(R.id.vibrationsSeekBar)).getProgress();
				final int vibrationDuration = ((SeekBar) findViewById(R.id.vibrationDurationSeekBar)).getProgress();
				final int flashAmount = ((SeekBar) findViewById(R.id.flashAmountSeekBar)).getProgress();
				final int flashDuration = ((SeekBar) findViewById(R.id.flashDurationSeekBar)).getProgress();
				final boolean noVibrate = ((CheckBox) findViewById(R.id.noVibrateCheckBox)).isChecked();
				final boolean userNotPresent = ((CheckBox) findViewById(R.id.userNotPresentCheckBox)).isChecked();

				final Date startTime = formatter.parse(((EditText) findViewById(R.id.startTimeTextField)).getText().toString());
				final Date endTime = formatter.parse(((EditText) findViewById(R.id.endTimeTextField)).getText().toString());

				mApplication.loadValues(vibrations, vibrationDuration, flashAmount, flashDuration, startTime, endTime, noVibrate, userNotPresent);

				UserPreferences userPreferences = UserPreferences.getInstance();
				userPreferences.addOrUpdateAppEntry(mApplication);
				userPreferences.savePreferences(openFileOutput(UserPreferences.FILE_NAME, Context.MODE_PRIVATE));

				Intent intent = new Intent(getApplicationContext(), MiOverviewActivity.class);
				startActivity(intent);
			}
			catch (NumberFormatException | ParseException | FileNotFoundException e)
			{
				Log.e(TAG, e.toString());
			}
		}
	};

	private View.OnClickListener mColourSetButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			new ColorPickerDialog(AppPreferenceActivity.this, mApplication.getmBandColour(), new ColorPickerDialog.OnColorSelectedListener()
			{
				@Override public void onColorSelected(int rgb)
				{
					mApplication.setmBandColour(rgb);
				}
			}).show();
		}
	};

	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getActionBar().hide();

		setContentView(R.layout.activity_edit_app_prefs);

		//New application route.
		final String packageName = getIntent().getStringExtra("packageName");

		final PackageManager pm = getPackageManager();
		try
		{
			ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);

			ImageView iconView = (ImageView) findViewById(R.id.iconView);
			Drawable appIcon = pm.getApplicationIcon(info);
			iconView.setImageDrawable(appIcon);

			TextView appNameLabel = (TextView) findViewById(R.id.appNameLabel);
			appNameLabel.setText(pm.getApplicationLabel(info));
		}
		catch(PackageManager.NameNotFoundException e)
		{

		}

		final boolean isNew = getIntent().getBooleanExtra("isNew", false);
		if(isNew)
		{
			mApplication = new Application();
			mApplication.setmPackageName(packageName);
		}
		//Edit application route
		else
		{
			mApplication = UserPreferences.getInstance().getApp(packageName);

			((SeekBar) findViewById(R.id.vibrationsSeekBar)).setProgress(mApplication.getmVibrateTimes());
			((SeekBar) findViewById(R.id.vibrationDurationSeekBar)).setProgress(mApplication.getmVibrateDuration());
			((SeekBar) findViewById(R.id.flashAmountSeekBar)).setProgress(mApplication.getmBandColourTimes());
			((SeekBar) findViewById(R.id.flashDurationSeekBar)).setProgress(mApplication.getmBandColourDuration());
			((CheckBox) findViewById(R.id.noVibrateCheckBox)).setChecked(mApplication.ismLightsOnlyOutsideOfPeriod());
			((CheckBox) findViewById(R.id.userNotPresentCheckBox)).setChecked(mApplication.ismUserPresent());
			((EditText) findViewById(R.id.startTimeTextField)).setText(formatter.format(mApplication.getmStartPeriod()));
			((EditText) findViewById(R.id.endTimeTextField)).setText(formatter.format(mApplication.getmEndPeriod()));
		}

		findViewById(R.id.doneButton).setOnClickListener(mDoneButtonListener);
		findViewById(R.id.colourButton).setOnClickListener(mColourSetButtonListener);
	}
}
