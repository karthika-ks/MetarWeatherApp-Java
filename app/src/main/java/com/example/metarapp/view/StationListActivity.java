package com.example.metarapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.metarapp.model.MetarDataManager;
import com.example.metarapp.R;

import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.PREF_KEY_IS_AVAILABLE;
import static com.example.metarapp.utilities.Constants.PREF_NAME_GERMAN_LIST;

public class StationListActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = StationListActivity.class.getSimpleName();
    private StationListAdapter stationListAdapter;
    private String[] germanStations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.station_listview);
        ListView listView = findViewById(R.id.card_listView);

        listView.addHeaderView(new View(this));
        listView.addFooterView(new View(this));
        germanStations = MetarDataManager.getInstance().getFilteredStationList();

        stationListAdapter = new StationListAdapter(getApplicationContext(), R.layout.station_list_item_card);

        updateListOnUI();
        listView.setAdapter(stationListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), StationDetailsActivity.class);
                intent.putExtra(EXTRA_CODE, germanStations[position - 1]);
                startActivity(intent);
            }
        });

        SharedPreferences pref = getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);
        pref.registerOnSharedPreferenceChangeListener(this);
    }

    private void updateListOnUI() {
        Log.i(TAG, "updateListOnUI: ");

        if (germanStations != null) {
            Log.i(TAG, "updateListOnUI: germanStations size " + germanStations.length);
            stationListAdapter.setStationList(germanStations);
            stationListAdapter.notifyDataSetChanged();
        } else {
            Log.i(TAG, "onCreate: German stations are downloading");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged: Key = " + key);
        if (sharedPreferences.getBoolean(PREF_KEY_IS_AVAILABLE, false)) {
            germanStations = MetarDataManager.getInstance().getFilteredStationList();
            updateListOnUI();
        }
    }
}
