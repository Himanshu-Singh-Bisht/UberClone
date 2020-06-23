package com.himanshu.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.LogOutCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class DriverRequestListActivity extends AppCompatActivity implements View.OnClickListener  , AdapterView.OnItemClickListener {

    private Button btnGetRequest;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private ListView listView;
    private ArrayList<String> nearbyDriveRequests;
    private ArrayAdapter adapter;

    private ArrayList<Double> passengersLatitude;
    private ArrayList<Double> passengersLongitude;

    private ArrayList<String> requestCarUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_request_list);

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        // as LocationManager is a service which can be used by using current class's activity instance.

        btnGetRequest = findViewById(R.id.btnGetRequests);
        btnGetRequest.setOnClickListener(this);

        passengersLatitude = new ArrayList<>();
        passengersLongitude = new ArrayList<>();

        requestCarUsername = new ArrayList<>();

        listView = findViewById(R.id.requestListView);
        nearbyDriveRequests = new ArrayList<>();
        adapter = new ArrayAdapter(this , android.R.layout.simple_list_item_1 , nearbyDriveRequests);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        nearbyDriveRequests.clear();

        // to see if permission is given by user to access the fine location.
        if(Build.VERSION.SDK_INT < 23 ||
                ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER , 0 , 0 , locationListener);
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
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu , menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.driverLogoutItem);
        {
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v)
    {

        if (Build.VERSION.SDK_INT < 23)      // then user permission is not required
        {

            Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateRequestListView(currentDriverLocation);
        }
        else if(Build.VERSION.SDK_INT >= 23)        // then permission is required.
        {
            if(ContextCompat.checkSelfPermission(DriverRequestListActivity.this ,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)     // permission not granted, then ask for permission
            {
                ActivityCompat.requestPermissions(DriverRequestListActivity.this ,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1000);         // Asking for one permission to access location

            }
            else            // if permission is given
            {

                // as we have already asked for requestLocationUpdates inside onCreate() so not needed here.
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER , 0 , 0  , locationListener);

                Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateRequestListView(currentDriverLocation);
            }
        }
    }




    // TO SEE WHETHER PERMISSION IS GIVEN OR NOT
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            if(ContextCompat.checkSelfPermission(DriverRequestListActivity.this ,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);


                Location currentDriverLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateRequestListView(currentDriverLocation);
            }
        }

    }




    // here , we only want to update the listView so no need to update the map's location
    private void updateRequestListView(Location driverLocation)
    {
        if(driverLocation != null)
        {

            final ParseGeoPoint driverCurrentLocation = new ParseGeoPoint(driverLocation.getLatitude() , driverLocation.getLongitude());

            final ParseQuery<ParseObject> requestCarQuery = ParseQuery.getQuery("RequestCar");

            requestCarQuery.whereNear("passengerLocation" , driverCurrentLocation);
            requestCarQuery.whereDoesNotExist("driverOfMe");        // as we want to remove the object which already have driver.

            requestCarQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null)
                    {
                        if(objects.size() > 0)
                        {
                            if(nearbyDriveRequests.size() > 0)
                            {
                                nearbyDriveRequests.clear();            // to remove the drive request which are
                                                                        // previously added to the listview (such that they won't repeat).
                            }
                            if(passengersLatitude.size() > 0)
                            {
                                passengersLatitude.clear();
                            }
                            if(passengersLongitude.size() > 0)
                            {
                                passengersLongitude.clear();
                            }
                            if(requestCarUsername.size() > 0)
                            {
                                requestCarUsername.clear();
                            }

                            for(ParseObject nearRequest : objects)
                            {
                                ParseGeoPoint pLocation = (ParseGeoPoint) nearRequest.get("passengerLocation");
                                Double milesDistanceToPassenger = driverCurrentLocation.distanceInMilesTo(pLocation);

                                float roundedDistanceValue = Math.round(milesDistanceToPassenger * 10) /10;


                                nearbyDriveRequests.add("There are " + roundedDistanceValue + " miles to " + nearRequest.getString("username"));

                                passengersLatitude.add(pLocation.getLatitude());
                                passengersLongitude.add(pLocation.getLongitude());

                                requestCarUsername.add(nearRequest.get("username") + "");
                            }
                        }
                        else        // objects.size() == 0
                        {
                            Toast.makeText(DriverRequestListActivity.this , "Sorry , There is no request yet." , Toast.LENGTH_LONG).show();
                        }

                        adapter.notifyDataSetChanged();         // to update the listview via adapter when the arraylist is updated.
                    }
                }
            });
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
//        Toast.makeText(this ,"Clicked" , Toast.LENGTH_SHORT).show();

        if (ContextCompat.checkSelfPermission(this , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            Location cdLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(cdLocation != null)
            {
                Intent intent = new Intent(this , ViewLocationMapActivity.class);

                intent.putExtra("dLatitude" , cdLocation.getLatitude());
                intent.putExtra("dLongitude" , cdLocation.getLongitude());

                intent.putExtra("pLatitude" , passengersLatitude.get(position));
                intent.putExtra("pLongitude" , passengersLongitude.get(position));

                intent.putExtra("rUsername" , requestCarUsername.get(position));
                startActivity(intent);
            }
        }
    }
}