package com.dsfstudios.apps.lappr.ui;

import com.dsfstudios.apps.lappr.R;
import com.dsfstudios.apps.lappr.SettingsActivity;
import com.dsfstudios.apps.lappr.Stroke;
import com.dsfstudios.apps.lappr.ui.adapters.WorkoutAdapterListener;
import com.dsfstudios.apps.lappr.database.*;
import com.dsfstudios.apps.lappr.database.entities.*;
import com.dsfstudios.apps.lappr.viewmodel.WorkoutViewModel;
import com.dsfstudios.apps.lappr.viewmodel.WorkoutViewModelFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.app.DialogFragment;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordWorkout extends AppCompatActivity implements
        WorkoutAdapterListener, Dashboard.DashboardListener,
        RecordWorkoutDetails.OnWorkoutDetailsListener, MyWorkouts.MyWorkoutsListener {

    // Receiver to handle logout request
    private BroadcastReceiver logoutReceiver;

    // Firebase authorization object
    private FirebaseAuth mAuth;

    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private WorkoutViewModel model;
    private MutableLiveData<Long> mSwimmerId = new MutableLiveData<>();

    // menu items
    private static final int VIEW_WORKOUT = 1;
    private static final int SETTINGS = 2;
    private static final int SCREEN_DASHBOARD = 3;
    private static final int SCREEN_CHOOSE_WORKOUT = 4;
    private static final int MY_WORKOUTS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        /*
        if (!isLogin()) {
            login();
        }*/

        setContentView(R.layout.activity_home);

        // build toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24px);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        TextView text = (TextView) header.findViewById(R.id.textView);
        text.setText("");

        // show e-mail/username in drawer header
        /*
        if (isLogin())
            text.setText(mAuth.getCurrentUser().getEmail());
            */

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        //menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here
                        switch (menuItem.getItemId()) {
                            // If we are on the lineup tab, go back to the lineup, otherwise go back to previous screen
                            case R.id.navigation_dashboard:
                                navigate(SCREEN_DASHBOARD);
                                return true;
                            case R.id.navigation_workouts:
                                navigate(MY_WORKOUTS);
                                break;
                            case R.id.navigation_about:
                                showAbout();
                                break;
                        }
                        return true;
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.package.ACTION_LOGOUT");
        logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Received logout broadcast
                login();
            }
        };

        registerReceiver(logoutReceiver, intentFilter);

        if (!isLogin())
            login();
        else {*/

            WorkoutViewModelFactory factory = new WorkoutViewModelFactory(AppDatabase.getAppDatabase(this));
            model = ViewModelProviders.of(this, factory).get(WorkoutViewModel.class);

            // set up swimmer Id here
        /*
            mSwimmerId.setValue(-1l);
            String authUid = mAuth.getUid();
            */
            String authUid = "000000";    // fake authUid while not using Firebase, stored in user's local database
            AppDatabase db = AppDatabase.getAppDatabase(this);
            initSwimmer(db, model, authUid, mSwimmerId);

            mSwimmerId.observe(this, new Observer<Long>() {
                @Override
                public void onChanged(@Nullable Long id) {
                    if (!id.equals(-1l))
                        navigate(SCREEN_DASHBOARD);
                }
            });
        /*} */
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(logoutReceiver);
    }

    // Function to asynchronously get the swimmer ID from the database and set the swimmer ID in the View Model
    private static void initSwimmer(final AppDatabase db, final WorkoutViewModel model, final String authUid, final MutableLiveData<Long> swimmerId) {
        new AsyncTask<String, Void, Long>() {

            @Override
            protected Long doInBackground(String... swimmer) {

                dbSwimmer existing = db.dao().getSwimmerByUid(swimmer[0]);
                long id = -1;
                if (existing != null)
                    id = existing.getId();
                else {
                    dbSwimmer newSwimmer = new dbSwimmer(swimmer[0], "", dbSwimmer.DEFAULT_UPCOMING_WORKOUTS, dbSwimmer.DEFAULT_RECENT_WORKOUTS, 25);
                    id = db.dao().addSwimmer(newSwimmer);
                }
                model.setSwimmerId(id);
                return id;
            }

            @Override
            protected void onPostExecute(Long id) {
                swimmerId.postValue(id);
            }
        }.execute(authUid);
    }

    public boolean isLogin() {
        return mAuth.getCurrentUser() != null;
    }

    public void login() {
        Intent login = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(login);
        finish();
    }

    public void logout() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("isLogin", false).commit();
        mAuth.signOut();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("com.package.ACTION_LOGOUT");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public Map<Stroke, Integer> getStrokeColors() {
        Map<Stroke, Integer> strokeColors = new HashMap<>();
        strokeColors.put(Stroke.FREESTYLE, ContextCompat.getColor(getApplicationContext(), R.color.Freestyle));
        strokeColors.put(Stroke.BACKSTROKE, ContextCompat.getColor(getApplicationContext(), R.color.Backstroke));
        strokeColors.put(Stroke.BREASTSTROKE, ContextCompat.getColor(getApplicationContext(), R.color.Breaststroke));
        strokeColors.put(Stroke.BUTTERFLY, ContextCompat.getColor(getApplicationContext(), R.color.Butterfly));
        strokeColors.put(Stroke.CHOICE, ContextCompat.getColor(getApplicationContext(), R.color.Choice));
        return strokeColors;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        ViewWorkout viewWorkoutFragment = (ViewWorkout) getSupportFragmentManager().findFragmentByTag("workoutView");
        if (viewWorkoutFragment != null && viewWorkoutFragment.isVisible()) {
            if (!viewWorkoutFragment.onBackPressed())
                super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    private void loadFragment(Fragment fragment, String tag) {
        if (getSupportFragmentManager().getFragments().size() > 0) {
            Fragment toLoad;
            if (getSupportFragmentManager().findFragmentByTag(tag) != null) {
                toLoad = getSupportFragmentManager().findFragmentByTag(tag);
            } else {
                toLoad = fragment;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, toLoad, tag).addToBackStack(null).commit();
        } else {
            getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer, fragment, tag).commit();
        }
    }

    private void navigate(int screen) {
        navigate(screen, null);
    }

    // set up menu for fragment and load fragment
    private void navigate(int screen, @Nullable Bundle bundle) {
        mToolbar.getMenu().clear();
        switch (screen) {
            case VIEW_WORKOUT:
                final ViewWorkout viewWorkout = ViewWorkout.newInstance(bundle);
                loadFragment(viewWorkout, "workoutView");
                break;
            case SETTINGS:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case SCREEN_DASHBOARD:
                loadFragment(Dashboard.newInstance(), "dashboard");
                break;
            case SCREEN_CHOOSE_WORKOUT:
                loadFragment(MyWorkouts.newInstance(bundle), "chooseWorkout");
                break;
            case MY_WORKOUTS:
                Bundle args = new Bundle();
                args.putString("TAG", "View Workout");
                loadFragment(MyWorkouts.newInstance(args), "myWorkouts");
                break;
        }
    }

    private void showAbout() {
        AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View aboutLayout = inflater.inflate(R.layout.dialog_about, null);

        LinearLayout dialogContainer = aboutLayout.findViewById(R.id.dialog);
        final TextView about = (TextView) dialogContainer.findViewById(R.id.about);
        //final SpannableString linkedAbout = new SpannableString(this.getText(R.string.aboutCredits));
        //Linkify.addLinks(linkedAbout, Linkify.WEB_URLS);
        about.setMovementMethod(LinkMovementMethod.getInstance());
        about.setText(R.string.aboutCredits);

        aboutDialog.setTitle(R.string.aboutTitle);

        aboutDialog.setPositiveButton(R.string.aboutPositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        aboutDialog.setView(aboutLayout);
        AlertDialog dialogAbout = aboutDialog.create();
        dialogAbout.show();
    }

    @Override
    public void createBlankWorkout(Date date, boolean complete) {
        model.addWorkout(date, complete);
        viewWorkout(true);
    }

    public void viewWorkout(boolean editMode) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("editMode", editMode);
        navigate(VIEW_WORKOUT, bundle);
    }

    @Override
    // user sent to list of workouts to choose a clone
    public void selectWorkout(long date, boolean complete, String TAG) {
        Bundle bundle = new Bundle();
        bundle.putLong("date", date);
        bundle.putBoolean("complete", complete);
        bundle.putString("TAG", TAG);
        navigate(SCREEN_CHOOSE_WORKOUT, bundle);
    }
}
