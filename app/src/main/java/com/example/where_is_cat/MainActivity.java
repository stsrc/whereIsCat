package com.example.where_is_cat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
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
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private List<BluetoothGattService> mServices;
    private SensorManager mSensorManager;
    private  float[] mAccelerometerReading;
    private float[] mMagnetometerReading;
    private float mCurrentDegree;
    private ImageView mArrow;
    private boolean mRealGps;
    private TextView mDistance;
    private double mMyLatitude;
    private double mMyLongitude;
    private double mCatsLatitude;
    private double mCatsLongitude;
    private double mCatsLatitudeProposition;
    private double mCatsLongitudeProposition;
    private GPSTracker mGpsTracker;
    private int mCommandState;
    private String mString;
    private char mChecksum;
    private char mChecksumSent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("---> <---", "onCreate()");
        mCommandState = 0;

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
        mDistance = (TextView) findViewById(R.id.textView11);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mRealGps = false;

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        SharedPreferences sharedPreferences = getSharedPreferences("where-is-cat", MODE_PRIVATE);

        if (extras != null) {
            if (extras.containsKey("realgps")) {
                mRealGps = extras.getBoolean("realgps");
            }
        } else {
            mRealGps = sharedPreferences.getBoolean("gps", false);
        }

        periodicMyGpsCheck = new Handler();
        final Runnable runnableGps = new Runnable() {
            public void run() {
                periodicMyGpsCheck.postDelayed(this, 1000);
                Location location;
                if (mRealGps)
                    location = mGpsTracker.GetGpsLocation();
                else
                    location = mGpsTracker.GetNetworkLocation();

                if (location != null) {
                    String mode;
                    if (mRealGps)
                        mode = "RealGPS";
                    else
                        mode = "Network";

                    mMyLatitude = location.getLatitude();
                    mMyLongitude = location.getLongitude();

                    if (mCatsLatitude != 0.0 && mCatsLongitude != 0.0) {
                        double result = calculateDistance(mMyLatitude, mMyLongitude, mCatsLatitude, mCatsLongitude);
                        DecimalFormat df2 = new DecimalFormat("#.##");
                        mDistance.setText(df2.format(result));
                    }
                    mMyGps.setText(Double.toString(mMyLatitude) + " " + Double.toString(mMyLongitude) + " [" + mode + "]");
                }
            }
        };
        periodicMyGpsCheck.post(runnableGps);

        periodicMyOrientationCheck = new Handler();
        final Runnable runnable = new Runnable() {
            public void run() {
                periodicMyOrientationCheck.postDelayed(this, 200);
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
                int bearing = (int) calculateBearing(mMyLatitude, mMyLongitude, mCatsLatitude, mCatsLongitude);
                mArrow.setRotation(-azimuth + bearing);
            }
        };
        periodicMyOrientationCheck.post(runnable);
    }

    protected void onResume() {
         super.onResume();
         Log.d("---> <---", "onResume()");
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
        mGpsTracker = new GPSTracker(this);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (intent != null) {
            if (extras != null && extras.containsKey("bluetooth")) {
                mBluetoothDevice = intent.getParcelableExtra("bluetooth");
                mBluetoothTextView.setText(mBluetoothDevice.getAddress() + " " + mBluetoothDevice.getName());
                mBluetoothGatt = mBluetoothDevice.connectGatt(this, true, gattCallback);
            }
        }

    }

    protected void onPause() {
        Log.d("---> <---", "onPause()");
         super.onPause();
         mSensorManager.unregisterListener(this);
        closeBt();
        mGpsTracker.closeGps();
    }

    protected void onDestroy() {
        Log.d("---> <---", "onDestroy()");
        super.onDestroy();

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
                //TODO here?
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

        private double calculateLatLon(String value) {
            double f = Double.parseDouble(value);
            int firstTwoDigits = ((int) f) / 100;
            double nextTwoDigits = f - (double) (firstTwoDigits * 100);
            double finalAnswer = (double)firstTwoDigits + nextTwoDigits / 60.0;
            return finalAnswer;
        }
        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String character = characteristic.getStringValue(0);
            if (character == null)
                return;

            if (mCommandState == 0) {
                if (character.equals("*")) {
                    mChecksumSent = 0;
                    mCommandState = 1;
                    String[] arrOfStr = mString.split(",", 0);
                    if (arrOfStr != null && arrOfStr[1] != null) {
                        mCatsLatitudeProposition = calculateLatLon(arrOfStr[1]);
                    }
                    if (arrOfStr != null && arrOfStr[3] != null) {
                        mCatsLongitudeProposition = calculateLatLon(arrOfStr[3]);
                    }
                } else {
                    mString += character;
                    if (character.equals("$"))
                        mChecksum = 0;
                    else {
                        int part = character.charAt(0);
                        mChecksum = (char) (mChecksum ^ (char) part);
                    }
                }
            } else {
                mCommandState++;
                int checksumPart =  Integer.valueOf(character, 16);
                mChecksumSent = (char) ((mChecksumSent << 4) | checksumPart);

                if (mCommandState == 3) {
                    if (mChecksum == mChecksumSent) {
                        mCatsLatitude = mCatsLatitudeProposition;
                        mCatsLongitude = mCatsLongitudeProposition;
                        mCatGps.setText(Double.toString(mCatsLatitude) + " " + Double.toString(mCatsLongitude));
                    } else {
                        mCatGps.setText(Double.toString(mCatsLatitude) + " " + Double.toString(mCatsLongitude) + ". Wrong checksum");
                    }
                    mCommandState = 0;
                    mString = "";
                }
            }
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

    public void closeBt() {
        if (mBluetoothGatt == null)
            return;

        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private double calculateDistance(double myLatitude, double myLongitude, double catsLatitude, double catsLongitude) {
        double earthRadiusM = 6378137;
        double dLatitudeRadians = Math.toRadians(catsLatitude - myLatitude);
        double dLongitudeRadians = Math.toRadians(catsLongitude - myLongitude);
        double myLatitudeRadians = Math.toRadians(myLatitude);
        double catsLatitudeRadians = Math.toRadians(catsLatitude);
        double a = Math.sin(dLatitudeRadians / 2) * Math.sin(dLatitudeRadians / 2) +
                Math.sin(dLongitudeRadians / 2) * Math.sin(dLongitudeRadians / 2) *
                        Math.cos(myLatitudeRadians) * Math.cos(catsLatitudeRadians);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusM * c;
    }

    private int calculateBearing(double myLatitude, double myLongitude, double catsLatitude, double catsLongitude) {
        double teta1 = Math.toRadians(myLatitude);
        double teta2 = Math.toRadians(catsLatitude);
        double delta2 =  Math.toRadians(catsLongitude - myLongitude);
        double y = Math.sin(delta2) * Math.cos(teta2);
        double x = Math.cos(teta1) * Math.sin(teta2) - Math.sin(teta1) * Math.cos(teta2) * Math.cos(delta2);
        double bearing = Math.atan2(y, x);
        bearing = Math.toDegrees(bearing);
        return((int) bearing) + 360 % 360;
    }

    public class GPSTracker extends Service implements LocationListener {
        private LocationManager mLocationManager;
        private Context mContext;
        private Location mGpsLocation;
        private Location mNetworkLocation;

        public Location GetGpsLocation() { return mGpsLocation; }
        public Location GetNetworkLocation() { return mNetworkLocation; }
        public void closeGps() {
            mLocationManager.removeUpdates(this);
        }
        GPSTracker(Context context) {

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mContext = context;
            mLocationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            boolean isGpsEnabled = mLocationManager.isProviderEnabled(mLocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = mLocationManager.isProviderEnabled(mLocationManager.NETWORK_PROVIDER);
            if (isGpsEnabled) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            }

            if (isNetworkEnabled) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
            }

        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            Log.d("---> <---", location.toString());
            if (location.getProvider().equals(mLocationManager.GPS_PROVIDER)) {
                mGpsLocation = location;
            } else {
                mNetworkLocation = location;
            }
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            Log.d("---> <---", provider);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Log.d("---> <---", provider);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    };
}