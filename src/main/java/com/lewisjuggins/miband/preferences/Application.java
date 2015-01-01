package com.lewisjuggins.miband.preferences;

import java.util.Date;

/**
 * Created by Lewis on 30/12/14.
 */
public class Application
{
	private String mPackageName;

	private int mVibrateTimes;

	private long mVibrateDuration;

	private int mBandColourTimes;

	private long mBandColourDuration;

	private int mBandColour = 0xFFFFFFFF;

	private Date mStartPeriod;

	private Date mEndPeriod;

	private boolean mLightsOnlyOutsideOfPeriod;

	private boolean mUserPresent;

	private transient String mAppName;

	public Application()
	{

	}

	public Application(String packageName, String applicationLabel)
	{
		this.mPackageName = packageName;
		this.mAppName = applicationLabel;
	}

	public void loadValues(int mVibrateTimes, int mVibrateDuration, int mBandColourTimes, int mBandColourDuration, Date mStartPeriod, Date mEndPeriod,
			boolean mLightsOnlyOutsideOfPeriod, boolean mUserPresent)
	{
		this.mVibrateTimes = mVibrateTimes;
		this.mVibrateDuration = mVibrateDuration;
		this.mBandColourTimes = mBandColourTimes;
		this.mBandColourDuration = mBandColourDuration;
		this.mStartPeriod = mStartPeriod;
		this.mEndPeriod = mEndPeriod;
		this.mLightsOnlyOutsideOfPeriod = mLightsOnlyOutsideOfPeriod;
		this.mUserPresent = mUserPresent;
	}

	public boolean ismUserPresent()
	{
		return mUserPresent;
	}

	public void setmUserPresent(boolean mUserPresent)
	{
		this.mUserPresent = mUserPresent;
	}

	public String getmPackageName()
	{
		return mPackageName;
	}

	public void setmPackageName(String mPackageName)
	{
		this.mPackageName = mPackageName;
	}

	public int getmVibrateTimes()
	{
		return mVibrateTimes;
	}

	public void setmVibrateTimes(int mVibrateTimes)
	{
		this.mVibrateTimes = mVibrateTimes;
	}

	public long getmVibrateDuration()
	{
		return mVibrateDuration;
	}

	public void setmVibrateDuration(int mVibrateDuration)
	{
		this.mVibrateDuration = mVibrateDuration;
	}

	public int getmBandColourTimes()
	{
		return mBandColourTimes;
	}

	public void setmBandColourTimes(int mBandColourTimes)
	{
		this.mBandColourTimes = mBandColourTimes;
	}

	public long getmBandColourDuration()
	{
		return mBandColourDuration;
	}

	public void setmBandColourDuration(int mBandColourDuration)
	{
		this.mBandColourDuration = mBandColourDuration;
	}

	public int getmBandColour()
	{
		return mBandColour;
	}

	public void setmBandColour(int mBandColour)
	{
		this.mBandColour = mBandColour;
	}

	public Date getmStartPeriod()
	{
		return mStartPeriod;
	}

	public void setmStartPeriod(Date mStartPeriod)
	{
		this.mStartPeriod = mStartPeriod;
	}

	public Date getmEndPeriod()
	{
		return mEndPeriod;
	}

	public void setmEndPeriod(Date mEndPeriod)
	{
		this.mEndPeriod = mEndPeriod;
	}

	public boolean ismLightsOnlyOutsideOfPeriod()
	{
		return mLightsOnlyOutsideOfPeriod;
	}

	public void setmLightsOnlyOutsideOfPeriod(boolean mLightsOnlyOutsideOfPeriod)
	{
		this.mLightsOnlyOutsideOfPeriod = mLightsOnlyOutsideOfPeriod;
	}

	public String getmAppName()
	{
		return mAppName;
	}

	public void setmAppName(String mAppName)
	{
		this.mAppName = mAppName;
	}
}
