package com.example.where_is_cat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnClickMe;
    private TextView mBluetoothTextView;
    private TextView mMyGps;
    private TextView mCatGps;
    private Handler periodicMyGpsCheck;
    private Location mGpsLocation;
    private Location mPhoneLocation;
    private LocationManager mLocationManager;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private List<BluetoothGattService> mServices;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnClickMe = (Button) findViewById(R.id.button);
        btnClickMe.setOnClickListener(MainActivity.this);
        mMyGps = (TextView) findViewById(R.id.textView3);
        mBluetoothTextView = (TextView) findViewById(R.id.textView6);
        mBluetoothTextView.setText("Not connected");

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
        } else {
            periodicMyGpsCheck = new Handler();
            final Runnable runnable = new Runnable() {
                @SuppressLint("MissingPermission")
                public void run() {
                    periodicMyGpsCheck.postDelayed(this, 10000);
                //TODO set is as option
                    // idk which one will be better...
                /*mGpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (mGpsLocation != null) {
                    mMyGps.setText(mGpsLocation.getLatitude() + " " + mGpsLocation.getLongitude());
                 }*/
                    mPhoneLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (mPhoneLocation != null) {
                        mMyGps.setText(mPhoneLocation.getLatitude() + " " + mPhoneLocation.getLongitude());
                    }
                }
            };
            periodicMyGpsCheck.post(runnable);
        }


        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("bluetooth")) {
                mBluetoothDevice = intent.getParcelableExtra("bluetooth");
                mBluetoothTextView.setText(mBluetoothDevice.getAddress() + " " + mBluetoothDevice.getName());
                mBluetoothGatt = mBluetoothDevice.connectGatt(this, true, gattCallback);
            }
        }
    }

    private final BluetoothGattCallback gattCallback =  new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.d("bluetooth", "newState = " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("bluetooth", "Connected to GATT server.");
                Log.i("bluetooth", "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("bluetooth", "Disconnected from GATT server.");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.w("bluetooth", "onServicesDiscovered received: " + status);
            mServices = gatt.getServices();
            for (BluetoothGattService service : mServices) {
                Log.w("bluetooth", service.getUuid().toString());
            }
            BluetoothGattService service = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristics().get(0);
                if (characteristic != null)
                    gatt.setCharacteristicNotification(characteristic, true);
                else
                    Log.w("bluetooth", "characteristic is null");
            } else {
                Log.w("bluetooth", "serivce is null");
            }
        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.w("bluetooth", characteristic.getStringValue(0));
        }



        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("bluetooth", "onCharacteristicRead ");
            }
        }
    };

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, BluetoothPairing.class);
        startActivity(intent);
    }
}