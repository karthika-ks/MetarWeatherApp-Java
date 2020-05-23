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
import com.example.metarapp.utilities.MetarData;

import java.util.ArrayList;
import java.util.List;

import static com.example.metarapp.utilities.Constants.EXTRA_CODE;
import static com.example.metarapp.utilities.Constants.PREF_KEY_IS_AVAILABLE;
import static com.example.metarapp.utilities.Constants.PREF_NAME_GERMAN_LIST;

public class StationListActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = StationListActivity.class.getSimpleName();
    private StationListAdapter stationListAdapter;
    private List<MetarData> germanStations;
    ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.station_listview);
        listView = findViewById(R.id.card_listView);

        germanStations = new ArrayList<>();
        stationListAdapter = new StationListAdapter(getApplicationContext(), R.layout.station_list_item_card);
        stationListAdapter.setStationList(germanStations);
        listView.setAdapter(stationListAdapter);

        listView.addHeaderView(new View(this));
        listView.addFooterView(new View(this));

        fetchStationList();
        updateListOnUI();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), StationDetailsActivity.class);
                intent.putExtra(EXTRA_CODE, germanStations.get(position - 1).getCode());
                startActivity(intent);
            }
        });

        SharedPreferences pref = getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);
        pref.registerOnSharedPreferenceChangeListener(this);
    }

    private void updateListOnUI() {
        Log.i(TAG, "updateListOnUI: ");

        if (germanStations != null) {
            Log.i(TAG, "updateListOnUI: germanStations size " + germanStations.size());
            stationListAdapter.setStationList(germanStations);
            stationListAdapter.notifyDataSetChanged();
            stationListAdapter.notifyDataSetInvalidated();
            listView.invalidate();
            listView.invalidateViews();
            listView.refreshDrawableState();


        } else {
            Log.i(TAG, "onCreate: German stations are downloading");
        }
    }

    private void fetchStationList() {
        String[] stationArray = MetarDataManager.getInstance().getFilteredStationList();
        germanStations.clear();

        if (stationArray != null) {
            for (String station : stationArray) {
                String stationName = MetarDataManager.getInstance().getStationNameFromCode(station);

                MetarData metarData = new MetarData();
                metarData.setCode(station);
                metarData.setStationName(stationName);
                germanStations.add(metarData);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged: Key = " + key);

        if (sharedPreferences.getBoolean(PREF_KEY_IS_AVAILABLE, false)) {
            fetchStationList();
            updateListOnUI();
        }
    }
}
