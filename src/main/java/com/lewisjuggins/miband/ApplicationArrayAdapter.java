package com.lewisjuggins.miband;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.lewisjuggins.miband.preferences.Application;

import java.util.List;

/**
 * Created by Lewis on 31/12/14.
 */
public class ApplicationArrayAdapter extends ArrayAdapter<Application>
{
	private final Context context;
	private final List<Application> values;
	private final int mResource;

	public ApplicationArrayAdapter(Context context, int resource, List<Application> values)
	{
		super(context, resource, values);
		this.context = context;
		this.values = values;
		this.mResource = resource;
	}

	@Override public View getView(int position, View convertView, ViewGroup parent)
	{
		try
		{
			if(convertView == null)
			{
				final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(mResource, parent, false);
			}

			final PackageManager pm = context.getPackageManager();

			final Application application = values.get(position);

			final ApplicationInfo applicationInfo = pm.getApplicationInfo(application.getmPackageName(), PackageManager.GET_META_DATA);

			application.setmAppName(pm.getApplicationLabel(applicationInfo).toString());

			final ImageView imageView = (ImageView) convertView.findViewById(R.id.appIcon);
			imageView.setImageDrawable(pm.getApplicationIcon(applicationInfo));

			final TextView textView = (TextView) convertView.findViewById(R.id.appName);
			textView.setText(pm.getApplicationLabel(applicationInfo));

			return convertView;
		}
		catch(PackageManager.NameNotFoundException e)
		{

		}
		return null;
	}

	@Override public Application getItem(int position)
	{
		return values.get(position);
	}
}
