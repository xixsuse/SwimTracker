package com.dsfstudios.apps.lappr;

import android.arch.lifecycle.MutableLiveData;
import android.databinding.BaseObservable;
import android.databinding.Observable;

public class CustomMutableLiveData<T extends BaseObservable> extends MutableLiveData<T> {
    @Override
    public void setValue(T value) {
        super.setValue(value);

        //listen to property changes
        value.addOnPropertyChangedCallback(callback);
    }

    Observable.OnPropertyChangedCallback callback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {

            //Trigger LiveData observer on change of any property in object
            setValue(getValue());

        }
    };
}
