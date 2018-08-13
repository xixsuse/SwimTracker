package com.dsfstudios.apps.lappr.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import java.sql.Time;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity (indices = {@Index("setComponentId")},
        foreignKeys = @ForeignKey(entity = dbSetComponent.class, parentColumns = "id", childColumns = "setComponentId", onDelete = CASCADE))
public class dbRep {

    @PrimaryKey (autoGenerate = true)
    private long id;
    private long setComponentId;
    private int pace;
    //public Time interval; // need TypeConverter?

    public dbRep(long setComponentId, int pace) {
        this.setComponentId = setComponentId;
        this.pace = pace;
        //this.interval = interval;
    }

    public dbRep(long setComponentId, dbRep copyRep) {
        this.setComponentId = setComponentId;
        this.pace = copyRep.pace;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSetComponentId() {
        return setComponentId;
    }

    public void setSetComponentId(long setComponentId) {
        this.setComponentId = setComponentId;
    }

    public int getPace() {
        return pace;
    }

    public void setPace(int pace) {
        this.pace = pace;
    }
}
