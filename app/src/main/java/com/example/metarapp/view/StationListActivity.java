package com.example.metarapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.metarapp.MetarDataManager;
import com.example.metarapp.R;

public class StationListActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = StationListActivity.class.getSimpleName();
    private ListView listView;
    private StationListAdapter stationListAdapter;
    private String[] germanStations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.station_listview);
        listView = findViewById(R.id.card_listView);

        listView.addHeaderView(new View(this));
        listView.addFooterView(new View(this));
        germanStations = MetarDataManager.getInstance().getStationList();

        stationListAdapter = new StationListAdapter(getApplicationContext(), R.layout.station_list_item_card);

        updateListOnUI();
        listView.setAdapter(stationListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), StationDetailsActivity.class);
                intent.putExtra("station_code", germanStations[position - 1]);
                startActivity(intent);
            }
        });

        SharedPreferences pref = getApplicationContext().getSharedPreferences("GERMAN_LIST_STATUS", MODE_PRIVATE);
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
        if (sharedPreferences.getBoolean("IS_AVAILABLE", false)) {
            germanStations = MetarDataManager.getInstance().getStationList();
            updateListOnUI();
        }
    }
}
