package com.example.sos;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ShowMap extends AppCompatActivity {

    FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap map;
    SupportMapFragment supportMapFragment;
    public static double currentLat = 0, currentLong = 0;
    public static Location destinationLocation = null;
    Button find;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_map);
//        find=findViewById(R.id.findlocationfor);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.showMapFragment);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(ShowMap.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(ShowMap.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }

//        getNearByPoliceStation();
    }

    public void shareLocationText(String string) {
        Log.d("Phone", "aa gua");
        SharedPreferences preferences = getSharedPreferences("EmergencyMessage", MODE_PRIVATE);
        String phone = preferences.getString("phone", null);
        String message = preferences.getString("message", null);
        Log.d("Phone", phone + "");
        if (phone == null) {
            Toast.makeText(ShowMap.this, "No Contacts Available", Toast.LENGTH_SHORT).show();
        } else {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, string, null, null);
            smsManager.sendTextMessage(phone, null, message, null, null);
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_SEND);
//        intent.putExtra(Intent.EXTRA_TEXT, string);
//        intent.setType("text/plain");
//        startActivity(Intent.createChooser(intent, getString(R.string.share_location_via)));
        }
    }


    private String getLatitude(Location location) {
        return String.format(Locale.US, "%2.5f", location.getLatitude());
    }


    private String getLongitude(Location location) {
        return String.format(Locale.US, "%3.5f", location.getLongitude());
    }

    private String formatLocation(Location location, String format) {
        return MessageFormat.format(format,
                getLatitude(location), getLongitude(location));
    }

    public void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "return ");
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    destinationLocation = location;
                    currentLong = location.getLongitude();
                    currentLat = location.getLatitude();
                    shareLocationText(formatLocation(location, getResources().getStringArray(R.array.link_options)[0]));
                    Log.d("TAG", location + " h ye meri");

                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            map = googleMap;
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            Log.d("LOC", latLng + "");
                            String current = location.getLatitude() + "," + location.getLongitude();
                            Log.d("LOC", current);
//                            DisplayTrack("Near Police Station", current);
                            MarkerOptions options = new MarkerOptions().position(latLng).title("Current Position");
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            googleMap.addMarker(options);
                            getNearByPoliceStation();
                        }
                    });
                }
            }
        });

    }

    private void getNearByPoliceStation() {
        Log.d("Loc", currentLat + "," + currentLong);
        String url = "https://maps.googleapis.com/maps/api/place/search/json?location=" + currentLat + "," + currentLong + "&rankby=distance&types=police&sensor=true&key=" + getResources().getString(R.string.google_maps_key);
        new PlaceTask().execute(url);
    }


    private void DisplayTrack(String panchkula, String barwala) {
        try {
            Uri uri = Uri.parse("https://www.google.co.in/maps/dir/" + panchkula + "/" + barwala);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private class PlaceTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... strings) {
            Log.d("Loc", strings + "thi sis uraj");
            String data = null;
            try {
                data = downloadUrl(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            new ParserTask().execute(s);

        }
    }

    private String downloadUrl(String string) throws IOException {
        URL url = new URL(string);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        InputStream stream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        String line = "";
        while ((line = reader.readLine()) != null) {
            builder.append(line);

        }
        String data = builder.toString();
        reader.close();
        return data;

    }

    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {

        @Override
        protected List<HashMap<String, String>> doInBackground(String... strings) {
            JsonParser jsonParser = new JsonParser();
            JSONObject object = null;
            List<HashMap<String, String>> mapList = null;
            try {
                Log.d("Loc", strings[0] + "hello");
                object = new JSONObject(strings[0]);
                mapList = jsonParser.parseResult(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return mapList;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            int min = Integer.MAX_VALUE;
            double latitude = 0, longitude = 0;
            map.clear();
            for (int i = 0; i < hashMaps.size(); i++) {
                HashMap<String, String> hashMapList = hashMaps.get(i);
                double lat = Double.parseDouble(hashMapList.get("lat"));
                double lng = Double.parseDouble(hashMapList.get("lng"));
                float[] results = new float[1];
                Location.distanceBetween(currentLat, currentLong, lat, lng, results);
                int distance = (int) results[0];
                if (distance < min) {
                    min = distance;
                    latitude = lat;
                    longitude = lng;
                }
//                int killometer=(int)(distance/1000);
//                Log.d("TAG",distance+"");
                String name = hashMapList.get("name");
                Log.d("TAG", lat + "," + lng);
            }

            LatLng latLng = new LatLng(latitude, longitude);
            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            map.addMarker(options);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}