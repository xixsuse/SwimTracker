package com.example.dave.swimtracker.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.example.dave.swimtracker.SetComponentEditor;
import com.example.dave.swimtracker.Stroke;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity (indices = {@Index("setId")},
        foreignKeys = @ForeignKey(entity = dbSet.class, parentColumns = "id", childColumns = "setId", onDelete = CASCADE))
public class dbSetComponent {

    @PrimaryKey (autoGenerate = true)
    private long id;
    private long setId;
    private int intervalType;
    private float baseInterval;
    private float deltaInterval;
    private int reps;
    private int distance;
    @TypeConverters(StrokeConverter.class)
    private Stroke stroke;
    private int type;

    public dbSetComponent(long setId, int intervalType, float baseInterval, float deltaInterval) {
        this.setId = setId;
        this.intervalType = intervalType;
        this.baseInterval = baseInterval;
        this.deltaInterval = deltaInterval;
        this.reps = 0;
        this.distance = 0;
        this.stroke = SetComponentEditor.DEFAULT_STROKE;
        this.type = SetComponentEditor.DEFAULT_TYPE;
    }

    public dbSetComponent(long setId, dbSetComponent copySetComponent) {
        this.setId = setId;
        this.intervalType = copySetComponent.intervalType;
        this.baseInterval = copySetComponent.baseInterval;
        this.deltaInterval = copySetComponent.deltaInterval;
        this.reps = copySetComponent.reps;
        this.distance = copySetComponent.distance;
        this.stroke = copySetComponent.stroke;
        this.type = copySetComponent.type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSetId() {
        return setId;
    }

    public void setSetId(long setId) {
        this.setId = setId;
    }

    public int getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(int intervalType) {
        this.intervalType = intervalType;
    }

    public float getBaseInterval() {
        return baseInterval;
    }

    public void setBaseInterval(float baseInterval) {
        this.baseInterval = baseInterval;
    }

    public float getDeltaInterval() {
        return deltaInterval;
    }

    public void setDeltaInterval(float deltaInterval) {
        this.deltaInterval = deltaInterval;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String intervalToString() {
        StringBuilder interval = new StringBuilder();
        switch (this.intervalType) {
            case SetComponentEditor.TIME_INTERVAL:
                interval.append(String.format("%2d:%02d", getMin(getBaseInterval()), getSeconds(getBaseInterval())));
                break;
            case SetComponentEditor.REST_INTERVAL:
                interval.append(String.format("%2d:%02d rest", getMin(getBaseInterval()), getSeconds(getBaseInterval())));
                break;
            case SetComponentEditor.TOTAL_INTERVAL:
                interval.append(String.format("%2d:%02d total", getMin(getBaseInterval()), getSeconds(getBaseInterval())));
                break;
            case SetComponentEditor.NO_INTERVAL:
                interval.append("No Interval");
                break;
            default:
                interval.append("No Interval");
                break;
        }
        return interval.toString();
    }

    public String deltaToString() {
        StringBuilder interval = new StringBuilder();
        if (this.deltaInterval < 0)
            interval.append(String.format("desc. by %2d:%02d", getMin(Math.abs(deltaInterval)), getSeconds(Math.abs(deltaInterval))));
        else if (this.deltaInterval > 0)
            interval.append(String.format("asc. by %2d:%02d", getMin(deltaInterval), getSeconds(deltaInterval)));
        return interval.toString();
    }

    private static int getMin(float seconds) {
        return (int) seconds / 60;
    }

    private int getSeconds(float seconds) {
        return (int) seconds % 60;
    }

    private String typeToString() {
        switch(type) {
            case SetComponentEditor.TYPE_MIXED:
                return "";
            case SetComponentEditor.TYPE_SWIM:
                return "";
            case SetComponentEditor.TYPE_KICK:
                return "Kick";
            case SetComponentEditor.TYPE_DRILL:
                return "Drill";
            default:
                return "";
        }
    }

    public String toString() {
        if (this.stroke == Stroke.MIXED)
            return this.reps + " x " + this.distance + " " + this.typeToString();
        else
            return this.reps + " x " + this.distance + " " + this.stroke + " " + this.typeToString();
    }
}