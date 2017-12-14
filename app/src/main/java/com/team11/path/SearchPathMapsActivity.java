package com.team11.path;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class SearchPathMapsActivity extends FragmentActivity implements OnMapReadyCallback, DirectionFinderListener {

        private GoogleMap map;
        private EditText startEdit;
        private EditText endEdit;
        private List<Marker> originMarkers = new ArrayList<>();
        private List<Marker> destinationMarkers = new ArrayList<>();
        private List<Polyline> polylinePaths = new ArrayList<>();
        private double latitude;
        private double longitude;
        private String str;
        private List<Address> addressList;
        private Marker currentMarker = null;

        private final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1000;
        private int PLACE_CODE = 1;
        private final int START_PLACE_CODE = 10;
        private final int END_PLACE_CODE = 20;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_search_path_maps);
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);


        }


        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera. In this case,
         * we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to install
         * it inside the SupportMapFragment. This method will only be triggered once the user has
         * installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;

            startEdit = findViewById(R.id.start_edit);
            endEdit = findViewById(R.id.end_edit);
            Button search = findViewById(R.id.path_button);


            startEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if(b){
                        new Handler().postDelayed(new Runnable(){

                            @Override
                            public void run() {
                                placeAutoComplete();
                                PLACE_CODE = START_PLACE_CODE;
                            }
                        }, 1);
                    }
                }
            });

            endEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if(b){
                        new Handler().postDelayed(new Runnable(){

                            @Override
                            public void run() {
                                placeAutoComplete();
                                PLACE_CODE = END_PLACE_CODE;
                            }
                        }, 1);
                    }
                }
            });



            search.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendRequest();
                }
            });

            map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {

                    final AlertDialog alertDialog = new AlertDialog.Builder(SearchPathMapsActivity.this)
                            .setMessage("현재위치 설정")
                            .setPositiveButton("도착지", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    endEdit.setText(addressList.get(0).getAddressLine(0));

                                }
                            })
                            .setNegativeButton("출발지", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startEdit.setText(addressList.get(0).getAddressLine(0));

                                }
                            })
                            .setNeutralButton("취소", null)
                            .create();

                    alertDialog.show();


                    return false;
                }
            });

            LatLng seoul = new LatLng(37.550618, 126.932523);
//        map.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 18));

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            map.setMyLocationEnabled(true);

            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (currentMarker != null) currentMarker.remove();
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        LatLng latLng = new LatLng(latitude, longitude);
                        Geocoder geocoder = new Geocoder(getApplicationContext());
                        try {
                            addressList = geocoder.getFromLocation(latitude, longitude, 1);
                            str = addressList.get(0).getLocality()+",";
                            str+=addressList.get(0).getCountryName();
                            currentMarker = map.addMarker(new MarkerOptions().position(latLng).title(str));
                            //        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {

                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }
                });
            }
            else if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (currentMarker != null)
                            currentMarker.remove();
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        LatLng latLng = new LatLng(latitude, longitude);
                        Geocoder geocoder = new Geocoder(getApplicationContext());
                        try {
                            addressList = geocoder.getFromLocation(latitude, longitude, 1);
                            str = addressList.get(0).getLocality()+",";
                            str+=addressList.get(0).getAddressLine(0);
                            currentMarker = map.addMarker(new MarkerOptions().position(latLng).title(str));
                            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    @Override
                    public void onProviderEnabled(String s) {

                    }

                    @Override
                    public void onProviderDisabled(String s) {

                    }
                });
            }






//         Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        }

    private void placeAutoComplete (){
        try{
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .build(SearchPathMapsActivity.this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);

        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

    }

    public void onPlaceSelected(Place place){

        if(PLACE_CODE == START_PLACE_CODE){
            startEdit.setText(place.getName());
        }

        if(PLACE_CODE == END_PLACE_CODE){
            endEdit.setText(place.getName());

        }

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                this.onPlaceSelected(place);

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled operation.
            }
        }
    }

    public void sendRequest(){
//        Uri uri = Uri.parse("https://maps.googleapis.com/maps/api/directions/json?origin=Brooklyn&destination=Queens&mode=transit&key=AIzaSyBjll9Cro-BivWa4MEgfTVQbaDUINajF1Q");
//        Intent it = new Intent(Intent.ACTION_VIEW, uri);
//        startActivity(it);

        String origin = startEdit.getText().toString();
        String destination = endEdit.getText().toString();

        if(origin.isEmpty()){
            return;
        }
        if(destination.isEmpty()){
            return;
        }

        try {
            new DirectionFinder(this, origin, destination).execute();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }



    }

    @Override
    public void onDirectionFinderStart() {
        if(originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if(destinationMarkers != null){
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if(polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
            ((TextView) findViewById(R.id.duration_textView)).setText(route.duration.text);
            ((TextView) findViewById(R.id.distance_textView)).setText(route.distance.text);

            originMarkers.add(map.addMarker(new MarkerOptions()
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(map.addMarker(new MarkerOptions()
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(R.color.colorAccent).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(map.addPolyline(polylineOptions));
        }

    }


}