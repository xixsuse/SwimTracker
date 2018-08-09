package com.example.dave.swimtracker.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class dbSwimmer {

    private static final int POOL_SCY = 0;
    private static final int POOL_SCM = 1;
    private static final int POOL_LCM = 2;

    public static final int DEFAULT_UPCOMING_WORKOUTS = 3;
    public static final int DEFAULT_RECENT_WORKOUTS = 3;

    @PrimaryKey (autoGenerate = true)
    private long id;
    private String firebaseUid;
    private String name;
    private int numUpcomingWorkouts;
    private int numRecentWorkouts;
    private int lapLength;

    public dbSwimmer(String firebaseUid, String name, int numUpcomingWorkouts, int numRecentWorkouts, int lapLength) {
        this.firebaseUid = firebaseUid;
        this.name = name;
        this.numUpcomingWorkouts = 1;
        this.numRecentWorkouts = 3;
        this.lapLength = POOL_SCY;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumUpcomingWorkouts() {
        return numUpcomingWorkouts;
    }

    public void setNumUpcomingWorkouts(int numUpcomingWorkouts) {
        this.numUpcomingWorkouts = numUpcomingWorkouts;
    }

    public int getNumRecentWorkouts() {
        return numRecentWorkouts;
    }

    public void setNumRecentWorkouts(int numRecentWorkouts) {
        this.numRecentWorkouts = numRecentWorkouts;
    }

    public int getLapLength() {
        return lapLength;
    }

    public void setLapLength(int lapLength) {
        this.lapLength = lapLength;
    }

}
