package com.example.dave.swimtracker.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.example.dave.swimtracker.database.AppDatabase;

public class WorkoutViewModelFactory implements ViewModelProvider.Factory {

    private final AppDatabase db;

    public WorkoutViewModelFactory(AppDatabase db) {
        this.db = db;
    }

    @NonNull
    @Override
    public WorkoutViewModel create(Class modelClass) {
        return new WorkoutViewModel(this.db);
    }
}
