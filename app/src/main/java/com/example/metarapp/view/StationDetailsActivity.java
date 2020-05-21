package com.example.metarapp.view;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.metarapp.R;
import com.example.metarapp.databinding.ActivityStationDetailsBinding;
import com.example.metarapp.viewmodel.MetarViewModel;

public class StationDetailsActivity extends AppCompatActivity {
    private MetarViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_details);

        ActivityStationDetailsBinding activityStationDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_station_details);
        mViewModel = MetarViewModel.getInstance(getApplicationContext());
        activityStationDetailsBinding.setViewModel(mViewModel);
        activityStationDetailsBinding.executePendingBindings();
        activityStationDetailsBinding.setLifecycleOwner(this);

        String stationCode = getIntent().getStringExtra("station_code");
        Log.i(">>>>>>", "onCreate: " + stationCode);
        mViewModel.startMetarService(stationCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.clearMutableValues();
    }
}
