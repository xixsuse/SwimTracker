package com.example.dave.swimtracker.database.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.example.dave.swimtracker.BR;

import java.text.SimpleDateFormat;
import java.util.Observable;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity (indices = {@Index("swimmerId")},
        foreignKeys = @ForeignKey(entity = dbSwimmer.class, parentColumns = "id", childColumns = "swimmerId", onDelete = CASCADE))
public class dbWorkout extends BaseObservable {

    @PrimaryKey (autoGenerate = true)
    private long id;
    private long swimmerId;
    private long date;
    private boolean completed;
    private String notes;

    public dbWorkout(long swimmerId, long date, boolean completed) {
        this.swimmerId = swimmerId;
        this.date = date;
        this.completed = completed;
        this.notes = "";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSwimmerId() {
        return swimmerId;
    }

    public void setSwimmerId(long swimmerId) {
        this.swimmerId = swimmerId;
    }

    @Bindable
    public long getDate() {
        return date;
    }

    public String getDate(String format) {
        return new SimpleDateFormat(format).format(date);
    }

    public void setDate(long date) {
        this.date = date;
        notifyPropertyChanged(BR.date);
    }

    @Bindable
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        notifyPropertyChanged(BR.completed);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
