package com.dsfstudios.apps.lappr.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dsfstudios.apps.lappr.R;

import java.util.Calendar;

public class MyWorkouts extends Fragment {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private MyWorkoutsListener mListener;

    public MyWorkouts() {
    }

    public static MyWorkouts newInstance(Bundle bundle) {
        MyWorkouts fragment = new MyWorkouts();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = new Bundle();
        if (getArguments() != null)
            bundle = getArguments();
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(), bundle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_workouts, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.workoutsPager);
        mTabLayout = (TabLayout) view.findViewById(R.id.workoutsTabs);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (MyWorkoutsListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private Bundle bundle;

        public SectionsPagerAdapter(FragmentManager fm, Bundle bundle) {
            super(fm);
            this.bundle = bundle;
        }

        @Override
        public Fragment getItem(int position) {
            boolean showCompleted = position % 2 == 0;
            Bundle args = new Bundle();
            args.putBoolean("showCompleted", showCompleted);
            args.putString("TAG", this.bundle.getString("TAG"));
            if (this.bundle.getString("TAG").equals("View Workout")) {
                Calendar cal = Calendar.getInstance();
                args.putLong("date", cal.getTimeInMillis());
                args.putBoolean("complete", false);
            } else {
                args.putLong("date", this.bundle.getLong("date"));
                args.putBoolean("complete", this.bundle.getBoolean("complete"));
            }
            WorkoutList sectionFragment = WorkoutList.newInstance(args);
            return sectionFragment;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0:
                    return "Completed Workouts";
                case 1:
                    return "Upcoming Workouts";
                default:
                    return "All Workouts";
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public interface MyWorkoutsListener {

    }
}
