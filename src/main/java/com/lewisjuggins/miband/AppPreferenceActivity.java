package com.lewisjuggins.miband;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import com.lewisjuggins.miband.colorpicker.ColorPickerDialog;
import com.lewisjuggins.miband.preferences.Application;
import com.lewisjuggins.miband.preferences.UserPreferences;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Lewis on 30/12/14.
 */
public class AppPreferenceActivity extends Activity
{
	private final String TAG = this.getClass().getSimpleName();

	private Application mApplication;

	private final DateFormat formatter = new SimpleDateFormat("hh:mm aa");

	private Palette mPalette;

	private int mColour;

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

				mApplication.loadValues(vibrations, vibrationDuration, flashAmount, flashDuration, noVibrate, userNotPresent, mColour);

				UserPreferences userPreferences = UserPreferences.getInstance();
				userPreferences.addOrUpdateAppEntry(mApplication);
				userPreferences.savePreferences(openFileOutput(UserPreferences.FILE_NAME, Context.MODE_PRIVATE));

				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
			catch (NumberFormatException | FileNotFoundException e)
			{
				Log.e(TAG, e.toString());
			}
		}
	};

	private View.OnClickListener mColourSetButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{
			new ColorPickerDialog(AppPreferenceActivity.this, mColour, new ColorPickerDialog.OnColorSelectedListener()
			{
				@Override public void onColorSelected(int rgb)
				{
                    Log.i(Constants.LOG_TAG, "" + rgb);
					mColour = rgb;
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
			final ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);

			final ImageView iconView = (ImageView) findViewById(R.id.iconView);
			final Drawable appIcon = pm.getApplicationIcon(info);
			iconView.setImageDrawable(appIcon);

			final Bitmap bitmap = ((BitmapDrawable) appIcon).getBitmap();
			mPalette = Palette.generate(bitmap);

			final TextView appNameLabel = (TextView) findViewById(R.id.appNameLabel);
			appNameLabel.setText(pm.getApplicationLabel(info));
		}
		catch(PackageManager.NameNotFoundException ignored)
		{

		}

		final boolean isNew = getIntent().getBooleanExtra("isNew", false);
		if(isNew)
		{
			mApplication = new Application(packageName);
		}
		//Edit application route
		else
		{
			mApplication = UserPreferences.getInstance().getApp(packageName);

			((SeekBar) findViewById(R.id.vibrationsSeekBar)).setProgress(mApplication.getmVibrateTimes());
			((SeekBar) findViewById(R.id.vibrationDurationSeekBar)).setProgress((int)mApplication.getmVibrateDuration());
			((SeekBar) findViewById(R.id.flashAmountSeekBar)).setProgress(mApplication.getmBandColourTimes());
			((SeekBar) findViewById(R.id.flashDurationSeekBar)).setProgress((int)mApplication.getmBandColourDuration());
			((CheckBox) findViewById(R.id.noVibrateCheckBox)).setChecked(mApplication.ismLightsOnlyOutsideOfPeriod());
			((CheckBox) findViewById(R.id.userNotPresentCheckBox)).setChecked(mApplication.ismUserPresent());
		}

		mColour = mApplication.getmBandColour() != 0 ? mApplication.getmBandColour() : mPalette.getVibrantColor(mPalette.getDarkVibrantColor(Color.WHITE));

		((EditText) findViewById(R.id.startTimeTextField)).setText(formatter.format(mApplication.getmStartPeriod()));
		((EditText) findViewById(R.id.endTimeTextField)).setText(formatter.format(mApplication.getmEndPeriod()));

		findViewById(R.id.startTimeTextField).setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(final View v)
			{
				TimePickerDialog dialog = new TimePickerDialog(AppPreferenceActivity.this, new TimePickerDialog.OnTimeSetListener()
				{
					@Override public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute)
					{
						final GregorianCalendar calendar = new GregorianCalendar();
						calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
						calendar.set(Calendar.MINUTE, minute);
						((EditText)findViewById(R.id.startTimeTextField)).setText(formatter.format(calendar.getTime()));
						mApplication.setmStartPeriod(calendar);
					}
				}, mApplication.getmStartPeriodCalendar().get(Calendar.HOUR_OF_DAY), mApplication.getmStartPeriodCalendar().get(Calendar.MINUTE), false);
				dialog.show();
			}
		});

		findViewById(R.id.endTimeTextField).setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(final View v)
			{
				TimePickerDialog dialog = new TimePickerDialog(AppPreferenceActivity.this, new TimePickerDialog.OnTimeSetListener()
				{
					@Override public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute)
					{
						final GregorianCalendar calendar = new GregorianCalendar();
						calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
						calendar.set(Calendar.MINUTE, minute);
						((EditText)findViewById(R.id.endTimeTextField)).setText(formatter.format(calendar.getTime()));
						mApplication.setmEndPeriod(calendar);
					}
				}, mApplication.getmEndPeriodCalendar().get(Calendar.HOUR_OF_DAY), mApplication.getmEndPeriodCalendar().get(Calendar.MINUTE), false);
				dialog.show();
			}
		});

		findViewById(R.id.doneButton).setOnClickListener(mDoneButtonListener);
		findViewById(R.id.colourButton).setOnClickListener(mColourSetButtonListener);
	}
}
