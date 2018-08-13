package com.dsfstudios.apps.lappr.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.databinding.Bindable;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity (indices = {@Index("workoutId")},
        foreignKeys = @ForeignKey(entity = dbWorkout.class, parentColumns = "id", childColumns = "workoutId", onDelete = CASCADE))
public class dbSet {

    public static final int SET_TYPE_WARMUP = 0;
    public static final int SET_TYPE_MAIN = 1;
    public static final int SET_TYPE_COOLDOWN = 2;

    @PrimaryKey (autoGenerate = true)
    private long id;
    private long workoutId;
    private int rounds;
    private int setType;

    public dbSet(long workoutId, int rounds, int setType) {
        this.workoutId = workoutId;
        this.rounds = rounds;
        this.setType = setType;
    }

    public dbSet(long workoutId, dbSet copySet) {
        this.workoutId = workoutId;
        this.rounds = copySet.rounds;
        this.setType = copySet.setType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(long workoutId) {
        this.workoutId = workoutId;
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public int getSetType() {
        return setType;
    }

    public void setSetType(int setType) {
        this.setType = setType;
    }

    public String toString() {
        StringBuilder desc = new StringBuilder();
        desc.append(this.rounds);
        desc.append(" x ");
        switch(setType) {
            case SET_TYPE_WARMUP:
                desc.append("Warmup");
                break;
            case SET_TYPE_MAIN:
                desc.append("Main Set");
                break;
            case SET_TYPE_COOLDOWN:
                desc.append("Cool Down");
                break;
            default:
                desc.append("Set");
                break;
        }
        return desc.toString();
    }
}
