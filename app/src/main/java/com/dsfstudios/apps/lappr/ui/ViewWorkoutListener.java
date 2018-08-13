package com.dsfstudios.apps.lappr.ui;

import com.dsfstudios.apps.lappr.database.entities.dbSetComponent;

public interface ViewWorkoutListener {
    void updateSetComponents();
    void editSetComponent(long setId, long setComponentId);
    void deleteSetComponent(dbSetComponent setComponent);
}
