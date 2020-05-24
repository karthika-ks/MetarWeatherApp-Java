package com.example.metarapp.view;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;

import com.example.metarapp.viewmodel.MetarViewModel;
import com.example.metarapp.R;
import com.example.metarapp.databinding.ActivitySearchByCodeBinding;

public class SearchByCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_by_code);

        // Binding with ViewModel
        ActivitySearchByCodeBinding activityBinding = DataBindingUtil.setContentView(this, R.layout.activity_search_by_code);
        MetarViewModel viewModel = MetarViewModel.getInstance();
        activityBinding.setViewModel(viewModel);
        activityBinding.executePendingBindings();
        activityBinding.setLifecycleOwner(this);
        viewModel.registerLifeCycleObserver(getLifecycle());

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
               // Convert string entered to uppercase
               String s = editable.toString();
               if (!s.equals(s.toUpperCase())) {
                   s = s.toUpperCase();
                   editText.setText(s);
                   editText.setSelection(editText.getText().toString().length());
               }
           }
       });
    }

    public void onHomeClicked(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
