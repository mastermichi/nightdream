package com.firebirdberlin.HueApi;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.github.zeroone3010.yahueapi.Hue;

public class HueApiKey extends Worker {
    private static final String TAG = "HueApiKey";
    public static MutableLiveData<String> hueKey = new MutableLiveData<>();

    public HueApiKey(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ppt HueApiKey");
        final String bridgeIp = "192.168.2.144"; // Fill in the IP address of your Bridge
        final String appName = "nightdream"; // Fill in the name of your application
        final CompletableFuture<String> apiKey = Hue.hueBridgeConnectionBuilder(bridgeIp).initializeApiConnection(appName);

        // Push the button on your Hue Bridge to resolve the apiKey future:
        try {
            final String key;
            key = apiKey.get();
            hueKey.postValue(key);
            Log.d(TAG,"ppt Store this API key for future use: " + key);
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.d(TAG,"ppt Error: " + e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG,"ppt Error: " + e.toString());
        }

        return Result.success();
    }

}
