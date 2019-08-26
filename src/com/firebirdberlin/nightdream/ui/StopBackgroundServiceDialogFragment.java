package com.firebirdberlin.nightdream.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;
import com.firebirdberlin.nightdream.services.ScreenWatcherService;
import com.firebirdberlin.nightdream.widget.ClockWidgetProvider;

public class StopBackgroundServiceDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //return super.onCreateDialog(savedInstanceState);

        String message = getString(R.string.backgroundServiceDialogMessage);
        if (ClockWidgetProvider.hasWidgets(getActivity().getApplicationContext())) {
            String messagePt2 = getString(R.string.backgroundServiceDialogMessageWidgets);
            message += "\n\n" + messagePt2;
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.DialogTheme);
        builder.setTitle(R.string.backgroundServiceDialogTitle)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Settings settings = new Settings(getActivity());
                        settings.disableSettingsNeedingBackgroundService();
                        ScreenWatcherService.stop(getActivity());


                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        return builder.create();
    }
}
