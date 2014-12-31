package com.lewisjuggins.miband;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.lewisjuggins.miband.colorpicker.ColorPickerDialog;
import com.lewisjuggins.miband.model.MiBand;
import com.lewisjuggins.miband.preferences.UserPreferences;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MiOverviewActivity extends Activity implements Observer
{
	private final String TAG = this.getClass().getSimpleName();

	private final MiBand mMiBand = MiBand.getInstance();

	private UserPreferences userPreferences;

	private ArrayAdapter<String> tempAdapter;

	private ListView mListView;

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
					Log.i(TAG, "" + rgb);
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

							Intent intent = new Intent(getApplicationContext(), AppPreferenceActivity.class);
							intent.putExtra("packageName", strName);
							intent.putExtra("isNew", true);
							startActivity(intent);
						}
					});
			builderSingle.show();
		}
	};

	private AdapterView.OnItemLongClickListener mItemLongClickListerer = new AdapterView.OnItemLongClickListener()
	{
		public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
		{
			new AlertDialog.Builder(MiOverviewActivity.this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle("Delete?")
					.setPositiveButton("Yes", new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							final String packageName = mListView.getItemAtPosition(position).toString();
							userPreferences.removeApp(packageName);
							try
							{
								userPreferences.savePreferences(openFileOutput(UserPreferences.FILE_NAME, Context.MODE_PRIVATE));
								tempAdapter.clear();
								tempAdapter.addAll(userPreferences.getAppArray());
								tempAdapter.notifyDataSetChanged();
							}
							catch(FileNotFoundException ignored)
							{
							}
						}

					})
					.setNegativeButton("No", null)
					.show();

			return true;
		}
	};

	private AdapterView.OnItemClickListener mItemClickListerer = new AdapterView.OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			final String packageName = mListView.getItemAtPosition(position).toString();

			Intent intent = new Intent(getApplicationContext(), AppPreferenceActivity.class);
			intent.putExtra("packageName", packageName);
			startActivity(intent);
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

	private boolean isEnabled()
	{
		String pkgName = getPackageName();
		final String flat = Settings.Secure.getString(getContentResolver(),
				"enabled_notification_listeners");
		if(!TextUtils.isEmpty(flat))
		{
			final String[] names = flat.split(":");
			for(int i = 0; i < names.length; i++)
			{
				final ComponentName cn = ComponentName.unflattenFromString(names[i]);
				if(cn != null)
				{
					if(TextUtils.equals(pkgName, cn.getPackageName()))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

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

		userPreferences = UserPreferences.getInstance();

		mMiBand.addObserver(this);

		getActionBar().hide();

		setContentView(R.layout.activity_mi_overview);

		findViewById(R.id.vibrateButton).setOnClickListener(mVibrateButtonListener);
		findViewById(R.id.rebootButton).setOnClickListener(mRebootButtonListener);
		findViewById(R.id.colourButton).setOnClickListener(mColourSetButtonListener);
		findViewById(R.id.addButton).setOnClickListener(mAddButtonListener);

		tempAdapter = new ArrayAdapter<>(MiOverviewActivity.this, android.R.layout.simple_list_item_1, userPreferences.getAppArray());
		mListView = ((ListView) findViewById(R.id.listView));
		mListView.setAdapter(tempAdapter);
		mListView.setOnItemLongClickListener(mItemLongClickListerer);
		mListView.setOnItemClickListener(mItemClickListerer);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		boolean isEnabledNLS = isEnabled();
		if(!isEnabledNLS)
		{
			showConfirmDialog();
		}
	}

	private void showConfirmDialog()
	{
		new AlertDialog.Builder(this)
				.setMessage("Please enable notification access")
				.setTitle("Notification Access")
				.setIconAttribute(android.R.attr.alertDialogIcon)
				.setCancelable(true)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								finish();
							}
						})
				.create().show();
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
