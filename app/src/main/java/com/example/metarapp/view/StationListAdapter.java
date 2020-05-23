package com.example.metarapp.view;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.metarapp.R;
import com.example.metarapp.utilities.MetarData;

import java.util.ArrayList;
import java.util.List;

public class StationListAdapter extends ArrayAdapter<MetarData> {
    private List<MetarData> stationList = new ArrayList<>();

    static class StationViewHolder {
        TextView stationCode;
        TextView stationName;
    }

    StationListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    void setStationList(List<MetarData> list) {
        this.stationList.clear();
        this.stationList.addAll(list);
        notifyDataSetChanged();
        notifyDataSetInvalidated();
    }

    @Override
    public void add(@Nullable MetarData object) {
        super.add(object);
        stationList.add(object);
    }

    @Override
    public int getCount() {
        return this.stationList.size();
    }

    @Override
    public MetarData getItem(int index) {
        return this.stationList.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        StationViewHolder viewHolder;
        MetarData metarData = getItem(position);

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.station_list_item_card, parent, false);
            viewHolder = new StationViewHolder();
            viewHolder.stationCode = row.findViewById(R.id.station_code);
            viewHolder.stationName = row.findViewById(R.id.station_name);
            row.setTag(viewHolder);

        } else {
            viewHolder = (StationViewHolder) row.getTag();
        }

        String code = metarData.getCode();
        viewHolder.stationCode.setText(code);
        String name = metarData.getStationName();
        viewHolder.stationName.setText(name);
        row.invalidate();
        return row;
    }
}
