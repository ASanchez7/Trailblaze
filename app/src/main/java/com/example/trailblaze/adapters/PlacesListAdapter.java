package com.example.trailblaze.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.example.trailblaze.R;

import java.util.List;

public class PlacesListAdapter extends ArrayAdapter<AutocompletePrediction> {

    private Context mContext;
    private List<AutocompletePrediction> mPlacesList;

    public PlacesListAdapter(Context context, List<AutocompletePrediction> placesList) {
        super(context, 0, placesList);
        mContext = context;
        mPlacesList = placesList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.place_item, parent, false);
        }

        AutocompletePrediction place = mPlacesList.get(position);

        TextView nameTextView = convertView.findViewById(R.id.place_name_text_view);
        nameTextView.setText(place.getPrimaryText(null));

        TextView addressTextView = convertView.findViewById(R.id.place_address_text_view);
        addressTextView.setText(place.getSecondaryText(null));

        return convertView;
    }
}
