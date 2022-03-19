package com.firebirdberlin.nightdream.viewmodels;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.firebirdberlin.nightdream.Utility;
import com.firebirdberlin.nightdream.hueapi.HueBridgeSearch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.zeroone3010.yahueapi.Hue;


public class HueViewModel extends ViewModel {
    private static final String TAG = "HueViewModel";

    private final MutableLiveData<String> hueIPLiveData = new MutableLiveData<>();
    private static MutableLiveData<String> hueKeyLiveData = new MutableLiveData<>();
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    private void findHueBridge(Context context, LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "ppt findHueBridge");

        //return no LAN
        if (!Utility.hasNetworkConnection(context)) {
            Log.d(TAG, "!hasNetworkConnection");
            hueIPLiveData.postValue("Error:No Network connection");
            return;
        }

        //return IP already found
        if (hueIPLiveData.getValue() != null && !hueIPLiveData.getValue().isEmpty() && !hueIPLiveData.getValue().contains("Error")) {
            return;
        }

        Constraints constraints =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

        OneTimeWorkRequest getHueBridgeIP =
                new OneTimeWorkRequest.Builder(
                        HueBridgeSearch.class)
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager manager = WorkManager.getInstance(context);
        manager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, getHueBridgeIP);

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(getHueBridgeIP.getId())
                .observe(lifecycleOwner, info -> {
                    if (info != null && info.getState().isFinished()) {
                        Log.d(TAG, "ppt onChanged");
                        manager.getWorkInfoByIdLiveData(getHueBridgeIP.getId()).removeObservers(lifecycleOwner);
                        String myResult = info.getOutputData().getString("bridgeIP");
                        hueIPLiveData.setValue(myResult);
                    }
                });
    }

    private void findHueKey(Context context, String bridgeIP) {
        Log.d(TAG, "ppt findHueKey");

        if (!Utility.hasNetworkConnection(context)) {
            Log.d(TAG, "ppt: !hasNetworkConnection");
            hueKeyLiveData.setValue("Error:No Network connection");
        } else {
            if (executor.isShutdown()) {
                executor = Executors.newSingleThreadExecutor();
            }

            executor.execute(() -> { //background thread
                Log.d(TAG, "ppt executor");
                final String appName = "nightdream"; // Fill in the name of your application
                final CompletableFuture<String> apiKey = Hue.hueBridgeConnectionBuilder(bridgeIP).initializeApiConnection(appName);
                String key;

                // Push the button on your Hue Bridge to resolve the apiKey future:
                try {
                    key = apiKey.get();
                    Log.d(TAG, "ppt Store this API key for future use: " + key);
                    String finalKey = key;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> { //like onPostExecute()
                        if (!finalKey.isEmpty()) {
                            hueKeyLiveData.postValue(finalKey);
                            Log.d(TAG, "ppt executor after: " + Hue.hueBridgeConnectionBuilder(bridgeIP).initializeApiConnection(appName).isDone());
                        }
                        apiKey.cancel(true);
                        executor.shutdown();
                    });
                } catch (ExecutionException e) {
                    Log.d(TAG, "ppt ExecutionException: " + e.getMessage());
                    handleHueApiException(e.getMessage());
                    apiKey.cancel(true);
                    executor.shutdown();
                } catch (InterruptedException e) {
                    Log.d(TAG, "ppt InterruptedException: " + e.toString());
                    handleHueApiException(e.getMessage());
                    apiKey.cancel(true);
                    executor.shutdown();
                }
            });
        }
    }

    private void handleHueApiException(String exception) {
        if (exception != null) {
            String[] splitStr = exception.split(":");
            Log.d(TAG, "ppt handleHueApiException: " + splitStr[splitStr.length - 1].trim());
            hueKeyLiveData.postValue("Error:" + splitStr[splitStr.length - 1].trim());
        } else {
           // hueKeyLiveData.postValue("Error:unknown");
        }
    }

    private MutableLiveData<String> getHueIP() {
        return hueIPLiveData;
    }

    private MutableLiveData<String> getHueKey() {
        return hueKeyLiveData;
    }

    public static void stopExecutor() {
        Log.d(TAG, "ppt stopExecutor()");
        executor.shutdownNow();
    }

    public static void observeIP(Context context, LifecycleOwner lifecycleOwner, @NonNull Observer<String> observer) {
        HueViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(HueViewModel.class);
        model.findHueBridge(context, lifecycleOwner);
        model.getHueIP().observe(lifecycleOwner, observer);
    }

    public static void observeKey(Context context, LifecycleOwner lifecycleOwner, String BridgeIP, @NonNull Observer<String> observer) {
        hueKeyLiveData = new MutableLiveData<>();
        HueViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(HueViewModel.class);
        model.findHueKey(context, BridgeIP);
        LiveDataUtil.observeOnce(model.getHueKey(), observer);
    }

    public static boolean testHueConnection(String bridgeIP, String apiKey) {
        final Hue hue = new Hue(bridgeIP, apiKey);
        try {
            hue.refresh();
            Log.d(TAG, "ppt testHueConnection true");
            return true;
        } catch (NetworkOnMainThreadException e) {
            Log.d(TAG, "ppt testHueConnection false: " + e);
            return false;

        }
    }

    private static class LiveDataUtil {
        public static <T> void observeOnce(final LiveData<T> liveData, final Observer<T> observer) {
            liveData.observeForever(new Observer<T>() {
                @Override
                public void onChanged(T t) {
                    liveData.removeObserver(this);
                    observer.onChanged(t);
                }
            });
        }
    }

}
