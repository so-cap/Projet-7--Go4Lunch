package com.sophieopenclass.go4lunch.base;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.facebook.FacebookSdk;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sophieopenclass.go4lunch.R;
import com.sophieopenclass.go4lunch.controllers.activities.LoginPageActivity;
import com.sophieopenclass.go4lunch.controllers.activities.RestaurantDetailsActivity;
import com.sophieopenclass.go4lunch.controllers.activities.WorkmateDetailActivity;
import com.sophieopenclass.go4lunch.injection.Injection;
import com.sophieopenclass.go4lunch.listeners.Listeners;
import com.sophieopenclass.go4lunch.notifications.NotificationWorker;
import com.sophieopenclass.go4lunch.utils.ViewModelFactory;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.EasyPermissions;

import static android.content.Intent.EXTRA_UID;
import static com.sophieopenclass.go4lunch.utils.Constants.LOCATION_PERMISSION_REQUEST_CODE;
import static com.sophieopenclass.go4lunch.utils.Constants.PERMS;
import static com.sophieopenclass.go4lunch.utils.Constants.PLACE_ID;
import static com.sophieopenclass.go4lunch.utils.Constants.PREF_LANGUAGE;
import static com.sophieopenclass.go4lunch.utils.Constants.PREF_REMINDER;
import static com.sophieopenclass.go4lunch.utils.Constants.SHARED_PREFS;
import static com.sophieopenclass.go4lunch.utils.Constants.WORK_REQUEST_NAME;

public abstract class BaseActivity<T extends ViewModel> extends AppCompatActivity implements Listeners.OnWorkmateClickListener,
        Listeners.OnRestaurantClickListener {
    public static final int LOCATION_REQUEST_CODE = 777;
    public final String TAG = "MAIN";
    public T viewModel;
    public final WorkManager workManager = WorkManager.getInstance(this);
    public static Location sCurrentLocation = null;
    public LocationManager locationManager;
    public static SharedPreferences sharedPrefs;
    public static boolean ORIENTATION_CHANGED = false;
    public PeriodicWorkRequest workRequest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        FacebookSdk.setAdvertiserIDCollectionEnabled(false);
        FacebookSdk.setAutoLogAppEventsEnabled(false);
        configureViewModel();
        setContentView(this.getFragmentLayout());
    }

    @SuppressWarnings("unchecked")
    protected void configureViewModel() {
        ViewModelFactory viewModelFactory = Injection.provideViewModelFactory();
        viewModel = (T) new ViewModelProvider(this, viewModelFactory).get(getViewModelClass());
    }

    public abstract Class getViewModelClass();

    protected abstract View getFragmentLayout();


    // --------------------
    // ERROR HANDLER
    // --------------------

    protected OnFailureListener onFailureListener() {
        return e -> Toast.makeText(getApplicationContext(), getString(R.string.error_unknown_error), Toast.LENGTH_LONG).show();
    }

    // ---------
    // PERMISSIONS CHECKS
    // ---------------

    public boolean networkUnavailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
    }

    public boolean requestLocationPermission() {
        boolean locationAvailable = false;
        if (!EasyPermissions.hasPermissions(this, PERMS)) {
            EasyPermissions.requestPermissions(this,
                    "Cette application a besoin de l'accès à votre localisation pour fonctionner.",
                    LOCATION_PERMISSION_REQUEST_CODE, PERMS);
        } else if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, (paramDialogInterface, paramInt) ->
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LOCATION_REQUEST_CODE))
                    .setNegativeButton(R.string.Cancel, null)
                    .show();
        } else
            locationAvailable = true;
        return locationAvailable;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    // --------------------
    // UTILS
    // --------------------

    @Nullable
    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    protected Boolean isCurrentUserLogged() {
        return (this.getCurrentUser() != null);
    }

    public T getViewModel() {
        return viewModel;
    }

    //Check for locale in BaseActivity as it is used in several activities
    public void checkCurrentLocale() {
        String defaultLocale = Locale.getDefault().getLanguage();
        if (!sharedPrefs.contains(PREF_LANGUAGE))
            sharedPrefs.edit().putString(PREF_LANGUAGE, defaultLocale).apply();
        else if (!sharedPrefs.getString(PREF_LANGUAGE, defaultLocale).equals(defaultLocale))
            updateLocale();
    }

    public void updateLocale() {
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.setLocale(new Locale(sharedPrefs.getString(PREF_LANGUAGE, Locale.getDefault().getLanguage())));
        res.updateConfiguration(conf, dm);
    }

    //

    public void activateReminder() {
        Calendar currentDate = Calendar.getInstance();
        Calendar dueDate = Calendar.getInstance();
        // Set Execution time of the reminder
        dueDate.set(Calendar.HOUR_OF_DAY, 12);
        dueDate.set(Calendar.MINUTE, 0);
        dueDate.set(Calendar.SECOND, 0);
        dueDate.set(Calendar.MILLISECOND, 0);

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_MONTH, 1);
        }

        long timeDiff = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();

        Data userId = new Data.Builder().build();
        if (getCurrentUser() != null)
            userId = new Data.Builder()
                    .putString(EXTRA_UID, getCurrentUser().getUid())
                    .build();

        workRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1,
                TimeUnit.DAYS)
                .setInputData(userId)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build();

        workManager.enqueueUniquePeriodicWork(WORK_REQUEST_NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
        sharedPrefs.edit().putBoolean(PREF_REMINDER, true).apply();

        workManager.getWorkInfoByIdLiveData(workRequest.getId()).observe(this, workInfo -> {
                    if (workInfo != null)
                        Log.i(TAG, "current workState " + workInfo.getState());
                }
        );
        Log.i(TAG, "activateReminder: ");
    }


    @Override
    public void onWorkmateClick(String uid) {
        Intent intent = new Intent(this, WorkmateDetailActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        startActivity(intent);
    }

    @Override
    public void onRestaurantClick(String placeId) {
        Intent intent = new Intent(this, RestaurantDetailsActivity.class);
        intent.putExtra(PLACE_ID, placeId);
        startActivity(intent);
    }

    public void signOut() {
        AuthUI.getInstance().signOut(this).addOnSuccessListener(aVoid -> backToLoginPage());
    }

    public void deleteAccount() {
        AuthUI.getInstance().delete(this).addOnSuccessListener(v -> {
            backToLoginPage();
        });
    }
    

    protected void backToLoginPage() {
        finishAffinity();
        Intent intent = new Intent(this, LoginPageActivity.class);
        startActivity(intent);
        workManager.cancelAllWork();
    }
}



