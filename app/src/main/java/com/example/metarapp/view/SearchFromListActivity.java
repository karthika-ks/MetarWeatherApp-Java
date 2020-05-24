package com.example.metarapp.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.metarapp.databinding.ActivitySearchFromListBinding;
import com.example.metarapp.model.MetarDataManager;
import com.example.metarapp.R;
import com.example.metarapp.utilities.MetarData;
import com.example.metarapp.viewmodel.MetarViewModel;

import java.util.ArrayList;
import java.util.List;

import static com.example.metarapp.utilities.Constants.PREF_KEY_HAS_INTERNET_CONNECTIVITY;
import static com.example.metarapp.utilities.Constants.PREF_KEY_IS_AVAILABLE;
import static com.example.metarapp.utilities.Constants.PREF_NAME_GERMAN_LIST;

public class SearchFromListActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = SearchFromListActivity.class.getSimpleName();

    private List<MetarData> filteredList;
    private MetarViewModel viewModel;
    private ArrayAdapter<String> dataAdapter;
    private Spinner spinner;
    private ProgressBar downloadProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_from_list);

        ActivitySearchFromListBinding activityStationDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_search_from_list);
        viewModel = MetarViewModel.getInstance();
        activityStationDetailsBinding.setViewModel(viewModel);
        activityStationDetailsBinding.executePendingBindings();
        activityStationDetailsBinding.setLifecycleOwner(this);

        viewModel.registerLifeCycleObserver(getLifecycle());

        spinner = findViewById(R.id.spinner);
        downloadProgress = findViewById(R.id.progress_list_download);

        filteredList = new ArrayList<>();
        List<String> filteredStationList = new ArrayList<>();
        filteredStationList.add("Select an item...");

        dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredStationList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        spinner.setOnItemSelectedListener(this);

        fetchStationList();
        updateListOnUI();

        SharedPreferences pref = getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);
        pref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != 0 && position <= filteredList.size()) {
            viewModel.startMetarService(filteredList.get(position - 1).getCode());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void onHomeClicked(View view) {
        finish();
    }

    private void updateListOnUI() {
        Log.i(TAG, "updateListOnUI: 1");
        SharedPreferences pref = getApplicationContext().getSharedPreferences(PREF_NAME_GERMAN_LIST, MODE_PRIVATE);

        RelativeLayout lytSpinner = findViewById(R.id.lyt_search_bar);

        if (filteredList != null && !filteredList.isEmpty()) {
            downloadProgress.setVisibility(View.GONE);
            lytSpinner.setVisibility(View.VISIBLE);
            dataAdapter.notifyDataSetChanged();
            spinner.invalidate();
        } else {
            Log.i(TAG, "updateListOnUI: German stations are downloading");
            downloadProgress.setVisibility(View.VISIBLE);
            lytSpinner.setVisibility(View.INVISIBLE);

            if (!pref.getBoolean(PREF_KEY_HAS_INTERNET_CONNECTIVITY, false)) {
                downloadProgress.setVisibility(View.GONE);
                viewModel.hasNetworkConnectivity.setValue(false);
            }
        }
    }

    private void fetchStationList() {
        String[] stationArray = MetarDataManager.getInstance().getFilteredStationList();
        filteredList.clear();
        dataAdapter.clear();
        dataAdapter.add("Select an item...");

        if (stationArray != null) {
            for (String station : stationArray) {
                String stationName = MetarDataManager.getInstance().getStationNameFromCode(station);
                dataAdapter.add(station);

                MetarData metarData = new MetarData();
                metarData.setCode(station);
                metarData.setStationName(stationName);
                filteredList.add(metarData);
            }
        } else {
            // Show progress bar
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
