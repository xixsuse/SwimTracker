package com.dsfstudios.apps.lappr.ui.adapters;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dsfstudios.apps.lappr.R;
import com.dsfstudios.apps.lappr.database.entities.dbSetComponent;
import com.dsfstudios.apps.lappr.database.entities.dbWorkout;
import com.dsfstudios.apps.lappr.ui.ViewWorkoutListener;

import java.util.List;

public class SetComponentAdapter extends RecyclerView.Adapter<SetComponentAdapter.ViewHolder> {

    private int mCardColor;
    private final List<dbSetComponent> mValues;
    private boolean mEditMode;
    private ViewWorkoutListener mListener;

    public SetComponentAdapter(List<dbSetComponent> values, int cardColor, boolean editMode, ViewWorkoutListener listener) {
        mCardColor = cardColor;
        mValues =  values;
        mEditMode = editMode;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_workout_card, parent, false);
        ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mView.setEnabled(mEditMode);
        holder.mItem = getItem(position);
        holder.mLayout.setBackgroundColor(mCardColor);
        String description = holder.mItem.toString() + "\non " + holder.mItem.intervalToString() + "\n" + holder.mItem.deltaToString();
        holder.mDescription.setText(description);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.editSetComponent(-1, holder.mItem.getId());
            }
        });

        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mListener.deleteSetComponent(holder.mItem);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    private dbSetComponent getItem(int position) {
        return mValues.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final LinearLayout mLayout;
        public final TextView mDescription;
        public dbSetComponent mItem;

        public int width = Resources.getSystem().getDisplayMetrics().widthPixels;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mLayout = (LinearLayout) view.findViewById(R.id.layout);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width / 2, ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(0, 8, 8, 8);
            mLayout.setLayoutParams(params);
            mDescription = (TextView) view.findViewById(R.id.description);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mDescription.getText() + "'";
        }
    }
}