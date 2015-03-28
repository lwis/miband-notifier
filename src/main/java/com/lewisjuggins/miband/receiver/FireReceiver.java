/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.lewisjuggins.miband.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.lewisjuggins.miband.BLECommunicationManager;
import com.lewisjuggins.miband.Constants;
import com.lewisjuggins.miband.MiBandConstants;
import com.lewisjuggins.miband.NotificationService;
import com.lewisjuggins.miband.bluetooth.BLEAction;
import com.lewisjuggins.miband.bluetooth.BLETask;
import com.lewisjuggins.miband.bluetooth.WaitAction;
import com.lewisjuggins.miband.bluetooth.WriteAction;
import com.lewisjuggins.miband.bundle.BundleScrubber;
import com.lewisjuggins.miband.bundle.PluginBundleManager;
import com.lewisjuggins.miband.preferences.UserPreferences;
import com.lewisjuggins.miband.ui.EditActivity;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FireReceiver extends BroadcastReceiver
{
    private static final WriteAction startVibrate = new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ (byte) 8, (byte) 2 });
    private BLECommunicationManager mBLEComms;

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction()))
        {
            return;
        }
        BundleScrubber.scrub(intent);
        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);
        if (PluginBundleManager.isBundleValid(bundle))
        {
            try {
                mBLEComms = new BLECommunicationManager(context);
            }
            catch(NullPointerException e){
                Log.d(Constants.LOG_TAG, "No Bluetooth device available");
            }

            final int vibrateTimes = bundle.getInt(PluginBundleManager.BUNDLE_EXTRA_VIBRATION);
            final int flashTimes = bundle.getInt(PluginBundleManager.BUNDLE_EXTRA_FLASH);
            final long flashDuration =  bundle.getInt(PluginBundleManager.BUNDLE_EXTRA_FLASH_DURATION);
            final int flashColour = bundle.getInt(PluginBundleManager.BUNDLE_EXTRA_FLASH_COLOR);
            int originalColour = 0;

            try
            {
                UserPreferences.loadPreferences(context.getApplicationContext().openFileInput(UserPreferences.FILE_NAME));
                UserPreferences userPreferences = UserPreferences.getInstance();
                originalColour = userPreferences.getmBandColour();
            }
            catch(FileNotFoundException e)
            {
                originalColour = bundle.getInt(PluginBundleManager.BUNDLE_EXTRA_FLASH_COLOR);
            }
            notifyBand(vibrateTimes, flashTimes, flashColour, originalColour, flashDuration);

        }
    }

    private byte[] convertRgb(int rgb)
    {
        final int red = ((rgb >> 16) & 0x0ff) / 42;
        final int green = ((rgb >> 8) & 0x0ff) / 42;
        final int blue = ((rgb) & 0x0ff) / 42;

        return new byte[]{ (byte) red, (byte) green, (byte) blue };
    }

    private void notifyBand(int vibrateTimes, int flashTimes, int flashColour, int originalColour, long flashDuration)
    {
        final List<BLEAction> list = new ArrayList<>();

        final byte[] flashColours = convertRgb(flashColour);
        final byte[] originalColours = convertRgb(originalColour);

        for(int i = 1; i <= vibrateTimes; i++)
        {
            list.add(startVibrate);
        }
        for(int i = 1; i <= flashTimes; i++)
        {
            list.add(new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ 14, flashColours[0], flashColours[1], flashColours[2], (byte) 1 }));
            list.add(new WaitAction(500L));
            list.add(new WriteAction(MiBandConstants.UUID_CHARACTERISTIC_CONTROL_POINT, new byte[]{ 14, originalColours[0], originalColours[1], originalColours[2], (byte) 0 }));
            list.add(new WaitAction(500L));
        }

        final BLETask task = new BLETask(list);

        try
        {
            mBLEComms.queueTask(task);
        }
        catch(NullPointerException ignored)
        {

        }
    }
}