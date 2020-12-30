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
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class BluetoothPairing extends AppCompatActivity implements View.OnClickListener {
    private Button buttonPrevious;
    private TextView mTextView;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private RadioGroup mRadioGroup;
    private ArrayList<BluetoothDevice> mLeDevices;
    private Button buttonOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pairing);
        buttonPrevious = (Button) findViewById(R.id.button2);
        buttonOk = (Button) findViewById(R.id.button3);
        buttonPrevious.setOnClickListener(BluetoothPairing.this);
        buttonOk.setOnClickListener(BluetoothPairing.this);
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        mTextView = findViewById(R.id.textView);

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
        switch (v.getId()) {
            case R.id.button2:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.button3:
                int selectedId = mRadioGroup.getCheckedRadioButtonId();
                RadioButton selectedButton = (RadioButton) findViewById(selectedId);
                if (selectedButton == null) {
                    intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                } else {
                    CharSequence buttonText = selectedButton.getText();
                    StringTokenizer tokenizer =  new StringTokenizer((String) buttonText);
                    CharSequence address = tokenizer.nextToken();
                    for (BluetoothDevice device : mLeDevices) {
                        if (device.getAddress().equalsIgnoreCase((String) address)) {
                            mTextView.setText(device.getAddress() + "@" + device.getName());
                            intent = new Intent(this, MainActivity.class);
                            intent.putExtra("bluetooth", device);
                            startActivity(intent);
                        }
                    }
                }
            default:
        }
    }
};