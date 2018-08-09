package com.example.dave.swimtracker.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.example.dave.swimtracker.Stroke;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity (indices = {@Index("repId"), @Index("setComponentId")},
        foreignKeys = @ForeignKey(entity = dbRep.class, parentColumns = "id", childColumns = "repId", onDelete = CASCADE))
public class dbLap {

    @PrimaryKey (autoGenerate = true)
    private long id;
    private long repId;
    private long setComponentId;
    private int distance;
    private int order; // numeric order within the rep
    @TypeConverters(StrokeConverter.class)
    public Stroke stroke;   // need TypeConverter
    private int type;   /* 0 = swim, 1 = kick, 2 = drill */
    private float seconds;

    public dbLap(long repId, long setComponentId, int distance, int order, Stroke stroke, int type, float seconds) {
        this.repId = repId;
        this.setComponentId = setComponentId;
        this.distance = distance;
        this.order = order;
        this.stroke = stroke;
        this.type = type;
        this.seconds = seconds;
    }

    public dbLap(long repId, long setComponentId, dbLap copyLap) {
        this.repId = repId;
        this.setComponentId = setComponentId;
        this.distance = copyLap.distance;
        this.order = copyLap.order;
        this.stroke = copyLap.stroke;
        this.type = copyLap.type;
        this.seconds = copyLap.seconds;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRepId() {
        return repId;
    }

    public void setRepId(long repId) {
        this.repId = repId;
    }

    public long getSetComponentId() {
        return setComponentId;
    }

    public void setSetComponentId(long setComponentId) {
        this.setComponentId = setComponentId;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    public float getSeconds() {
        return seconds;
    }

    public void setSeconds(float seconds) {
        this.seconds = seconds;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
