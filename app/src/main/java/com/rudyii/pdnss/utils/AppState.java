package com.rudyii.pdnss.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class AppState {
    private final MutableLiveData<Long> pdnsState = new MutableLiveData<>();

    public LiveData<Long> getPdnsState() {
        return pdnsState;
    }

    public void updatePdnsState(Long trigger) {
        pdnsState.postValue(trigger);
    }
}
