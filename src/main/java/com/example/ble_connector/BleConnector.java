package com.example.ble_connector;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BleConnector{

    public enum OPERATIONS{
        OPERATION_WRITE,
        OPERATION_READ,
        OPERATION_DESC_WRITE
    }

    public interface BleConnectorCallbacks{
        void connectedCallback(List<BluetoothGattService> services);
        void disconnectedCallback();
        void writeCharCallback();
        void readCharCallback(BluetoothGattCharacteristic characteristic);
        void notificationCallback(BluetoothGattCharacteristic characteristic);
        void operationFailed(OPERATIONS operation);
    }

    private final int TRY_NUM = 10;
    private final int TRY_PERIOD = 500; // ms
    private Context context;
    private BluetoothDevice device = null;
    private BleConnectorCallbacks callbacks;
    public BluetoothGatt bleGatt = null;
    private boolean connected = false;

    final int STATE_DISCONNECTED = 0;
    final int STATE_CONNECTING = 1;
    final int STATE_CONNECTED = 2;

    public List<BluetoothGattService> services;

    final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            switch (newState) {
                case STATE_DISCONNECTED:
                    connected = false;
                    Log.i("BLE_TEST", "STATE_DISCONNECTED");
                    break;
                case STATE_CONNECTING:
                    connected = false;
                    Log.i("BLE_TEST", "STATE_CONNECTING");
                    break;
                case STATE_CONNECTED:
                    connected = true;
                    Log.i("BLE_TEST", "STATE_CONNECTED");
                    break;
            };

            if (newState == STATE_CONNECTED) {
                if (gatt.discoverServices())
                    Log.i("BLE_TEST", "discaverService STARTED");
                else
                    Log.i("BLE_TEST", "discaverService FAILE");
            } else if (newState == STATE_DISCONNECTED) {
                bleGatt.disconnect();
                bleGatt.close();
                bleGatt = null;

                bleGatt = device.connectGatt(context, false, gattCallback);
                callbacks.disconnectedCallback();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                services = gatt.getServices();
                callbacks.connectedCallback(services);

            } else {
                Log.i("BLE_TEST", "onServicesDiscovered failed!");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            if (status == BluetoothGatt.GATT_SUCCESS)
                callbacks.writeCharCallback();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                callbacks.readCharCallback(characteristic);
            } else
                Log.i("BLE_TEST", "Red char failed!");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            callbacks.notificationCallback(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    public BleConnector(Context ctx, BluetoothDevice device, BleConnectorCallbacks callbacks){
        this.context = ctx;
        this.device = device;
        this.callbacks = callbacks;
    }

    public void connect(){
        bleGatt = device.connectGatt(context, false, gattCallback);
    }
    public void disconnect(){
        /*if(bleGatt != null)
            bleGatt.disconnect();*/
    }
    public boolean isConnect(){
        return connected;
    }
    public boolean writeChar(final BluetoothGattCharacteristic characteristic){

        Thread thread = new Thread(
                new Runnable(){
                    @Override
                    public void run() {

                        boolean result = false;
                        for(int tryNum = 0; tryNum < TRY_NUM; tryNum++) {
                            if(bleGatt.writeCharacteristic(characteristic)){
                                Log.i("BLE_TEST", "Write success");
                                result = true;
                                break;
                            }
                            else{
                                try {
                                    Thread.sleep(TRY_PERIOD);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if(!result) callbacks.operationFailed(OPERATIONS.OPERATION_WRITE);
                    }
                });
        thread.start();

        return true;
    }
    public boolean readChar(final BluetoothGattCharacteristic characteristic){

        Thread thread = new Thread(
                new Runnable(){
                    @Override
                    public void run() {
                        boolean result = false;
                        for(int tryNum = 0; tryNum < TRY_NUM; tryNum++) {
                            if(bleGatt.readCharacteristic(characteristic)){
                                Log.i("BLE_TEST", "Read success");
                                result = true;
                                break;
                            }
                            else{
                                try {
                                    Thread.sleep(TRY_PERIOD);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if(!result) callbacks.operationFailed(OPERATIONS.OPERATION_READ);
                    }
                });
        thread.start();

        return true;
    }
    public boolean writeDesc(final BluetoothGattDescriptor descriptor){
        if(descriptor == null){
            Log.i("BLE_TEST", "Descriptor is null");
            return false;
        }
        Thread thread = new Thread(
                new Runnable(){
                    @Override
                    public void run() {

                        boolean result = false;
                        for(int tryNum = 0; tryNum < TRY_NUM; tryNum++) {
                            if(bleGatt.writeDescriptor(descriptor)){
                                Log.i("BLE_TEST", "Write desc success");
                                result = true;
                                break;
                            }
                            else{
                                try {
                                    Thread.sleep(TRY_PERIOD);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if(!result) callbacks.operationFailed(OPERATIONS.OPERATION_DESC_WRITE);
                    }
                });
        thread.start();

        return true;
    }
    public boolean notiEnable(final String srvUUID, String charUUID){

        BluetoothGattService service = this.bleGatt.getService(UUID.fromString(srvUUID));
        UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = BleConnector.convertFromInteger(0x2902);
        BluetoothGattDescriptor descriptor;

        // Enable notification
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(charUUID));
        this.bleGatt.setCharacteristicNotification(characteristic, true);
        descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        return this.writeDesc(descriptor);
    }
    static public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}