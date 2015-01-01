package com.lewisjuggins.miband;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Lewis on 01/01/15.
 */
public class StartupReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Intent serviceIntent = new Intent(context, MiBandCommunicationService.class);
		context.startService(serviceIntent);
	}
}