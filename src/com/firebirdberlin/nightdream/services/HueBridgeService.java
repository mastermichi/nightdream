package com.firebirdberlin.nightdream.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.firebirdberlin.openweathermapapi.models.WeatherEntry;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.github.zeroone3010.yahueapi.HueBridge;
import io.github.zeroone3010.yahueapi.discovery.HueBridgeDiscoveryService;

public class HueBridgeService extends Worker {
    private static final String TAG = "HueService";
    public static MutableLiveData<String> hueIP = new MutableLiveData<>();

    public HueBridgeService(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork()");

        Future<List<HueBridge>> bridgesFuture = new HueBridgeDiscoveryService()
                .discoverBridges(bridge -> Log.d(TAG, "ppt Bridge found: " + bridge));
        final List<HueBridge> bridges;
        try {
            bridges = bridgesFuture.get();
            if (!bridges.isEmpty()) {
                final String bridgeIp = bridges.get(0).getIp();
                Log.d(TAG, "ppt Bridge found at " + bridgeIp);
                // Then follow the code snippets below under the "Once you have a Bridge IP address" header
                hueIP.postValue(bridgeIp);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
            return Result.failure();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Result.failure();
        }
        return Result.success();
    }
}
