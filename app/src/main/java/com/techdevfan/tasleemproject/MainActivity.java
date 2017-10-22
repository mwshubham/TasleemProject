package com.techdevfan.tasleemproject;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.techdevfan.tasleemproject.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    public static final int RC_CAMERA_PICK_IMAGE = 101;

    public static final int PERM_ALL = 200;
    public static final int PERM_CAMERA_PICK_IMAGE = 201;
    public static final int PERM_ACCESS_LOCATION = 202;

    private static final String TAG = "MainActivity";
    private ActivityMainBinding mBinding;

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager mLocationManager;

    private static final long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private static final long FASTEST_INTERVAL = 2000; /* 2 sec */

    //    private StorageReference mStorageRef;
    private FirebaseStorage mFirebaseStorage = FirebaseStorage.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        init();
        buildGoogleApiClient();
        initControlListener();
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    }

    private void initControlListener() {
        Log.d(TAG, "initControlListener: ");
        mBinding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.CAMERA)) {
                        Snackbar.make(mBinding.getRoot(), "Need camera access to pick image.", Snackbar.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, PERM_CAMERA_PICK_IMAGE);
                    }
                } else {
                    pickImageFromCamera();
                }
            }
        });
    }

    private void init() {
        setSupportActionBar(mBinding.toolbar);
        mBinding.etOverlayText.addTextChangedListener(new InputTextWatcher());
        mBinding.btnImageUpload.setOnClickListener(new UploadOnClickListerner());
//        mStorageRef = FirebaseStorage.getInstance().getReference();
//        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, PERM_ALL);
    }

    private void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient: ");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void pickImageFromCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, RC_CAMERA_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult requestCode: " + requestCode);
        Log.d(TAG, "onActivityResult resultCode: " + resultCode);

        switch (requestCode) {
            case RC_CAMERA_PICK_IMAGE:
                switch (resultCode) {
                    case RESULT_OK:
                        if (data != null && data.getExtras() != null && data.getExtras().containsKey("data")) {
                            Bitmap photo = (Bitmap) data.getExtras().get("data");
                            mBinding.ivCaptureImage.setImageBitmap(photo);

                            mBinding.flImageContainer.setVisibility(View.VISIBLE);
                            mBinding.etOverlayText.setVisibility(View.VISIBLE);
                            mBinding.btnImageUpload.setVisibility(View.VISIBLE);

                            mBinding.tvLabel.setVisibility(View.GONE);
                        } else {
                            Snackbar.make(mBinding.getRoot(), getString(R.string.error_something_went_wrong_try_again), Snackbar.LENGTH_LONG).show();
                        }
                        break;
                }
                break;


        }
    }

    private class InputTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mBinding.tvOverlayText.setVisibility(s.length() > 0 ? View.VISIBLE : View.INVISIBLE);
            mBinding.tvOverlayText.setText(s.toString());
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult requestCode: " + requestCode);
        switch (requestCode) {
            case PERM_CAMERA_PICK_IMAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromCamera();
                } else {
                    Snackbar.make(mBinding.getRoot(), "Need camera access to pick image.", Snackbar.LENGTH_LONG).show();
                }
            }
            break;

            case PERM_ACCESS_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    Snackbar.make(mBinding.getRoot(), "Need location access.", Snackbar.LENGTH_LONG).show();
                }
            }
            break;

            case PERM_ALL:
                // do nothing ...
                break;

        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected: ");
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorMessage());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates: ");
        if (!isLocationEnabled()) {
            showAlert();
            return;
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERM_ACCESS_LOCATION);
            return;
        }

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation != null) {
            Log.d(TAG, "startLocationUpdates: Location: " + Double.toString(mLocation.getLatitude()) + "," + Double.toString(mLocation.getLongitude()));
        } else {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(UPDATE_INTERVAL)
                    .setFastestInterval(FASTEST_INTERVAL);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: Updated Location: " + Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()));
        mLocation = location;
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return mLocationManager != null && (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private class UploadOnClickListerner implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mBinding.flImageContainer.setDrawingCacheEnabled(true);
            mBinding.flImageContainer.buildDrawingCache();
            Bitmap bitmap = mBinding.flImageContainer.getDrawingCache();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            mBinding.flImageContainer.setDrawingCacheEnabled(false);
            byte[] data = byteArrayOutputStream.toByteArray();
            String path = "tempfile/" + UUID.randomUUID() + ".png";
            StorageReference storageReference = mFirebaseStorage.getReference(path);

            StorageMetadata.Builder metadataBuilder = new StorageMetadata.Builder();
            metadataBuilder.setCustomMetadata("Overlay Text", mBinding.tvOverlayText.getText().toString());
            if (mLocation != null) {
                metadataBuilder.setCustomMetadata("Latitude", String.valueOf(mLocation.getLatitude()));
                metadataBuilder.setCustomMetadata("Longitude", String.valueOf(mLocation.getLongitude()));
            }

            mBinding.progressBar.setVisibility(View.VISIBLE);
            mBinding.btnImageUpload.setEnabled(false);
            UploadTask uploadTask = storageReference.putBytes(data, metadataBuilder.build());
            uploadTask.addOnSuccessListener(new MainActivity(), new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Snackbar.make(mBinding.getRoot(), "Image Uploaded Successfully !", Snackbar.LENGTH_LONG).show();

                    mBinding.progressBar.setVisibility(View.GONE);
                    mBinding.btnImageUpload.setEnabled(true);

                    Uri uri = taskSnapshot.getDownloadUrl();
                    Log.d(TAG, "onSuccess: " + uri);
                }
            });
        }
    }
}
