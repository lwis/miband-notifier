package com.lewisjuggins.miband;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Lewis on 30/12/14.
 */
public class AppPreferenceActivity extends Activity
{
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getActionBar().hide();

		setContentView(R.layout.activity_edit_app_prefs);

		final String packageName = getIntent().getStringExtra("packageName");
		PackageManager pm = getPackageManager();
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
			e.printStackTrace();
		}
	}
}
