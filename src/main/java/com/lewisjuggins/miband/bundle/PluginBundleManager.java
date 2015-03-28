package com.lewisjuggins.miband.bundle;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.lewisjuggins.miband.Constants;

public final class PluginBundleManager
{
    public static final String BUNDLE_EXTRA_VIBRATION = "com.lewisjuggins.miband.extra.VIBRATION";
    public static final String BUNDLE_EXTRA_FLASH = "com.lewisjuggins.miband.extra.FLASH";
    public static final String BUNDLE_EXTRA_FLASH_COLOR = "com.lewisjuggins.miband.extra.FLASH_COLOR";
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE = "com.lewisjuggins.miband.extra.INT_VERSION_CODE";
    public static boolean isBundleValid(final Bundle bundle) {
        if (null == bundle){
            return false;
        }
        if (!bundle.containsKey(BUNDLE_EXTRA_VIBRATION)){
            return false;
        }
        if (!bundle.containsKey(BUNDLE_EXTRA_FLASH)){
            return false;
        }
        if (!bundle.containsKey(BUNDLE_EXTRA_FLASH_COLOR)){
            return false;
        }
        if (!bundle.containsKey(BUNDLE_EXTRA_INT_VERSION_CODE)){
            return false;
        }

        if (4 != bundle.keySet().size()){
            return false;
        }

        if (bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 0) != bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1)) {
            return false;
        }

        return true;
    }

    public static Bundle generateBundle(final Context context, final int vibration, final int flash, final int flashColor)
    {
        final Bundle result = new Bundle();
        result.putInt(BUNDLE_EXTRA_INT_VERSION_CODE, Constants.getVersionCode(context));
        result.putInt(BUNDLE_EXTRA_VIBRATION, vibration);
        result.putInt(BUNDLE_EXTRA_FLASH, flash);
        result.putInt(BUNDLE_EXTRA_FLASH_COLOR, flashColor);
        return result;
    }

    private PluginBundleManager()
    {
        throw new UnsupportedOperationException("This class is non-instantiable");
    }
}