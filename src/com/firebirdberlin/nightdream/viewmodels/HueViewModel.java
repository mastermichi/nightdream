package com.firebirdberlin.nightdream.viewmodels;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.firebirdberlin.nightdream.services.DownloadWeatherService;
import com.firebirdberlin.nightdream.services.HueBridgeService;


public class HueViewModel extends ViewModel {
    private static final String TAG = "HueVieModel";

    private final MutableLiveData<String> hueIPLiveData = new MutableLiveData<>();

    private void findHueBridge(Context context, LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "findHueBridge");
        Constraints constraints =
                new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

        OneTimeWorkRequest getHueBridgeIP =
                new OneTimeWorkRequest.Builder(
                        HueBridgeService.class)
                        .addTag(TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueue(getHueBridgeIP);

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(getHueBridgeIP.getId())
                .observe((LifecycleOwner) context, info -> HueBridgeService.hueIP.observe(
                        lifecycleOwner,
                        String -> {
                            //EDIT: Remove the observer of the worker otherwise
                            //before execution of your below code, the observation might switch
                            Log.d(TAG, "onChanged");
                            WorkManager.getInstance(context).getWorkInfoByIdLiveData(getHueBridgeIP.getId()).removeObservers(lifecycleOwner);
                            hueIPLiveData.setValue(String);
                        }
                ));
    }

    private MutableLiveData<String> getHueIP() {
        return hueIPLiveData;
    }

    public static void observeIP(Context context, @NonNull Observer<String> observer) {
        HueViewModel model = new ViewModelProvider((ViewModelStoreOwner) context).get(HueViewModel.class);
        model.findHueBridge(context, (LifecycleOwner) context);
        model.getHueIP().observe((LifecycleOwner) context, observer);
    }
}
