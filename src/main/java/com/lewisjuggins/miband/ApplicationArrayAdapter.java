package com.lewisjuggins.miband;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

	public ApplicationArrayAdapter(Context context, int resource, List<Application> values)
	{
		super(context, resource, values);
		this.context = context;
		this.values = values;
	}

	@Override public View getView(int position, View convertView, ViewGroup parent)
	{
		try
		{
			//This behaves weirdly (unable to resolve the local variables) when the LayoutInflater isn't used - this is not performant.
			final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);

			final TextView textView = (TextView) rowView.findViewById(android.R.id.text1);

			final PackageManager pm = context.getPackageManager();

			final Application application = values.get(position);
			final ApplicationInfo applicationInfo = pm.getApplicationInfo(application.getmPackageName(), PackageManager.GET_META_DATA);

			application.setmAppName(pm.getApplicationLabel(applicationInfo).toString());

			textView.setText(pm.getApplicationLabel(applicationInfo));

			return rowView;
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
