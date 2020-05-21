package com.example.metarapp.view;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.metarapp.MetarDataManager;
import com.example.metarapp.R;

public class StationListActivity extends AppCompatActivity {
    private ListView listView;
    private StationListAdapter stationListAdapter;
    private String[] germanStations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.station_listview);
        listView = findViewById(R.id.card_listView);
        Resources res = getResources();
        germanStations = res.getStringArray(R.array.station_icao_code_list);

        listView.addHeaderView(new View(this));
        listView.addFooterView(new View(this));
//        germanStations = MetarDataManager.getInstance().getStationList();

        stationListAdapter = new StationListAdapter(getApplicationContext(), R.layout.station_list_item_card);

        for (int i = 0; i < germanStations.length; i++) {
            stationListAdapter.add(germanStations[i]);
        }
        listView.setAdapter(stationListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), StationDetailsActivity.class);
                intent.putExtra("station_code", germanStations[position - 1]);
                startActivity(intent);
            }
        });
    }
}
