package com.example.junction;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

@SuppressLint("NewApi")
public class LocationsMain extends FragmentActivity implements LocationListener, OnMarkerClickListener {
	static public int locationId = -1;
	private Boolean newLocation = false;
	
	private LocationManager LocManager;
	private Location userLocation;
	private Geocoder myGeocoder;
	private String currentLocation, goLocation;
	
	private MapFragment mapFragment;
	private GoogleMap myMap;
	private SupportMapFragment frag;
	private Marker marker1, marker2;
	private Marker lastOpened = null;
	private LatLngBounds.Builder builder;
 	
	private EditText titleEditText;
	private TextView titleTextView;
	private Button takePhotoButton, starButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_locations_main);
		
		ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.locationTextViewSwitcher1);
		ImageView recentImage = (ImageView) findViewById(R.id.locationImageView);
		titleEditText = (EditText)findViewById(R.id.LocationTitleEditText1);
		titleTextView = (TextView)findViewById(R.id.locationTitleTextView);
		takePhotoButton = (Button) findViewById(R.id.takePhotoButton1);
		takePhotoButton.setOnClickListener(takePhotoButtonListener);
		starButton = (Button) findViewById(R.id.starButton);
		starButton.setOnClickListener(takePhotoButtonListener);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			locationId = extras.getInt("locationId");
			Log.e("main", Integer.toString(locationId));
		}
		
		if (locationId == -1) {
			switcher.showNext();
			starButton.setVisibility(View.INVISIBLE);
			newLocation = true;
		} else {
			String whereClause = "id = ?";
			String[] whereArgs = new String[] { Integer.toString(locationId) };
			
			Cursor locationData = HomeActivity.junctionDB.query("locations", null, whereClause , whereArgs, null, null, null);
			if (locationData.getCount() != 0) {
				int titleColumn = locationData.getColumnIndex("title");
				locationData.moveToFirst();
				titleTextView.setText(locationData.getString(titleColumn));
			}
		}
		
		//
		LocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		Criteria myCriteria = new Criteria();
		myCriteria.setAccuracy(Criteria.NO_REQUIREMENT);
		myCriteria.setPowerRequirement(Criteria.POWER_LOW);
		
		String bestProvider = LocManager.getBestProvider(myCriteria, true);
		userLocation = LocManager.getLastKnownLocation(bestProvider);
		
		LocManager.requestLocationUpdates(bestProvider, 500, 20.0f, this);
		goLocation = "Your destination";
		
		myGeocoder = new Geocoder(this, Locale.CANADA);
		if (userLocation != null) {
			getAddress(userLocation);
		}
		
		
		//Map
		frag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.directionMap);
		myMap = frag.getMap();

		myMap.setMyLocationEnabled(true);
		myMap.setIndoorEnabled(true);
		myMap.setOnMarkerClickListener(this);
		double d = userLocation.getLatitude();
		marker1 = myMap.addMarker(new MarkerOptions().position(new LatLng(userLocation.getLatitude(), userLocation.getLongitude())).title(currentLocation).snippet("You are here"));
		builder = new LatLngBounds.Builder();
		//
		
		//setting photo
		String whereClause = "dateTime in (SELECT max(dateTime) FROM subjects WHERE locationId = ?)";
		String[] whereArgs = new String[] { Integer.toString(locationId) };
		
		Cursor subjectData = HomeActivity.junctionDB.query("subjects", null, whereClause , whereArgs, null, null, null);
		if (subjectData.getCount() != 0) {
			int subjectImageColumn = subjectData.getColumnIndex("image");
			subjectData.moveToFirst();
			byte[] img = subjectData.getBlob(subjectImageColumn);
			
			Bitmap bmp = BitmapFactory.decodeByteArray(img,0,img.length);
			recentImage.setImageBitmap(bmp);
			recentImage.invalidate();
		}
		
		if (locationId != -1) {
			whereClause = "id = ?";
			whereArgs = new String[] { Integer.toString(locationId) };
			Cursor locationData = HomeActivity.junctionDB.query("locations", null, whereClause , whereArgs, null, null, null);
			if (locationData.getCount() != 0) {
				int latColumn = locationData.getColumnIndex("latitude");
				int longColumn = locationData.getColumnIndex("longitude");
				locationData.moveToFirst();
				
				if (!locationData.getString(latColumn).isEmpty()) {
					double latitude = Double.parseDouble(locationData.getString(latColumn));
					double longitude = Double.parseDouble(locationData.getString(longColumn));
					marker2 = myMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("location").snippet(goLocation));
				}
			}
		}
		
	}
	
	
	public void onBackPressed() {
		Intent i = new Intent(getApplicationContext(), HomeActivity.class);
 	    startActivity(i);
	};
	
	View.OnClickListener takePhotoButtonListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (v == takePhotoButton) {
				if (newLocation) {
					ContentValues cv = new ContentValues();
					cv.put("title", titleEditText.getText().toString());
		        	cv.put("locationName", "");
		        	cv.put("latitude", "");
		        	cv.put("longitude", "");
		            HomeActivity.junctionDB.insert("locations", null, cv);
		            
		            Cursor locationData = HomeActivity.junctionDB.query("locations", null, null , null, null, null, null);
		            locationId = locationData.getCount();
				}
				
				Intent i = new Intent(LocationsMain.this, CameraActivity.class);
				i.putExtra("locationId", locationId); 
				startActivity(i);
			}
			
			if (v == starButton) {
				String whereClause = "name = ?";
				String[] whereArgs = new String[] { HomeActivity.username };
				
				Cursor userData = HomeActivity.junctionDB.query("users", null, whereClause , whereArgs, null, null, null);
				if (userData.getCount() != 0) {
					userData.moveToFirst();
					int starIdsColumn = userData.getColumnIndex("starIds");
					String starIds = userData.getString(starIdsColumn);
					
					Boolean inArray = false;
					if (!starIds.isEmpty()) {
						String[] ids = starIds.split(",");
						
						if (ids.length > 0) {
							for (int i = 0; i < ids.length; i++) {
								if (Integer.parseInt(ids[i]) == locationId) {
									inArray = true;
								}
							}
						} 
					}
					
					if (!inArray) {
						if (starIds.length() == 0) {
							starIds = Integer.toString(locationId);
						} else {
							starIds += "," + Integer.toString(locationId);
						}
					}
		            
		            ContentValues cv = new ContentValues();
		        	cv.put("starIds", starIds);
		            HomeActivity.junctionDB.update("users", cv, whereClause, whereArgs);
		            Toast.makeText(getApplicationContext(), "Location added to Stars", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getApplicationContext(), "You need to login before starring a location", Toast.LENGTH_LONG).show();
				}
			}
		}
	};
	
	private void getAddress(Location location) {
		Address myAddress = new Address(Locale.CANADA);
		
		try {
			List<Address> addresses = myGeocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if (addresses != null && !addresses.isEmpty())
			{
				StringBuilder userPlace = new StringBuilder();
				myAddress = addresses.get(0);
				
				for (int i = 0; i < 3; i++) {
					userPlace.append(myAddress.getAddressLine(i) + "\n");
				}
				
				currentLocation = userPlace.toString();
			}
			else{
				currentLocation = "Cannot find current location";
			}
		}
		
		catch (IOException e) {
			currentLocation = (e.getMessage()); 
		} 
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.locations_main, menu);
		return true;
	}

	@Override
	public void onLocationChanged(Location location) {
		userLocation = location;
		builder.include(marker1.getPosition());
		if (marker2 != null) {
			builder.include(marker2.getPosition());
		}
		

		LatLngBounds bounds = builder.build();
		
		int padding = 100; // offset from edges of the map in pixels
		CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
		myMap.moveCamera(cu);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		
		if (lastOpened != null) {
            // Close the info window
            lastOpened.hideInfoWindow();

            // Is the marker the same marker that was already open
            if (lastOpened.equals(marker)) {
                // Nullify the lastOpened object
                lastOpened = null;
                // Return so that the info window isn't opened again
                return true;
            } 
        }

        // Open the info window for the marker
        marker.showInfoWindow();
        // Re-assign the last opened such that we can close it later
        lastOpened = marker;
		
		return true;
	}

}
