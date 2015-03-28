package com.lewisjuggins.miband.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.lewisjuggins.miband.Constants;
import com.lewisjuggins.miband.R;
import com.lewisjuggins.miband.bundle.BundleScrubber;
import com.lewisjuggins.miband.bundle.PluginBundleManager;
import com.lewisjuggins.miband.colorpicker.ColorPickerDialog;

public final class EditActivity extends AbstractPluginActivity {

    private static int color;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BundleScrubber.scrub(getIntent());

        final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(localeBundle);

        setContentView(R.layout.activity_tasker_main);

        if (null == savedInstanceState) {
            if (PluginBundleManager.isBundleValid(localeBundle)) {
                final int vibration = localeBundle.getInt(PluginBundleManager.BUNDLE_EXTRA_VIBRATION);
                ((SeekBar) findViewById(R.id.vibrationsSeekBarPlugin)).setProgress(vibration);

                final int flash = localeBundle.getInt(PluginBundleManager.BUNDLE_EXTRA_FLASH);
                ((SeekBar) findViewById(R.id.flashAmountSeekBarPlugin)).setProgress(flash);

                final int flashDuration = localeBundle.getInt(PluginBundleManager.BUNDLE_EXTRA_FLASH_DURATION);
                ((SeekBar) findViewById(R.id.flashDurationSeekBarPlugin)).setProgress(flashDuration);

                final int color = localeBundle.getInt(PluginBundleManager.BUNDLE_EXTRA_FLASH_COLOR);
                EditActivity.color = color;
                Log.i(Constants.LOG_TAG, "" + color);
            }
        }

        ((Button) findViewById(R.id.colourButtonPlugin)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                new ColorPickerDialog(EditActivity.this, EditActivity.color, new ColorPickerDialog.OnColorSelectedListener() {
                    @Override public void onColorSelected(int rgb) {
                        Log.i(Constants.LOG_TAG, "" + rgb);
                        EditActivity.color = rgb;
                    }
                }).show();
            }
        });
    }

    @Override
    public void finish()
    {
        if (!isCanceled())
        {
            final int vibration = ((SeekBar) findViewById(R.id.vibrationsSeekBarPlugin)).getProgress();
            final int flash = ((SeekBar) findViewById(R.id.flashAmountSeekBarPlugin)).getProgress();
            final int flashDuration = ((SeekBar) findViewById(R.id.flashDurationSeekBarPlugin)).getProgress();

            final Intent resultIntent = new Intent();
            final Bundle resultBundle = PluginBundleManager.generateBundle(getApplicationContext(),vibration, flash, flashDuration, EditActivity.color);
            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);

            int r = (EditActivity.color >> 16) & 0xFF;
            int g = (EditActivity.color >> 8) & 0xFF;
            int b = (EditActivity.color >> 0) & 0xFF;

            final String blurb = getResources().getText(R.string.vibrations)+": "+vibration+"\n"+
                    getResources().getText(R.string.flash)+": "+flash+"\n"+
                    getResources().getText(R.string.duration)+": "+flashDuration+"\n"+
                    getResources().getText(R.string.colour)+": "+String.format("#%02x%02x%02x", r, g, b)+"\n";

            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);
            setResult(RESULT_OK, resultIntent);
        }

        super.finish();
    }

    private static String decToHex(int dec) {
        char[] hexDigits = {
                '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder hexBuilder = new StringBuilder(8);
        hexBuilder.setLength(8);
        for (int i = 7; i >= 0; --i)
        {
            int j = dec & 0x0F;
            hexBuilder.setCharAt(i, hexDigits[j]);
            dec >>= 4;
        }
        return hexBuilder.toString();
    }

}