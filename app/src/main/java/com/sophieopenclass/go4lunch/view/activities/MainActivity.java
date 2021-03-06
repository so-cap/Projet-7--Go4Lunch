package com.sophieopenclass.go4lunch.view.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.auth.AuthUI;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.navigation.NavigationView;
import com.sophieopenclass.go4lunch.AppController;
import com.sophieopenclass.go4lunch.BuildConfig;
import com.sophieopenclass.go4lunch.MyViewModel;
import com.sophieopenclass.go4lunch.R;
import com.sophieopenclass.go4lunch.base.BaseActivity;
import com.sophieopenclass.go4lunch.view.fragments.MapViewFragment;
import com.sophieopenclass.go4lunch.view.fragments.RestaurantListFragment;
import com.sophieopenclass.go4lunch.view.fragments.WorkmatesListFragment;
import com.sophieopenclass.go4lunch.databinding.ActivityMainBinding;
import com.sophieopenclass.go4lunch.models.User;
import com.sophieopenclass.go4lunch.utils.PreferenceHelper;

import static android.content.Intent.EXTRA_UID;
import static com.sophieopenclass.go4lunch.utils.Constants.ACTIVITY_MY_LUNCH;
import static com.sophieopenclass.go4lunch.utils.Constants.ACTIVITY_SETTINGS;
import static com.sophieopenclass.go4lunch.utils.Constants.FRAGMENT_MAP_VIEW;
import static com.sophieopenclass.go4lunch.utils.Constants.FRAGMENT_RESTAURANT_LIST_VIEW;
import static com.sophieopenclass.go4lunch.utils.Constants.FRAGMENT_WORKMATES_LIST;

public class MainActivity extends BaseActivity<MyViewModel> implements NavigationView.OnNavigationItemSelectedListener {
    private String currentUserId;
    private Fragment fragmentMapView;
    private Fragment fragmentRestaurantList;
    private Fragment fragmentWorkmatesList;
    public PlacesClient placesClient;
    public ActivityMainBinding binding;

