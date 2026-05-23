package com.androidapp.SmartTask;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvLocations;
    private TextView btnCurrentLocation;
    private LocationAdapter adapter;
    private List<Address> addressList = new ArrayList<>();
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        geocoder = new Geocoder(this, Locale.getDefault());

        etSearch = findViewById(R.id.etSearch);
        rvLocations = findViewById(R.id.rvLocations);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        TextView btnBack = findViewById(R.id.btnBack);

        adapter = new LocationAdapter(addressList);
        rvLocations.setLayoutManager(new LinearLayoutManager(this));
        rvLocations.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnSearch).setOnClickListener(v -> searchLocation());

        btnCurrentLocation.setOnClickListener(v -> {
            // Trả về vị trí mặc định (có thể thay bằng GPS)
            Intent result = new Intent();
            result.putExtra("location_name", "Vi tri hien tai");
            result.putExtra("location_lat", 10.762622);
            result.putExtra("location_lng", 106.660172);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private void searchLocation() {
        String query = etSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Nhap dia diem", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            List<Address> results = geocoder.getFromLocationName(query, 10);
            if (results != null && !results.isEmpty()) {
                addressList.clear();
                addressList.addAll(results);
                adapter.notifyDataSetChanged();

                if (addressList.isEmpty()) {
                    Toast.makeText(this, "Khong tim thay", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Khong tim thay dia diem", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Loi ket noi", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectLocation(Address address) {
        Intent result = new Intent();
        result.putExtra("location_name", address.getAddressLine(0));
        result.putExtra("location_lat", address.getLatitude());
        result.putExtra("location_lng", address.getLongitude());
        setResult(RESULT_OK, result);
        finish();
    }

    private class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.VH> {
        private List<Address> list;

        LocationAdapter(List<Address> list) {
            this.list = list;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false));
        }

        @Override
        public void onBindViewHolder(VH holder, int pos) {
            Address addr = list.get(pos);
            holder.tv1.setText(addr.getFeatureName());
            holder.tv2.setText(addr.getAddressLine(0));
            holder.itemView.setOnClickListener(v -> selectLocation(addr));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tv1, tv2;

            VH(android.view.View v) {
                super(v);
                tv1 = v.findViewById(android.R.id.text1);
                tv2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}