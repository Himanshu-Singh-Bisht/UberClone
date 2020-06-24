package com.himanshu.uberclone;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PassengerActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Button btnRequestCar;

    private boolean isUberCancelled = true;

    private Button btnBeep;

    private boolean isCarReady = false;

    private Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setTitle("Passenger");
        btnRequestCar = findViewById(R.id.btnRequestCar);
        btnRequestCar.setOnClickListener(this);



        // TO SHOW THAT IF THE USER HAS ORDERED THE UBER OR NOT WHEN THE APP STARTS AGAIN.
        ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
        carRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (objects.size() > 0 && e == null) {
                    isUberCancelled = false;
                    btnRequestCar.setText("Cancel Uber Request");
                }
            }
        });


        btnBeep = findViewById(R.id.btnBeep);
        btnBeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDriverUpdates();
            }
        });


        Button btnLogoutFromPassenger = findViewById(R.id.btnLogoutFromPassenger);
        btnLogoutFromPassenger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e == null)
                        {
                            finish();
                        }
                    }
                });
            }
        });
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
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);     // as LocationManager is a service which can be used by using current class's activity instance.
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                updateCameraPassengerLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


        if (Build.VERSION.SDK_INT < 23)      // then user permission is not required
        {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        }
        else if(Build.VERSION.SDK_INT >= 23)        // then permission is required.
        {
            if(ContextCompat.checkSelfPermission(PassengerActivity.this ,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)     // permission not granted, then ask for permission
            {
                ActivityCompat.requestPermissions(PassengerActivity.this ,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1000);         // Asking for one permission to access location

            }
            else            // if permission is given
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER , 0 , 0  , locationListener);

                Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateCameraPassengerLocation(currentPassengerLocation);
            }
        }
    }



    // TO SEE WHETHER PERMISSION IS GIVEN OR NOT
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            if(ContextCompat.checkSelfPermission(PassengerActivity.this ,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateCameraPassengerLocation(currentPassengerLocation);
            }
        }

    }


    private void updateCameraPassengerLocation(Location pLocation)
    {
        if(!isCarReady) {
            LatLng passengerLocation = new LatLng(pLocation.getLatitude(), pLocation.getLongitude());

            // NOW UPDATE OUR LOCATION
            mMap.clear();       // to clear previously stored location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerLocation, 10));         // Zoom used to zoom in a little bit.
            mMap.addMarker(new MarkerOptions().position(passengerLocation).title("This is your location!!")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));              // to change the color of the marker just deal with the icon.
        }
    }


    // onClick for the button.
    @Override
    public void onClick(View v)
    {
        if(isUberCancelled)             // button was clicked when uber is cancelled (means we can request a car).
        {
            // Firstly , check whether has given us the permission to access the location or not.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location currentPassengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (currentPassengerLocation != null) {
                    ParseObject requestCar = new ParseObject("RequestCar");

                    requestCar.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint userLocation = new ParseGeoPoint(currentPassengerLocation.getLatitude(), currentPassengerLocation.getLongitude());

                    requestCar.put("passengerLocation", userLocation);
                    requestCar.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(PassengerActivity.this, "A car request is Send.", Toast.LENGTH_SHORT).show();
                                btnRequestCar.setText("Cancel the Uber Order");
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(this, "Unknown Error! something went wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else                // uber isn't cancelled and button is tapped so now cancel uber request.
        {
            ParseQuery<ParseObject> carRequestQuery = ParseQuery.getQuery("RequestCar");
            carRequestQuery.whereEqualTo("username" , ParseUser.getCurrentUser().getUsername());
            carRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> requestList, ParseException e) {
                    if(requestList.size() > 0 && e == null)
                    {
                        isUberCancelled = false;
                        btnRequestCar.setText("Request A Car");

                        for(ParseObject uberRequest : requestList)
                        {
                            uberRequest.deleteInBackground(new DeleteCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if(e == null)
                                    {
                                        Toast.makeText(PassengerActivity.this , "Request/s Deleted." , Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }


    // for letting the passenger know that driver accepts the request.
    private void getDriverUpdates() {
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run()
            {
                ParseQuery<ParseObject> uberRequestQuery = ParseQuery.getQuery("RequestCar");
                uberRequestQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

                uberRequestQuery.whereEqualTo("requestAccepted", true);
                uberRequestQuery.whereExists("driverOfMe");

                uberRequestQuery.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (objects.size() > 0 && e == null) {
                            for (final ParseObject requestObject : objects) {
//                        Toast.makeText(PassengerActivity.this , "Hurray! " + requestObject.get("driverOfMe") + " wants to give you a ride." , Toast.LENGTH_SHORT).show();

                                ParseQuery<ParseUser> driverQuery = ParseUser.getQuery();
                                driverQuery.whereEqualTo("username", requestObject.getString("driverOfMe"));
                                driverQuery.findInBackground(new FindCallback<ParseUser>() {
                                    @Override
                                    public void done(List<ParseUser> driver, ParseException e) {
                                        if (driver.size() > 0 && e == null) {
                                            for (ParseUser driverOfRequest : driver) {
                                                ParseGeoPoint driverOfRequestLocation = driverOfRequest.getParseGeoPoint("driverLocation");

                                                if (ContextCompat.checkSelfPermission(PassengerActivity.this,
                                                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                                    Location passengerLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                                    ParseGeoPoint pLocationParseGeoPoint = new ParseGeoPoint(passengerLocation.getLatitude()
                                                            , passengerLocation.getLongitude());

                                                    double milesDistance = driverOfRequestLocation.distanceInMilesTo(pLocationParseGeoPoint);

                                                    if(milesDistance < 0.3)
                                                    {
                                                        requestObject.deleteInBackground(new DeleteCallback() {
                                                            @Override
                                                            public void done(ParseException e) {
                                                                Toast.makeText(PassengerActivity.this , "Your Uber is ready!!" , Toast.LENGTH_SHORT).show();
                                                                isCarReady = false;     // as car is reached the location.
                                                                isUberCancelled = true;     // as we can order the new uber
                                                                btnRequestCar.setText("You can request a new Uber.");
                                                            }
                                                        });

                                                    }
                                                    else {
                                                        float roundedDistance = Math.round(milesDistance * 10) / 10;

                                                        Toast.makeText(PassengerActivity.this, requestObject.getString("driverOfMe") +
                                                                " is " + roundedDistance + " miles away from you." + "Please Wait!", Toast.LENGTH_SHORT).show();


                                                        // MARKER FOR DRIVER LOCATION
                                                        LatLng dLocation = new LatLng(driverOfRequestLocation.getLatitude(), driverOfRequestLocation.getLongitude());

                                                        // MARKER FOR PASSENGER LOCATION
                                                        LatLng pLocation = new LatLng(pLocationParseGeoPoint.getLatitude(), pLocationParseGeoPoint.getLongitude());

                                                        // TO GET BOTH DRIVER AND PASSENGER LOCATION MARKERS SIMULTANEOUSLY
                                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                        Marker driverMarker = mMap.addMarker(new MarkerOptions().position(dLocation).title("Driver Location"));
                                                        Marker passengerMarker = mMap.addMarker(new MarkerOptions().position(pLocation).title("Passenger Location"));

                                                        ArrayList<Marker> myMarkers = new ArrayList<>();
                                                        myMarkers.add(driverMarker);
                                                        myMarkers.add(passengerMarker);

                                                        for (Marker marker : myMarkers) {
                                                            builder.include(marker.getPosition());
                                                        }

                                                        LatLngBounds bounds = builder.build();
                                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 10);
                                                        mMap.animateCamera(cameraUpdate);
                                                    }
                                                }

                                            }
                                        }
                                    }
                                });
                            }
                        } else {
                            isCarReady = true;
                        }
                    }
                });

            }
        } , 0 , 3000);          //It will be repeated every 3 seconds
    }
}