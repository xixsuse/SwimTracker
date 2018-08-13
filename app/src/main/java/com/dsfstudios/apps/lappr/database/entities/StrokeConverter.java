package com.dsfstudios.apps.lappr.database.entities;

import android.arch.persistence.room.TypeConverter;

import com.dsfstudios.apps.lappr.Stroke;

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
