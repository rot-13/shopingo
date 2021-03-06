package com.iic.shopingo.services.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.iic.shopingo.ShopingoApplication;

/**
 * Created by assafgelber on 3/5/15.
 */
public class CurrentLocationProvider
    implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private LocationManager locationManager;

  private GoogleApiClient googleApiClient;

  private LocationUpdatesListener updatesListener;

  private long requestInterval;

  public CurrentLocationProvider(Context context, long requestInterval, LocationUpdatesListener listener) {
    this.requestInterval = requestInterval;
    this.updatesListener = listener;
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);



    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
    if (resultCode == ConnectionResult.SUCCESS) {
      googleApiClient =
          new GoogleApiClient.Builder(context).addApi(LocationServices.API).addConnectionCallbacks(this).build();
      googleApiClient.connect();
    } else if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
      GooglePlayServicesUtil.showErrorDialogFragment(resultCode, (android.app.Activity) context, 0);
    } else {
      throw new RuntimeException("Google play services are not installed/enabled in this device");
    }
  }

  @Override
  public void onConnected(Bundle bundle) {
    Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    if (lastLocation == null) {
      lastLocation = locationManager.getLastKnownLocation(ShopingoApplication.MOCK_LOCATION_PROVIDER);
    }

    if (lastLocation != null) {
      updatesListener.onLocationUpdated(lastLocation);
    }

    LocationRequest locationRequest = new LocationRequest();
    locationRequest.setInterval(requestInterval);
    locationRequest.setFastestInterval(requestInterval);
    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
  }

  @Override
  public void onConnectionSuspended(int i) {
    stop();
    updatesListener.onConnectionStop();
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    updatesListener.onConnectionFail();
  }

  @Override
  public void onLocationChanged(Location location) {
    updatesListener.onLocationUpdated(location);
  }

  public void stop() {
    LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
  }

  public interface LocationUpdatesListener {
    public void onLocationUpdated(Location location);

    public void onConnectionStop();

    public void onConnectionFail();
  }
}
