package com.firebirdberlin.nightdream.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.viewmodels.HueViewModel;

public class HueConnectPreferenceFragmentCompat extends PreferenceDialogFragmentCompat {
    private static final String TAG = "HueConnectPrefFragmentC";
    private static String bridgeIP;
    private boolean isDismissible = false;

    public static HueConnectPreferenceFragmentCompat newInstance(String key, String bridge_IP) {
        final HueConnectPreferenceFragmentCompat
                fragment = new HueConnectPreferenceFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        bridgeIP = bridge_IP;

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "ppt onCreateView");

        HueViewModel.observeKey(getContext(), getViewLifecycleOwner(), bridgeIP, Key -> {

            Log.d(TAG, "ppt found Key: " + Key);

            if (!Key.isEmpty()) {
                setFragmentResult(Key);
                dismissAllowingStateLoss();
            }
            else {
                Log.d(TAG, "ppt Key empty");
            }

        });

        View view = inflater.inflate(R.layout.hue_connect_preferences, container, false);
        return view;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        Log.d(TAG, "ppt onDialogClosed: "+positiveResult);
        HueViewModel.stopExecutor();
    }

    @Override
    public void dismiss() {
        Log.d(TAG, "ppt dismiss()");
        try {
            isDismissible = true;
            super.dismiss();
            Log.d(TAG, "ppt Dialog dismissed!");
        } catch (IllegalStateException e) {
            Log.d(TAG, "ppt dismiss() error: "+e.toString());
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "ppt onDismiss()");
        if (isDismissible) {
            super.onDismiss(dialog);
        }
    }

    private void setFragmentResult(String hueKey){
        if (isAdded()) {
            Bundle result = new Bundle();
            result.putString("hueKey", hueKey);
            getParentFragmentManager().setFragmentResult("hueKey", result);
        }
    }

}
