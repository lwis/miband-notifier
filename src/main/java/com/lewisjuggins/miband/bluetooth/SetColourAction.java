package com.lewisjuggins.miband.bluetooth;

import com.lewisjuggins.miband.MiBandConstants;

/**
 * Created by Lewis on 11/01/15.
 */
public class SetColourAction extends WriteAction
{
	public SetColourAction(final int rgb, final boolean display)
	{
		super(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{});
		final byte[] converted = convertRgb(rgb);
		payload = new byte[]{14, converted[0], converted[1], converted[2], display ? (byte)1 : 0};
	}

	private byte[] convertRgb(final int rgb)
	{
		final int red = ((rgb >> 16) & 0x0ff) / 42;
		final int green = ((rgb >> 8) & 0x0ff) / 42;
		final int blue = ((rgb) & 0x0ff) / 42;

		return new byte[]{ (byte) red, (byte) green, (byte) blue };
	}
}
