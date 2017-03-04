package com.firebirdberlin.nightdream;

import java.util.Calendar;
import java.lang.IllegalArgumentException;

import de.greenrobot.event.EventBus;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebirdberlin.nightdream.Config;
import com.firebirdberlin.nightdream.R;
import com.firebirdberlin.nightdream.events.OnClockClicked;
import com.firebirdberlin.nightdream.events.OnLightSensorValueTimeout;
import com.firebirdberlin.nightdream.events.OnNewAmbientNoiseValue;
import com.firebirdberlin.nightdream.events.OnNewLightSensorValue;
import com.firebirdberlin.nightdream.events.OnPowerConnected;
import com.firebirdberlin.nightdream.events.OnPowerDisconnected;
import com.firebirdberlin.nightdream.models.SimpleTime;
import com.firebirdberlin.nightdream.models.BatteryValue;
import com.firebirdberlin.nightdream.services.AlarmService;
import com.firebirdberlin.nightdream.services.RadioStreamService;
import com.firebirdberlin.nightdream.ui.NightDreamUI;
import com.firebirdberlin.nightdream.repositories.BatteryStats;


public class NightDreamActivity extends Activity implements View.OnTouchListener {
    public static String TAG ="NightDreamActivity";
    private static int PENDING_INTENT_STOP_APP = 1;
    final private Handler handler = new Handler();
    AlarmClock alarmClock;
    ImageView background_image;

    Sensor lightSensor = null;
    int mode = 2;
    int currentRingerMode;
    mAudioManager AudioManage = null;

    private float last_ambient = 4.0f;
    private double last_ambient_noise = 32000; // something loud

    private NightDreamUI nightDreamUI = null;
    private Utility utility;
    private NotificationReceiver nReceiver = null;
    private ReceiverShutDown shutDownReceiver = null;
    private ReceiverRadioStream receiverRadioStream = null;
    private PowerManager pm;

    private double NOISE_AMPLITUDE_WAKE  = Config.NOISE_AMPLITUDE_WAKE;
    private double NOISE_AMPLITUDE_SLEEP = Config.NOISE_AMPLITUDE_SLEEP;

    private Settings mySettings = null;
    private boolean isChargingWireless = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.i(TAG, "onCreate()");
        Window window = getWindow();

        nightDreamUI = new NightDreamUI(this, window);
        utility = new Utility(this);
        AudioManage = new mAudioManager(this);
        mySettings = new Settings(this);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // allow the app to be displayed above the keyguard
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setKeepScreenOn(true);

        background_image = (ImageView) findViewById(R.id.background_view);
        background_image.setOnTouchListener(this);

        alarmClock = (AlarmClock) findViewById(R.id.AlarmClock);
        alarmClock.setSettings(mySettings);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setKeepScreenOn(true);
        Log.i(TAG, "onStart()");
        EventBus.getDefault().register(this);

        nightDreamUI.onStart();

        lightSensor = Utility.getLightSensor(this);
        if ( lightSensor == null ){
            last_ambient = 400.0f;
        }

        BatteryValue batteryValue = new BatteryStats(this).reference;
        this.isChargingWireless = batteryValue.isChargingWireless;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        setKeepScreenOn(true);
        mySettings = new Settings(this);
        alarmClock.setSettings(mySettings);

        scheduleShutdown();
        nightDreamUI.onResume();
        nReceiver = registerNotificationReceiver();
        shutDownReceiver = registerPowerDisconnectionReceiver();
        receiverRadioStream = registerReceiverRadioStream();

        if (Build.VERSION.SDK_INT >= 18){
            // ask for active notifications
            Intent i = new Intent(Config.ACTION_NOTIFICATION_LISTENER);
            i.putExtra("command", "list");
            sendBroadcast(i);
        }

        NOISE_AMPLITUDE_SLEEP *= mySettings.sensitivity;
        NOISE_AMPLITUDE_WAKE  *= mySettings.sensitivity;

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.i(TAG, "Intent has extras");
            String action = extras.getString("action");

