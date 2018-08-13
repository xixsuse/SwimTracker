package com.dsfstudios.apps.lappr;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

public enum Stroke {
    // a lap must have one of the 4 strokes or choice or rest, sets/reps can have others
    FREESTYLE("Freestyle"), BACKSTROKE("Backstroke"), BREASTSTROKE("Breaststroke"), BUTTERFLY("Butterfly"), CHOICE("Choice"),
    IM("IM"), REVERSE_IM("Reverse IM"), IM_ORDER("IM Order"), MIXED("Mixed"), REST("Rest");

    private String stroke;

    Stroke(String stroke) {
        this.stroke = stroke;
    }

    public String stroke() {
        return this.stroke;
    }

    public Stroke nextStroke() {
        switch(this) {
            case FREESTYLE:
                return BACKSTROKE;
            case BACKSTROKE:
                return BREASTSTROKE;
            case BREASTSTROKE:
                return BUTTERFLY;
            case BUTTERFLY:
                return CHOICE;
            case CHOICE:
                return FREESTYLE;
        }
        return CHOICE;
    }

    public String strokeAbbrev() {
        switch(this) {
            case FREESTYLE:
                return "FR";
            case BACKSTROKE:
                return "BA";
            case BREASTSTROKE:
                return "BR";
            case BUTTERFLY:
                return "FLY";
            case CHOICE:
                return "CH";
        }
        return "**";
    }

    @Override
    public String toString() {
        return this.stroke;
    }
}
