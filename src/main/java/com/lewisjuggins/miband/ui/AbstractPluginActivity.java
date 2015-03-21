package com.lewisjuggins.miband.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.lewisjuggins.miband.Constants;
import com.twofortyfouram.locale.BreadCrumber;
import com.lewisjuggins.miband.Constants;
import com.lewisjuggins.miband.R;

public abstract class AbstractPluginActivity extends Activity
{
    private boolean mIsCancelled = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            setupTitleApi11();
        }
        else
        {
            setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(),  getString(R.string.app_name)));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupTitleApi11()
    {
        CharSequence callingApplicationLabel = null;
        try
        {
            callingApplicationLabel =
                    getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getCallingPackage(),
                                                                                                   0));
        }
        catch (final NameNotFoundException e)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, "Calling package couldn't be found", e); //$NON-NLS-1$
            }
        }
        if (null != callingApplicationLabel)
        {
            setTitle(callingApplicationLabel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.twofortyfouram_locale_help_save_dontsave, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            setupActionBarApi11();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            setupActionBarApi14();
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBarApi11()
    {
        getActionBar().setSubtitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(), getString(R.string.app_name)));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setupActionBarApi14()
    {
        getActionBar().setDisplayHomeAsUpEnabled(true);

        try
        {
            getActionBar().setIcon(getPackageManager().getApplicationIcon(getCallingPackage()));
        }
        catch (final NameNotFoundException e)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.w(Constants.LOG_TAG, "An error occurred loading the host's icon", e); //$NON-NLS-1$
            }
        }
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item)
    {
        final int id = item.getItemId();

        if (android.R.id.home == id)
        {
            finish();
            return true;
        }
        else if (R.id.twofortyfouram_locale_menu_dontsave == id)
        {
            mIsCancelled = true;
            finish();
            return true;
        }
        else if (R.id.twofortyfouram_locale_menu_save == id)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected boolean isCanceled()
    {
        return mIsCancelled;
    }
}
