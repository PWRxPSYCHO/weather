package org.woheller69.weather.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.woheller69.weather.R;
import org.woheller69.weather.database.CityToWatch;
import org.woheller69.weather.database.PFASQLiteHelper;
import org.woheller69.weather.dialogs.AddLocationDialog;
import org.woheller69.weather.preferences.PrefManager;
import org.woheller69.weather.services.UpdateDataService;
import org.woheller69.weather.ui.RecycleList.RecyclerItemClickListener;
import org.woheller69.weather.ui.RecycleList.RecyclerOverviewListAdapter;
import org.woheller69.weather.ui.RecycleList.SimpleItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.support.v4.app.JobIntentService.enqueueWork;

//in-App: where cities get added & sorted
public class ManageLocationsActivity extends NavigationActivity {

    private final String DEBUG_TAG = "main_activity_debug";
    private PFASQLiteHelper database;
    PrefManager prefManager;
    private ItemTouchHelper.Callback callback;
    private ItemTouchHelper touchHelper;
    RecyclerOverviewListAdapter adapter;
    List<CityToWatch> cities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_locations);
        overridePendingTransition(0, 0);

        database = PFASQLiteHelper.getInstance(this);

        cities = new ArrayList<CityToWatch>();

        try {
            cities = database.getAllCitiesToWatch();
            Collections.sort(cities, new Comparator<CityToWatch>() {
                @Override
                public int compare(CityToWatch o1, CityToWatch o2) {
                    return o1.getRank() - o2.getRank();
                }

            });
        } catch (NullPointerException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(getBaseContext(), "No cities in DB", Toast.LENGTH_SHORT);
            toast.show();
        }

        prefManager = new PrefManager(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list_view_cities);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
      //  recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getBaseContext()));

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getBaseContext(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        // Get the corresponding city_id of the entry and pass it on to the started activity
                        int cityId = RecyclerOverviewListAdapter.getListItems().get(position).getCityId();
                        Intent intent = new Intent(getBaseContext(), ForecastCityActivity.class);
                        intent.putExtra("cityId", cityId);
                        startFetchingService(cityId);
                        startActivity(intent);
                    }

                    public void onLongItemClick(View view, int position) {
                        //sets the current city as default location - not used since sorting available
                        //CityToWatch city = RecyclerOverviewListAdapter.getListItems().get(position);
                        //setDefaultLocation(city);
                    }

                })
        );

        adapter = new RecyclerOverviewListAdapter(getBaseContext(), cities);
        recyclerView.setAdapter(adapter);
        recyclerView.setFocusable(false);

        callback = new SimpleItemTouchHelperCallback(adapter);
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        FloatingActionButton addFab = (FloatingActionButton) findViewById(R.id.fabAddLocation);
        if (addFab != null) {

            addFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    AddLocationDialog addLocationDialog = new AddLocationDialog();
                    addLocationDialog.show(fragmentManager, "AddLocationDialog");
                    getSupportFragmentManager().executePendingTransactions();
                    addLocationDialog.getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                }
            });

        }

    }

    public void startFetchingService(int cityId) {
        // Start a background task to retrieve and store the weather data
        Intent intent = new Intent(this, UpdateDataService.class);
        intent.setAction(UpdateDataService.UPDATE_SINGLE_ACTION);  //changed to update single. Only selected city needs update, others will be updated as their tabs are selected
        intent.putExtra("cityId", cityId);
        enqueueWork(this, UpdateDataService.class, 0, intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_manage;
    }

    public void addCityToList(CityToWatch city) {
        cities.add(city);
        adapter.notifyDataSetChanged();
    }

}