            if (action != null) {
                Log.i(TAG, "action: " + action);


                if (action.equals("start night mode")) {
                    last_ambient = mySettings.minIlluminance;
                    last_ambient_noise = 32000;
                    nightDreamUI.dimScreen(0, last_ambient, mySettings.dim_offset);
                    if ( lightSensor == null ) {
                        handler.postDelayed(setScreenOff, 20000);
                    }
                }
            }
        }

        if ( AlarmService.isRunning ) {
            alarmClock.startAlarm();
            nightDreamUI.showAlarmClock();
        }

        setupRadioStreamUI();
    }

    void setupRadioStreamUI() {
        switch (RadioStreamService.streamingMode) {
            case ALARM:
                setVolumeControlStream(AudioManager.STREAM_ALARM);
                nightDreamUI.showAlarmClock();
                nightDreamUI.setRadioIconInactive();
                break;
            case RADIO:
                setVolumeControlStream(AudioManager.STREAM_MUSIC);
                nightDreamUI.setRadioIconActive();
                break;
            default:
                setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
                nightDreamUI.setRadioIconInactive();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG ,"onPause()");
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        nightDreamUI.onPause();

        handler.removeCallbacks(finishApp);
        PowerConnectionReceiver.schedule(this);
        cancelShutdown();
        unregister(nReceiver);
        unregister(shutDownReceiver);
        unregister(receiverRadioStream);

        if (mySettings.allow_screen_off && mode == 0 && !isScreenOn() ){ // screen off in night mode
            startBackgroundListener();
        } else {
            nightDreamUI.restoreRingerMode();
        }
    }

    private void unregister(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch ( IllegalArgumentException e ) {

        }
    }

    protected boolean isScreenOn() {
        if (Build.VERSION.SDK_INT <= 19){
            return deprecatedIsScreenOn();
        }
        return pm.isInteractive();
    }

    @SuppressWarnings("deprecation")
    protected boolean deprecatedIsScreenOn() {
        return pm.isScreenOn();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");

        nightDreamUI.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

        // do not restore ringer mode, otherwise calls will ring
        // after the dream has ended
        //audiomanage.setRingerMode(currentRingerMode);

        utility  = null;
        shutDownReceiver = null;
        nReceiver  = null;
    }

    private NotificationReceiver registerNotificationReceiver() {
        NotificationReceiver receiver = new NotificationReceiver(getWindow());
        IntentFilter filter = new IntentFilter(Config.ACTION_NOTIFICATION_LISTENER);
        registerReceiver(receiver, filter);
        return receiver;
    }

    private ReceiverShutDown registerPowerDisconnectionReceiver() {
        ReceiverShutDown shutDownReceiver = new ReceiverShutDown();
        IntentFilter pwrFilter = new IntentFilter(Config.ACTION_SHUT_DOWN);
        registerReceiver(shutDownReceiver, pwrFilter);
        return shutDownReceiver;
    }

    private ReceiverRadioStream registerReceiverRadioStream() {
        ReceiverRadioStream receiver = new ReceiverRadioStream();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Config.ACTION_RADIO_STREAM_STARTED);
        filter.addAction(Config.ACTION_RADIO_STREAM_STOPPED);
        registerReceiver(receiver, filter);
        return receiver;
    }

    public boolean onTouch(View view, MotionEvent e) {
        return nightDreamUI.onTouch(view, e, last_ambient);
    }

    // click on the settings icon
    public void onSettingsClick(View v) {
        PreferencesActivity.start(this);
    }

    public void onRadioClick(View v) {
        if ( RadioStreamService.streamingMode == RadioStreamService.StreamingMode.ALARM ) {
            alarmClock.stopRadioStream();
        }

        if ( mySettings.radioStreamURLUI.isEmpty() ) {
            PreferencesActivity.start(this, PreferencesActivity.PREFERENCES_SCREEN_WEB_RADIO_INDEX);
            return;
        }

        toggleRadioStreamState();

    }

    private void toggleRadioStreamState() {
        final Context context = this;
        if ( RadioStreamService.streamingMode != RadioStreamService.StreamingMode.RADIO ) {
            if (Utility.hasNetworkConnection(this)) {
                if ( Utility.hasFastNetworkConnection(this) ) {
                    RadioStreamService.startStream(this);
                } else {
                    new AlertDialog.Builder(this, R.style.DialogTheme)
                        .setTitle(R.string.message_mobile_data_connection)
                        .setMessage(R.string.message_mobile_data_connection_confirmation)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                RadioStreamService.startStream(context);
                            }
                        })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(R.drawable.ic_attention)
                        .show();
                }
            } else { // no network connection
                Toast.makeText(context, R.string.message_no_data_connection, Toast.LENGTH_LONG).show();
            }
        } else {
            RadioStreamService.stop(this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        nightDreamUI.onConfigurationChanged(newConfig);
    }


    private void SwitchModes(float light_value, double last_ambient_noise){
        int current_mode = mode;
        mode = nightDreamUI.determineScreenMode(current_mode, light_value, last_ambient_noise);

        nightDreamUI.switchModes(light_value, last_ambient_noise);

        if ((mode == 0) && (current_mode != 0)){
            boolean on = shallKeepScreenOn(mode);
            setKeepScreenOn(on); // allow the screen to go off
        } else if ((mode > 0) && (current_mode != mode)) {
            setKeepScreenOn(true);
        }

    }

    private boolean shallKeepScreenOn(int mode) {
        if (mode > 0 || ! mySettings.allow_screen_off) return true;

        long now = Calendar.getInstance().getTimeInMillis();
        if ( (0 < mySettings.nextAlarmTime - now
                && mySettings.nextAlarmTime - now < 600000)
                || AlarmService.isRunning
                || RadioStreamService.isRunning ) {
            Log.d(TAG, "shallKeepScreenOn() true");
            return true;
        }
        Log.d(TAG, "shallKeepScreenOn() false");
        return false;
    }

    private Runnable setScreenOff = new Runnable() {
       @Override
       public void run() {
            handler.removeCallbacks(setScreenOff);
            SwitchModes(last_ambient, last_ambient_noise);
       }
    };

    private Runnable finishApp = new Runnable() {
       @Override
       public void run() {
           finish();
       }
    };

    private void startBackgroundListener() {
        Intent i = new Intent(this, NightModeListener.class);
        startService(i);

        finish();
    }

    public void onEvent(OnClockClicked event){
        if (AlarmService.isRunning) {
            AlarmService.stop(this);
            alarmClock.stopAlarm();
        }

        // toggle the night mode manually
        if ( lightSensor == null
                || mySettings.nightModeActivationMode == Settings.NIGHT_MODE_ACTIVATION_MANUAL ) {
            last_ambient = ( mode == 0 ) ? 400.f : mySettings.minIlluminance;
            SwitchModes(last_ambient, 0);
        }
    }

    public void onEvent(OnNewLightSensorValue event){
        Log.i(TAG, String.valueOf(event.value) + " lux, n=" + String.valueOf(event.n));
        last_ambient = event.value;
        SwitchModes(last_ambient, last_ambient_noise);
    }

    public void onEvent(OnLightSensorValueTimeout event){
        last_ambient = (event.value >= 0.f) ? event.value : mySettings.minIlluminance;
        Log.i(TAG, "Static for 15s: " + String.valueOf(last_ambient) + " lux.");
        SwitchModes(last_ambient, last_ambient_noise);
    }

    public void onEvent(OnNewAmbientNoiseValue event) {
        last_ambient_noise = event.value;
        SwitchModes(last_ambient, last_ambient_noise);
    }

    public void onEvent(OnPowerDisconnected event) {
        if ( mySettings.handle_power_disconnection ) {
            handler.removeCallbacks(finishApp);
            if ( isChargingWireless ) {
                handler.postDelayed(finishApp, 5000);
            } else {
                finish();
            }
        }
    }

    public void onEvent(OnPowerConnected event) {
        handler.removeCallbacks(finishApp);
        if ( event != null ) {
            isChargingWireless = event.referenceValue.isChargingWireless;
        }
    }

    class ReceiverShutDown extends BroadcastReceiver{
        // this receiver is needed to shutdown the app at the end of the autostart time range
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if ( mySettings.handle_power_disconnection ) {
                finish();
            }
        }
    }

    class ReceiverRadioStream extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            setupRadioStreamUI();
        }
    }


    public void setKeepScreenOn(boolean keepScreenOn) {
        if( keepScreenOn ) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.i(TAG, "new intent received");
        //now getIntent() should always return the last received intent
    }

    static public void start(Context context) {
        NightDreamActivity.start(context, null);
    }

    static public void start(Context context, Bundle extras) {
        Intent intent = new Intent(context, NightDreamActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (extras != null) {
            intent.putExtras(extras);
        }
        context.startActivity(intent);
    }

    private PendingIntent getShutdownIntent() {
        Intent alarmIntent = new Intent(Config.ACTION_SHUT_DOWN);
        return PendingIntent.getBroadcast(this,
                                          PENDING_INTENT_STOP_APP,
                                          alarmIntent,
                                          PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void scheduleShutdown() {
        if (mySettings == null) return;

        Calendar start = new SimpleTime(mySettings.autostartTimeRangeStart).getCalendar();
        Calendar end = new SimpleTime(mySettings.autostartTimeRangeEnd).getCalendar();
        if( start.equals(end)) {
            cancelShutdown();
            return;
        }

        if (PowerConnectionReceiver.shallAutostart(this, mySettings)) {
            PendingIntent pendingIntent = getShutdownIntent();
            Calendar calendar = new SimpleTime(mySettings.autostartTimeRangeEnd).getCalendar();
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 19){
                alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
            } else {
                deprecatedSetAlarm(calendar, pendingIntent);
            }
        } else {
            cancelShutdown();
        }

    }

    @SuppressWarnings("deprecation")
    private void deprecatedSetAlarm(Calendar calendar, PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }

    private void cancelShutdown() {
        PendingIntent pendingIntent = getShutdownIntent();
        pendingIntent.cancel();
    }
}
