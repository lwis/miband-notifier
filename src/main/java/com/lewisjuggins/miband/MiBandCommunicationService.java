package com.lewisjuggins.miband;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Lewis on 29/12/14.
 */
public class MiBandCommunicationService extends Service
{
	@Override public IBinder onBind(Intent intent)
	{
		return null;
	}
}
