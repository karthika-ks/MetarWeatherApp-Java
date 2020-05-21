package com.example.metarapp.view;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;

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
        mViewModel = MetarViewModel.getInstance(getApplicationContext());
        activityMainBinding.setViewModel(mViewModel);
        activityMainBinding.executePendingBindings();
        activityMainBinding.setLifecycleOwner(this);

       final EditText editText = findViewById(R.id.edit_code);
       editText.addTextChangedListener(new TextWatcher() {
           @Override
           public void beforeTextChanged(CharSequence s, int start, int count, int after) {

           }

           @Override
           public void onTextChanged(CharSequence s, int start, int before, int count) {

           }

           @Override
           public void afterTextChanged(Editable editable) {
               String s = editable.toString();
               if (!s.equals(s.toUpperCase())) {
                   s = s.toUpperCase();
                   editText.setText(s);
                   editText.setSelection(editText.getText().toString().length());
               }
           }
       });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.clearMutableValues();
    }
}
