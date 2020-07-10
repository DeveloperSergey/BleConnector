package com.example.ble_connector;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BleSearch {

    private final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    private final int BT_ENABLE_REQUEST = 100;

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bleScanner = null;
    private BluetoothDevice bleDevice = null;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ArrayList<String> deviceNames = new ArrayList<>();

    private Activity activity;
    private BleSearchCallback bleSearchCallback;

    //----------------------------------------------------------------------------------------------
    public interface BleSearchCallback{
        void bleDeviceFound(BluetoothDevice device);
    }

    public BleSearch(Activity activity, BleSearchCallback bleSearchCallback){
        this.activity = activity;
        this.bleSearchCallback = bleSearchCallback;

        if(checkPermission() == false) throw new NullPointerException("Permissions for BLE!!!");

        // Adapter
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) throw new NullPointerException("Bluetooth adapter not found!!!");

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBT, BT_ENABLE_REQUEST);
        }
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        else return true;
    }

    //----------------------------------------------------------------------------------------------

    final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            bleSearchCallback.bleDeviceFound(device);
        }

        @Override
        public void onBatchScanResults(List results) {
        }

        @Override
        public void onScanFailed(int errorCode) {
        }
    };

    //----------------------------------------------------------------------------------------------

    public void startScan(){
        devices.clear();
        // Scanner
        if ((bluetoothAdapter != null) && (bluetoothAdapter.isEnabled())) {
            if (bleScanner == null)
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bleScanner != null) {
                ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(svUUID)).build();
                ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
                filters.add(filter);
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                bleScanner.startScan(filters, settings, scanCallback);

                Log.i("BLE_TEST", "Scan is started");
            }
        } else {
            Log.i("BLE_TEST", "Bluetooth is disable!");
        }
    }

    public void stopScan(){ bleScanner.stopScan(scanCallback);};
}
