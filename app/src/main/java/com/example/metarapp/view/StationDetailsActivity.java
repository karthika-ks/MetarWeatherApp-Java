package com.example.metarapp.view;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.metarapp.R;
import com.example.metarapp.databinding.ActivityStationDetailsBinding;
import com.example.metarapp.viewmodel.MetarViewModel;

import static com.example.metarapp.utilities.Constants.EXTRA_CODE;

public class StationDetailsActivity extends AppCompatActivity {
    private static final String TAG = StationDetailsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_details);

        ActivityStationDetailsBinding activityStationDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_station_details);
        MetarViewModel viewModel = MetarViewModel.getInstance(getApplicationContext());
        activityStationDetailsBinding.setViewModel(viewModel);
        activityStationDetailsBinding.executePendingBindings();
        activityStationDetailsBinding.setLifecycleOwner(this);

        String stationCode = getIntent().getStringExtra(EXTRA_CODE);
        Log.i(TAG, "onCreate: " + stationCode);
        viewModel.startMetarService(stationCode);
        viewModel.registerLifeCycleObserver(getLifecycle());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
