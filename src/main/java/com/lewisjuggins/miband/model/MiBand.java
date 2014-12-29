package com.lewisjuggins.miband.model;

import java.util.Observable;

public class MiBand extends Observable
{
	public static final MiBand INSTANCE = new MiBand();

	public String mBTAddress, mColour;
	private boolean connected;

	private MiBand()
	{

	}

	public static MiBand getInstance()
	{
		return INSTANCE;
	}

	public boolean isConnected()
	{
		return connected;
	}

	public void setConnected(boolean connected)
	{
		this.connected = connected;
		setChanged();
		notifyObservers(connected);
	}
}
