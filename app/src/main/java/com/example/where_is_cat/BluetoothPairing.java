package com.example.where_is_cat;


import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class BluetoothPairing<onSaveInstanceState> extends AppCompatActivity implements View.OnClickListener {
    private Button buttonPrevious;
    private TextView mTextView;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private RadioGroup mRadioGroup;
    private ArrayList<BluetoothDevice> mLeDevices;
    private Button buttonOk;
    private RadioGroup mRadioGroupGps;
    private RadioGroup mRadioGroupColors;
    private int mColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pairing);
        buttonPrevious = (Button) findViewById(R.id.button2);
        buttonOk = (Button) findViewById(R.id.button3);
        buttonPrevious.setOnClickListener(BluetoothPairing.this);
        buttonOk.setOnClickListener(BluetoothPairing.this);
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mRadioGroupGps = (RadioGroup) findViewById(R.id.radioGroup3);
        mRadioGroupColors = (RadioGroup) findViewById(R.id.radioGroup4);
        mTextView = findViewById(R.id.textView);

        RadioButton button = findViewById(R.id.radioButton3);
        SharedPreferences sharedPreferences = getSharedPreferences("where-is-cat", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("gps", false)) {
            button.setChecked(true);
        } else {
            button.setChecked(false);
        }

        RadioButton buttonColorsBlack = findViewById(R.id.radioButton4);
        RadioButton buttonColorsWhite = findViewById(R.id.radioButton);

        View view = findViewById(R.id.button2);
        View root = view.getRootView();

        if (sharedPreferences.getString( "Colors", "Black").equals("Black")) {
            buttonColorsBlack.setChecked(true);
            mColor = getResources().getColor(android.R.color.darker_gray);
            root.setBackgroundColor(getResources().getColor(android.R.color.black));

        } else {
            buttonColorsBlack.setChecked(false);
            mColor = getResources().getColor(android.R.color.black);
            root.setBackgroundColor(getResources().getColor(android.R.color.white));
        }

        ((TextView) findViewById(R.id.textView)).setTextColor(mColor);
        ((TextView) findViewById(R.id.textView10)).setTextColor(mColor);
        ((TextView) findViewById(R.id.textView14)).setTextColor(mColor);
        ((RadioButton) findViewById(R.id.radioButton2)).setTextColor(mColor);
        ((RadioButton) findViewById(R.id.radioButton3)).setTextColor(mColor);
        ((RadioButton) findViewById(R.id.radioButton)).setTextColor(mColor);
        ((RadioButton) findViewById(R.id.radioButton4)).setTextColor(mColor);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        mBluetoothLeScanner = mBluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(leScanCallback);

        mHandler = new Handler();
        mLeDevices = new ArrayList<BluetoothDevice>();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeScanner.stopScan(leScanCallback);
            }
        }, 10000);
    }
    private void addRadioButton(String text) {
        RadioButton button = new RadioButton(this);
        button.setText(text);
        button.setTextColor(mColor);

        ColorStateList colorList = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled}, //disabled
                        new int[]{android.R.attr.state_enabled} //enabled
                },
                new int[] {
                        Color.BLACK //disabled
                        ,Color.GRAY //enabled
                }
        );

        button.setButtonTintList(colorList);
        mRadioGroup.addView(button);
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (!mLeDevices.contains(result.getDevice())) {
                        mLeDevices.add(result.getDevice());
                        addRadioButton(result.getDevice().getAddress() + " " +
                                result.getDevice().getName());
                    }
                }
            };

    @Override
    public void onClick(View v) {
        mBluetoothLeScanner.stopScan(leScanCallback);

        Intent intent = new Intent(this, MainActivity.class);
        switch (v.getId()) {
            case R.id.button3:
                int selectedId = mRadioGroup.getCheckedRadioButtonId();
                int selectedGpsId = mRadioGroupGps.getCheckedRadioButtonId();
                int selectedColorsId = mRadioGroupColors.getCheckedRadioButtonId();
                RadioButton selectedButton = (RadioButton) findViewById(selectedId);
                RadioButton selectedGpsButton = (RadioButton) findViewById(selectedGpsId);
                RadioButton selectedColorsButton = (RadioButton) findViewById(selectedColorsId);

                if (selectedButton != null) {
                    CharSequence buttonText = selectedButton.getText();
                    StringTokenizer tokenizer =  new StringTokenizer((String) buttonText);
                    CharSequence address = tokenizer.nextToken();
                    for (BluetoothDevice device : mLeDevices) {
                        if (device.getAddress().equalsIgnoreCase((String) address)) {
                            mTextView.setText(device.getAddress() + "@" + device.getName());
                            intent.putExtra("bluetooth", device);
                        }
                    }
                }

                SharedPreferences sharedPreferences = getSharedPreferences("where-is-cat", MODE_PRIVATE);
                SharedPreferences.Editor shEditor = sharedPreferences.edit();
                if (selectedGpsButton != null) {
                    CharSequence text = selectedGpsButton.getText();
                    if (text.equals("Network")) {
                        intent.putExtra("realgps", false);
                        shEditor.putBoolean("gps", false);
                    } else {
                        intent.putExtra("realgps", true);
                        shEditor.putBoolean("gps", true);
                    }

                }

                if (selectedColorsButton != null) {
                    intent.putExtra("Colors",  (String) selectedColorsButton.getText());
                    shEditor.putString("Colors", (String) selectedColorsButton.getText());
                }
                shEditor.commit();
            default:

        }
        startActivity(intent);
    }
};