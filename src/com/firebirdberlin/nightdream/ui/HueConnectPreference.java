package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.firebirdberlin.nightdream.R;

public class HueConnectPreference extends DialogPreference {

    private final int mDialogLayoutResId = R.layout.hue_connect_preferences;

    public HueConnectPreference(Context context) {
        this(context, null);
    }

    public HueConnectPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public HueConnectPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public HueConnectPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // Do custom stuff here, read attributes etc.
        setPositiveButtonText(null);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }
}
