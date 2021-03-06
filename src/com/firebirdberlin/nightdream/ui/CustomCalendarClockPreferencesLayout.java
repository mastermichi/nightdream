package com.firebirdberlin.nightdream.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.Settings;


public class CustomCalendarClockPreferencesLayout extends LinearLayout {

    private OnConfigChangedListener mListener = null;
    private Settings settings = null;
    private boolean isPurchased = false;
    AppCompatActivity activity = null;

    public CustomCalendarClockPreferencesLayout(
            Context context, Settings settings, AppCompatActivity activity
    ) {
        super(context);
        this.settings = settings;
        this.activity = activity;
        init(context);
    }

    public CustomCalendarClockPreferencesLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(final Context context) {

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View child = inflater.inflate(R.layout.custom_calendar_clock_preferences_layout, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
//        child.setBackgroundResource(R.drawable.border);
        addView(child, lp);

        SeekBar glowRadius = child.findViewById(R.id.glowRadius);
        glowRadius.setProgress(settings.getGlowRadius(ClockLayout.LAYOUT_ID_CALENDAR));
        glowRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                //settings.glowRadius = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                settings.setGlowRadius(progress, ClockLayout.LAYOUT_ID_CALENDAR);
                if (mListener != null) {
                    mListener.onConfigChanged();
                }
            }
        });
        TextView fontButton = child.findViewById(R.id.typeface_preference);
        String fontButtonText = fontButton.getText().toString();
        fontButtonText = String.format(
                "%s: %s", fontButtonText, settings.getFontName(ClockLayout.LAYOUT_ID_CALENDAR)
        );
        fontButton.setText(fontButtonText);
        fontButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activity == null) {
                    return;
                }

                FragmentManager fm = activity.getSupportFragmentManager();
                ManageFontsDialogFragment dialog = new ManageFontsDialogFragment();
                dialog.setIsPurchased(isPurchased);
                dialog.setSelectedUri(settings.getFontUri(ClockLayout.LAYOUT_ID_CALENDAR));
                dialog.setDefaultFonts(
                        "roboto_regular.ttf", "roboto_light.ttf",
                        "roboto_thin.ttf", "7_segment_digital.ttf", "dseg14classic.ttf",
                        "dancingscript_regular.ttf"
                );
                dialog.setOnFontSelectedListener(new ManageFontsDialogFragment.ManageFontsDialogListener() {
                    @Override
                    public void onFontSelected(Uri uri, String name) {
                        settings.setFontUri(uri.toString(), name, ClockLayout.LAYOUT_ID_CALENDAR);
                        if (mListener != null) {
                            mListener.onConfigChanged();
                        }
                    }

                    @Override
                    public void onPurchaseRequested() {
                        if (mListener != null) {
                            mListener.onPurchaseRequested();
                        }
                    }
                });
                dialog.show(fm, "custom fonts");
            }
        });

        final TextView decorationStylePreference = child.findViewById(R.id.decoration_preference);

        String[] textures = context.getResources().getStringArray(R.array.textures);
        String title = context.getString(R.string.style);
        decorationStylePreference.setText(
                String.format("%s: %s",
                        title,
                        textures[settings.getTextureId(ClockLayout.LAYOUT_ID_CALENDAR)]
                )
        );
        decorationStylePreference.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.style)
                        .setItems(R.array.textures, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                settings.setTextureId(which, ClockLayout.LAYOUT_ID_CALENDAR);
                                if (mListener != null) {
                                    mListener.onConfigChanged();
                                }
                            }
                        });
                builder.show();
            }
        });

        Switch switchShowSeconds = child.findViewById(R.id.switch_show_seconds);
        switchShowSeconds.setChecked(settings.getShowSeconds(ClockLayout.LAYOUT_ID_CALENDAR));
        switchShowSeconds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                settings.setShowSeconds(isChecked, ClockLayout.LAYOUT_ID_CALENDAR);
                if (mListener != null) {
                    mListener.onConfigChanged();
                }
            }
        });
    }

    public void setIsPurchased(boolean isPurchased) {
        this.isPurchased = isPurchased;
    }

    public void setOnConfigChangedListener(OnConfigChangedListener listener) {
        this.mListener = listener;
    }

    public interface OnConfigChangedListener {
        void onConfigChanged();

        void onPurchaseRequested();
    }
}