    @Override
    public View getLayout() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureToolbar();
        configureDrawerLayout();
        configureNavigationView();
        initPlacesApi();
        setReminder();
        if (getCurrentUser() != null)
            viewModel.getUser(getCurrentUser().getUid()).observe(this, user -> {
                currentUserId = user.getUid();
                handleDrawerUI(user);
            });
        binding.bottomNavView.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);
        binding.searchBarRestaurantList.searchBarRestaurantList.setVisibility(View.GONE);
        binding.searchBarMap.searchBarMap.setVisibility(View.GONE);
        binding.searchBarWorkmates.searchBarWorkmates.setVisibility(View.GONE);
    }

    @Override
    public Class getViewModelClass() {
        return MyViewModel.class;
    }

    private void handleDrawerUI(User user) {
        View drawerView = binding.navigationView.getHeaderView(0);
        ImageView profilePic = drawerView.findViewById(R.id.profile_pic);
        TextView username = drawerView.findViewById(R.id.profile_username);
        TextView email = drawerView.findViewById(R.id.profile_email);

        if (user.getUrlPicture() != null) {
            Glide.with(profilePic.getContext())
                    .load(user.getUrlPicture())
                    .apply(RequestOptions.circleCropTransform())
                    .into(profilePic);
            username.setText(user.getUsername());
            email.setText(user.getEmail());
        }
    }

    private void configureToolbar() {
        setSupportActionBar(binding.myToolbar);
    }

    private void initPlacesApi() {
        Places.initialize(getApplicationContext(), BuildConfig.API_KEY);
        placesClient = Places.createClient(this);
    }

    private void configureDrawerLayout() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.myToolbar,
                R.string.open_navigation_drawer, R.string.close_navigation_drawer);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void configureNavigationView() {
        binding.navigationView.setNavigationItemSelectedListener(this);
    }

    // To activate notifications by default when first launching the app OR
    // activate the notifications if the user signed out and logged back in
    private void setReminder() {
        if (PreferenceHelper.getReminderPreference()) {
            activateReminder();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // to not display the map if the user leaves the app and comes back
        // or when permissions get granted
        restartState = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // In order to not show mapFragment every time the activity gets recreated
        if (!orientationChanged && !restartState)
            showFragmentOrActivity(FRAGMENT_MAP_VIEW);

        // Update UI
        if (AppController.getInstance().getSettingsState())
            updateUiAfterChangeInSettings();

        if (restartState)
            restartState = false;
    }

    private void updateUiAfterChangeInSettings() {
        Intent intent = new Intent(this, MainActivity.class);
        finish();
        startActivity(intent);
        AppController.getInstance().setSettingsHaveChanged(false);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fragmentMapView = getSupportFragmentManager().findFragmentByTag(FRAGMENT_MAP_VIEW);
        fragmentWorkmatesList = getSupportFragmentManager().findFragmentByTag(FRAGMENT_WORKMATES_LIST);
        fragmentRestaurantList = getSupportFragmentManager().findFragmentByTag(FRAGMENT_RESTAURANT_LIST_VIEW);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.my_lunch:
                showFragmentOrActivity(ACTIVITY_MY_LUNCH);
                break;
            case R.id.settings:
                showFragmentOrActivity(ACTIVITY_SETTINGS);
                break;
            case R.id.sign_out:
                signOut();
                break;
            case R.id.map_view:
                showFragmentOrActivity(FRAGMENT_MAP_VIEW);
                break;
            case R.id.restaurant_list_view:
                showFragmentOrActivity(FRAGMENT_RESTAURANT_LIST_VIEW);
                break;
            case R.id.workmates_view:
                showFragmentOrActivity(FRAGMENT_WORKMATES_LIST);
                break;
            default:
                break;
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (fragmentMapView == null || !fragmentMapView.isVisible())
            binding.bottomNavView.setSelectedItemId(R.id.map_view);
        else
            super.onBackPressed();
    }

    private void showFragmentOrActivity(String controllerIdentifier) {
        if (ACTIVITY_MY_LUNCH.equals(controllerIdentifier)) {
            startNewActivity(UserDetailActivity.class);
        } else if (ACTIVITY_SETTINGS.equals(controllerIdentifier)) {
            startNewActivity(SettingsActivity.class);
        } else if (FRAGMENT_MAP_VIEW.equals(controllerIdentifier)) {
            showMapViewFragment();
        } else if (FRAGMENT_RESTAURANT_LIST_VIEW.equals(controllerIdentifier)) {
            showRestaurantListFragment();
        } else if (FRAGMENT_WORKMATES_LIST.equals(controllerIdentifier)) {
            showWorkmatesListFragment();
        }
    }

    private void startNewActivity(Class activity) {
        Intent intent = new Intent(this, activity);
        if (activity.equals(UserDetailActivity.class))
            intent.putExtra(EXTRA_UID, currentUserId);
        startActivity(intent);
    }

    private void showMapViewFragment() {
        if (this.fragmentMapView == null) this.fragmentMapView = MapViewFragment.newInstance();
        startTransactionFragment(fragmentMapView);
    }

    private void showRestaurantListFragment() {
        if (this.fragmentRestaurantList == null)
            this.fragmentRestaurantList = RestaurantListFragment.newInstance();
        startTransactionFragment(fragmentRestaurantList);
    }

    private void showWorkmatesListFragment() {
        if (this.fragmentWorkmatesList == null)
            this.fragmentWorkmatesList = WorkmatesListFragment.newInstance();
        startTransactionFragment(fragmentWorkmatesList);
    }

    private void startTransactionFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment, fragment.getClass().getSimpleName()).commit();

        updateToolbarUI(fragment);
    }

    private void updateToolbarUI(Fragment fragment) {
        if (fragment instanceof WorkmatesListFragment)
            binding.myToolbar.setTitle(R.string.available_workmates);
        else
            binding.myToolbar.setTitle(R.string.im_hungry);

        binding.searchBarRestaurantList.searchBarRestaurantList.setVisibility(View.GONE);
        binding.searchBarMap.searchBarMap.setVisibility(View.GONE);
        binding.searchBarWorkmates.searchBarWorkmates.setVisibility(View.GONE);
    }

    public void signOut() {
        AuthUI.getInstance().signOut(this).addOnSuccessListener(aVoid -> backToLoginPage());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_button, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        EditText focusedEditText = null;

        if (item.getItemId() == R.id.search_bar_menu) {
            if (fragmentMapView != null && fragmentMapView.isVisible()) {
                binding.searchBarMap.searchBarMap.setVisibility(View.VISIBLE);
                focusedEditText = binding.searchBarMap.searchBarInput;
            } else if (fragmentRestaurantList != null && fragmentRestaurantList.isVisible()) {
                binding.searchBarRestaurantList.searchBarRestaurantList.setVisibility(View.VISIBLE);
                focusedEditText = binding.searchBarRestaurantList.searchBarInput;
            } else if (fragmentWorkmatesList != null && fragmentWorkmatesList.isVisible()) {
                binding.searchBarWorkmates.searchBarWorkmates.setVisibility(View.VISIBLE);
                focusedEditText = binding.searchBarWorkmates.searchBarInput;
            }
        }
        showKeyboard(focusedEditText);
        return true;
    }

    private void showKeyboard(EditText focusedEditText) {
        if (focusedEditText != null)
            focusedEditText.requestFocus();

        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null)
            inputManager.showSoftInput(focusedEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    public ActivityMainBinding getMainActivityBinding() {
        return binding;
    }

    @VisibleForTesting
    public void setDummyUserId(String userId) {
        currentUserId = userId;
    }
}
