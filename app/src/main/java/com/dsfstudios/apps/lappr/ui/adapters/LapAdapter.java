package com.dsfstudios.apps.lappr.ui.adapters;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dsfstudios.apps.lappr.R;
import com.dsfstudios.apps.lappr.SetComponentEditor;
import com.dsfstudios.apps.lappr.Stroke;
import com.dsfstudios.apps.lappr.ui.RecordWorkoutDetails.OnWorkoutDetailsListener;

import java.util.ArrayList;
import java.util.List;

public class LapAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final OnWorkoutDetailsListener mListener;
    private final SetComponentEditor mEditor;
    public static final int TYPE_HEADER = -1;
    public static final int TYPE_ITEM = 1;

    public LapAdapter(SetComponentEditor editor, OnWorkoutDetailsListener listener) {
        mEditor = editor;
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_gridlaps, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_gridlaps, parent, false);
            return new ItemViewHolder(view, viewType);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_HEADER;
        else
            return position - 1;
    }

    @Override
    public int getItemCount() {
        return mEditor.getNumReps() + 1;
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final LinearLayout mLayout;
        public final List<TextView> gridItems;

        HeaderViewHolder(View view) {
            super(view);

            mView = view;
            mLayout = view.findViewById(R.id.gridLayout);
            gridItems = new ArrayList<>();

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            // add header item showing rep number
            TextView headerRow = new TextView(view.getContext());
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(width / 10, width / 10, 1));
            mLayout.addView(headerRow);
            headerRow.setText("");
            headerRow.setPadding(10, 10, 10, 10);
            headerRow.setGravity(Gravity.CENTER);
            headerRow.setTextSize(20);
            gridItems.add(headerRow);
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final LinearLayout mLayout;
        public final List<TextView> gridItems;

        ItemViewHolder(View view, final int repNum) {
            super(view);
            mView = view;
            mLayout = view.findViewById(R.id.gridLayout);
            gridItems = new ArrayList<>();

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            // add header item showing rep number
            TextView headerRow = new TextView(view.getContext());
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(width / 10, width / 10, 1));
            mLayout.addView(headerRow);
            headerRow.setText(""+(repNum + 1));
            headerRow.setPadding(10, 10, 10, 10);
            headerRow.setGravity(Gravity.CENTER);
            headerRow.setTextSize(20);
            gridItems.add(headerRow);

            int numLaps = mEditor.getNumLapsForRep(repNum);

            for (int i = 0; i < numLaps; i++) {

                final int lapNum = i;
                final TextView toAdd = new TextView(view.getContext());
                toAdd.setLayoutParams(new LinearLayout.LayoutParams(
                        width / 10,
                        width / 10,
                        1));
                mLayout.addView(toAdd);

                Stroke lapStroke = mEditor.getStrokeForLap(repNum, lapNum);
                toAdd.setText(lapStroke.strokeAbbrev());
                toAdd.setPadding(10, 10, 10, 10);
                toAdd.setGravity(Gravity.CENTER);
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(mListener.getStrokeColors().get(lapStroke));
                gd.setCornerRadius(5);
                switch (mEditor.getTypeForLap(repNum, lapNum)) {
                    case SetComponentEditor.TYPE_SWIM:
                        gd.setStroke(10, Color.BLACK);
                        break;
                    case SetComponentEditor.TYPE_KICK:
                        gd.setStroke(10, Color.RED);
                        break;
                    case SetComponentEditor.TYPE_DRILL:
                        gd.setStroke(10, Color.YELLOW);
                        break;
                }
                toAdd.setBackground(gd);
                toAdd.setTextSize(20);
                toAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Stroke stroke = mEditor.nextStrokeForLap(repNum, lapNum);
                        toAdd.setText(stroke.strokeAbbrev());
                        GradientDrawable newBg = (GradientDrawable) toAdd.getBackground();
                        newBg.setColor(mListener.getStrokeColors().get(stroke));
                        toAdd.setBackground(newBg);
                    }
                });
                toAdd.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        int type = mEditor.nextTypeForLap(repNum, lapNum);
                        GradientDrawable newBg = (GradientDrawable) toAdd.getBackground();

                        switch (type) {
                            case SetComponentEditor.TYPE_SWIM:
                                newBg.setStroke(10, Color.BLACK);
                                break;
                            case SetComponentEditor.TYPE_KICK:
                                newBg.setStroke(10, Color.RED);
                                break;
                            case SetComponentEditor.TYPE_DRILL:
                                newBg.setStroke(10, Color.YELLOW);
                                break;
                        }
                        toAdd.setBackground(newBg);
                        return true;
                    }
                });
                gridItems.add(toAdd);
            }
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
