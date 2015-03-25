package com.lewisjuggins.miband.preferences;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Lewis on 30/12/14.
 */
public class UserPreferences
{
	private static final String TAG = UserPreferences.class.getSimpleName();

	public static final String FILE_NAME = "prefs.json";

	private HashMap<String, Application> mAppsToNotify = new HashMap<>();

	private int mBandColour = 0xFFFFFFFF;

    private String miBandMAC = "";

	private static UserPreferences INSTANCE;

	//This should never be invoked, but is required for Gson instantiation.
	public UserPreferences(){}

	public static UserPreferences getInstance()
	{
		return INSTANCE;
	}

	public synchronized static void loadPreferences(FileInputStream fis)
	{
		try
		{
			Gson gson = new Gson();
			JsonReader jsonReader = new JsonReader(new BufferedReader(new InputStreamReader(fis)));

			UserPreferences userPreferences = gson.fromJson(jsonReader, UserPreferences.class);

			jsonReader.close();
			fis.close();

			INSTANCE = userPreferences;
		}
		catch(FileNotFoundException e)
		{
			Log.e(TAG, "No prefs");
		}
		catch(IOException e)
		{

		}
		finally
		{

		}
	}

	public synchronized void savePreferences(FileOutputStream fos)
	{
		try
		{
			Gson gson = new Gson();
			String json = gson.toJson(this);

			fos.write(json.getBytes());
			fos.close();

			INSTANCE = this;
		}
		catch(FileNotFoundException e)
		{

		}
		catch(IOException e)
		{

		}
		finally
		{

		}
	}

	public void addOrUpdateAppEntry(final Application application)
	{
		mAppsToNotify.put(application.getmPackageName(), application);
	}

	public List<Application> getAppArray()
	{
		final List<Application> toRet = new ArrayList<>();
		toRet.addAll(mAppsToNotify.values());
		return toRet;
	}

	public Application getApp(String key)
	{
		return mAppsToNotify.get(key);
	}

	public void removeApp(String key)
	{
		mAppsToNotify.remove(key);
	}

	public int getmBandColour()
	{
		return mBandColour;
	}

	public void setmBandColour(int mBandColour)
	{
		this.mBandColour = mBandColour;
	}

    public String getMiBandMAC() { return miBandMAC; }

    public void setMiBandMAC(String miBandMAC) { this.miBandMAC = miBandMAC; }
}
