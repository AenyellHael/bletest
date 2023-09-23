package com.bletest;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import java.util.UUID;

public class FanControlModule extends ReactContextBaseJavaModule {

    private final BluetoothManager bluetoothManager;
    private BluetoothGatt bluetoothGatt;

    public FanControlModule(ReactApplicationContext reactContext) {
        super(reactContext);
        bluetoothManager = (BluetoothManager) reactContext.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    @Override
    public String getName() {
        return "FanControlModule";
    }

    @ReactMethod
    public void connectToDevice(String deviceAddress, final Promise promise) {
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        bluetoothGatt = device.connectGatt(getReactApplicationContext(), false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    promise.resolve(true);
                } else {
                    promise.reject("CONNECTION_ERROR", "Failed to connect to the device");
                }
            }
        });
    }

    @ReactMethod
    public void readFanLevel(final Promise promise) {
        if (bluetoothGatt == null) {
            promise.reject("NOT_CONNECTED", "Device is not connected");
            return;
        }

        UUID fanLevelServiceUUID = UUID.fromString("92758440-9725-11e9-b475-0800200c9a66");
        UUID fanLevelCharacteristicUUID = UUID.fromString("92758442-9725-11e9-b475-0800200c9a66");

        BluetoothGattService service = bluetoothGatt.getService(fanLevelServiceUUID);
        if (service == null) {
            promise.reject("SERVICE_NOT_FOUND", "Fan level service not found");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(fanLevelCharacteristicUUID);
        if (characteristic == null) {
            promise.reject("CHARACTERISTIC_NOT_FOUND", "Fan level characteristic not found");
            return;
        }

        bluetoothGatt.readCharacteristic(characteristic);
    }

    @ReactMethod
    public void writeFanLevel(int fanLevel, final Promise promise) {
        if (bluetoothGatt == null) {
            promise.reject("NOT_CONNECTED", "Device is not connected");
            return;
        }

        UUID fanLevelServiceUUID = UUID.fromString("92758440-9725-11e9-b475-0800200c9a66");
        UUID fanLevelCharacteristicUUID = UUID.fromString("92758442-9725-11e9-b475-0800200c9a66");

        BluetoothGattService service = bluetoothGatt.getService(fanLevelServiceUUID);
        if (service == null) {
            promise.reject("SERVICE_NOT_FOUND", "Fan level service not found");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(fanLevelCharacteristicUUID);
        if (characteristic == null) {
            promise.reject("CHARACTERISTIC_NOT_FOUND", "Fan level characteristic not found");
            return;
        }

        characteristic.setValue(new byte[]{(byte) fanLevel});
        bluetoothGatt.writeCharacteristic(characteristic);
    }
}