package com.example.dave.swimtracker.database.entities;

import android.arch.persistence.room.TypeConverter;

import com.example.dave.swimtracker.Stroke;

public class StrokeConverter {

    @TypeConverter
    public String fromStroke(Stroke stroke) {
        return stroke.stroke();
    }

    @TypeConverter
    public Stroke toStroke(String stroke) {
        stroke = stroke.replace(" ", "_");
        return Stroke.valueOf(stroke.toUpperCase());
    }
}
