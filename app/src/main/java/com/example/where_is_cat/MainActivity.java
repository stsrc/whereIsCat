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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {
    private Button btnClickMe;
    private TextView mBluetoothTextView;
    private TextView mMyGps;
    private TextView mCatGps;
    private TextView mMyOrientation;
    private Handler periodicMyGpsCheck;
    private Handler periodicMyOrientationCheck;
    private Location mGpsLocation;
    private Location mPhoneLocation;
    private LocationManager mLocationManager;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private List<BluetoothGattService> mServices;
    private SensorManager mSensorManager;
    private  float[] mAccelerometerReading;
    private float[] mMagnetometerReading;
    private float mCurrentDegree;
    private ImageView mArrow;
    private boolean mRealGps;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        btnClickMe = (Button) findViewById(R.id.button);
        btnClickMe.setOnClickListener(this);
        mMyGps = (TextView) findViewById(R.id.textView3);
        mCatGps = (TextView) findViewById(R.id.textView4);
        mMyOrientation = (TextView) findViewById(R.id.textView8);
        mBluetoothTextView = (TextView) findViewById(R.id.textView6);
        mBluetoothTextView.setText("Not connected");
        mAccelerometerReading = new float[3];
        mMagnetometerReading = new float[3];
        mArrow = (ImageView) findViewById(R.id.imageView);
        mCurrentDegree = 0;

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mRealGps = false;

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (intent != null && extras != null) {
            if (extras.containsKey("realgps")) {
                mRealGps = extras.getBoolean("realgps");
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
        } else {
            periodicMyGpsCheck = new Handler();
            final Runnable runnable = new Runnable() {
                @SuppressLint("MissingPermission") //TODO this suppress lint should be removed
                public void run() {
                    periodicMyGpsCheck.postDelayed(this, 10000);

                    if (mRealGps) {
                        mGpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (mGpsLocation != null) {
                            mMyGps.setText(mGpsLocation.getLatitude() + " " + mGpsLocation.getLongitude());
                        }
                    } else {
                        mPhoneLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (mPhoneLocation != null) {
                            mMyGps.setText(mPhoneLocation.getLatitude() + " " + mPhoneLocation.getLongitude());
                        }
                    }
                }
            };
            periodicMyGpsCheck.post(runnable);
        }

        periodicMyOrientationCheck = new Handler();
        final Runnable runnable = new Runnable() {
            public void run() {
                periodicMyOrientationCheck.postDelayed(this, 1000);
                // Rotation matrix based on current readings from accelerometer and magnetometer.
                final float[] rotationMatrix = new float[9];
                mSensorManager.getRotationMatrix(rotationMatrix, null,
                        mAccelerometerReading, mMagnetometerReading);

                // Express the updated rotation matrix as three orientation angles.
                final float[] orientationAngles = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationAngles);

                float azimuth =  (float) (orientationAngles[0] * 180 / Math.PI);
                if (azimuth < 0)
                    azimuth += 360;

                mMyOrientation.setText(Float.toString(azimuth));

                mArrow.setRotation(-azimuth);
            }
        };
        periodicMyOrientationCheck.post(runnable);

        if (intent != null) {
            if (extras != null && extras.containsKey("bluetooth")) {
                mBluetoothDevice = intent.getParcelableExtra("bluetooth");
                mBluetoothTextView.setText(mBluetoothDevice.getAddress() + " " + mBluetoothDevice.getName());
                mBluetoothGatt = mBluetoothDevice.connectGatt(this, true, gattCallback);
            }
        }
    }

    protected void onResume() {
         super.onResume();
         if (mSensorManager != null) {
             Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
             if (accelerometer != null) {
                 mSensorManager.registerListener(this, accelerometer,
                         SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
             }
             Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
             if (magneticField != null) {
                 mSensorManager.registerListener(this, magneticField,
                         SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
             }
       }
    }

    protected void onPause() {
         super.onPause();
         mSensorManager.unregisterListener(this);
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
                Log.w("bluetooth", "service is null");
            }
        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            mCatGps.setText(characteristic.getStringValue(0));
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}