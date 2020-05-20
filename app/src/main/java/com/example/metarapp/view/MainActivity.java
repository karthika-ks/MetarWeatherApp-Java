package com.example.metarapp.view;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.databinding.DataBindingUtil;

import com.example.metarapp.viewmodel.MetarViewModel;
import com.example.metarapp.R;
import com.example.metarapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private MetarViewModel mViewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityMainBinding activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mViewModel = new MetarViewModel(getApplicationContext());
        activityMainBinding.setViewModel(mViewModel);
        activityMainBinding.executePendingBindings();
        activityMainBinding.setLifecycleOwner(this);
    }
}
