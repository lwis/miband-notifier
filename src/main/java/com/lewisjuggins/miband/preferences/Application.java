package com.lewisjuggins.miband.preferences;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

	private int mBandColour;

	private Calendar mStartPeriod;

	private Calendar mEndPeriod;

	private boolean mLightsOnlyOutsideOfPeriod;

    private boolean mPriorityModeNone;

	private boolean mPriorityModePriority;

	private transient String mAppName;

	public Application()
	{

	}

	public Application(String packageName)
	{
		this.mPackageName = packageName;

		mStartPeriod = new GregorianCalendar();
		mEndPeriod = new GregorianCalendar();
		mStartPeriod.set(Calendar.HOUR_OF_DAY, 8);
		mStartPeriod.set(Calendar.MINUTE, 0);
		mEndPeriod.set(Calendar.HOUR_OF_DAY, 22);
		mEndPeriod.set(Calendar.MINUTE, 0);
	}

	public Application(String packageName, String applicationLabel)
	{
		this.mPackageName = packageName;
		this.mAppName = applicationLabel;
	}

	public void loadValues(int mVibrateTimes, int mVibrateDuration, int mBandColourTimes, int mBandColourDuration, boolean mLightsOnlyOutsideOfPeriod, boolean mPriorityModeNone, boolean mPriorityModePriority, int mBandColour)
	{
		this.mVibrateTimes = mVibrateTimes;
		this.mVibrateDuration = mVibrateDuration;
		this.mBandColourTimes = mBandColourTimes;
		this.mBandColourDuration = mBandColourDuration;
		this.mLightsOnlyOutsideOfPeriod = mLightsOnlyOutsideOfPeriod;
        this.mPriorityModeNone = mPriorityModeNone;
		this.mPriorityModePriority = mPriorityModePriority;
		this.mBandColour = mBandColour;
	}

	public boolean ismPriorityModeNone()
	{
		return mPriorityModeNone;
	}

	public void setmPriorityModeNone(boolean mPriorityModeNone)
	{
		this.mPriorityModeNone = mPriorityModeNone;
	}

    public boolean ismPriorityModePriority()
    {
        return mPriorityModePriority;
    }

    public void setmPriorityModePriority(boolean mPriorityModePriority)
    {
        this.mPriorityModePriority = mPriorityModePriority;
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
		return mStartPeriod.getTime();
	}

	public Calendar getmStartPeriodCalendar()
	{
		return mStartPeriod;
	}

	public void setmStartPeriod(Calendar mStartPeriod)
	{
		this.mStartPeriod = mStartPeriod;
	}

	public Date getmEndPeriod()
	{
		return mEndPeriod.getTime();
	}

	public Calendar getmEndPeriodCalendar()
	{
		return mEndPeriod;
	}

	public void setmEndPeriod(Calendar mEndPeriod)
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
