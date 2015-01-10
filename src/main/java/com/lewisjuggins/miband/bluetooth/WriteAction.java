package com.lewisjuggins.miband.bluetooth;

import java.util.UUID;

/**
 * Created by Lewis on 10/01/15.
 */
public class WriteAction implements BLEAction
{
	private UUID characteristic;

	private byte[] payload;

	public WriteAction(final UUID characteristic, final byte[] payload)
	{
		this.characteristic = characteristic;
		this.payload = payload;
	}

	public UUID getCharacteristic()
	{
		return characteristic;
	}

	public void setCharacteristic(final UUID characteristic)
	{
		this.characteristic = characteristic;
	}

	public byte[] getPayload()
	{
		return payload;
	}

	public void setPayload(final byte[] payload)
	{
		this.payload = payload;
	}

	@Override public void run()
	{
		//Do nothing.
	}
}
