package com.example.metarapp.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.metarapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StationListAdapter extends ArrayAdapter {
    private List<String> stationList = new ArrayList<>();

    static class StationViewHolder {
        TextView stationCode;
    }

    StationListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    void setStationList(String[] stationList) {
        this.stationList.clear();
        this.stationList.addAll(Arrays.asList(stationList));
    }

    @Override
    public void add(@Nullable Object object) {
        super.add(object);
        stationList.add((String) object);
    }

    @Override
    public int getCount() {
        return this.stationList.size();
    }

    @Override
    public String getItem(int index) {
        return this.stationList.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        StationViewHolder viewHolder;

        if (row == null) {

            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.station_list_item_card, parent, false);
            viewHolder = new StationViewHolder();
            viewHolder.stationCode = row.findViewById(R.id.station_code);
            row.setTag(viewHolder);

        } else {
            viewHolder = (StationViewHolder) row.getTag();
        }
        String code = getItem(position);
        viewHolder.stationCode.setText(code);
        return row;
    }
}
