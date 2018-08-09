package com.example.dave.swimtracker.ui;

import com.example.dave.swimtracker.database.entities.dbSetComponent;

public interface ViewWorkoutListener {
    void updateSetComponents();
    void editSetComponent(long setId, long setComponentId);
    void deleteSetComponent(dbSetComponent setComponent);
}
