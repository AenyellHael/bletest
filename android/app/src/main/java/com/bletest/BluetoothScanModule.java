package com.bletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Handler;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Build;
import java.io.IOException;
import java.io.OutputStream;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import java.util.UUID;
import java.util.Arrays;

import com.facebook.react.bridge.Promise;
import android.bluetooth.BluetoothProfile;

import com.facebook.react.bridge.Callback;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class BluetoothScanModule extends ReactContextBaseJavaModule {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CODE_BLUETOOTH_SCAN = 1;
    private static final int REQUEST_CODE_BLUETOOTH_CONNECT = 1;
    private static final UUID fanLevelServiceUUID = UUID.fromString("92758440-9725-11e9-b475-0800200c9a66");
    private static final UUID fanLevelCharacteristicUUID = UUID.fromString("92758442-9725-11e9-b475-0800200c9a66");
    private Promise fanLevelPromise;
    private BluetoothGatt bluetoothGatt;
    private final ReactApplicationContext reactContext;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler;
    private boolean scanning;

    private List<BluetoothDevice> foundDevices = new ArrayList<>();

    private StringBuilder logsBuilder = new StringBuilder(); // Создаем StringBuilder для логов

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private void addLog(String log) {
        logsBuilder.append(log).append("\n"); // Добавляем новый лог в StringBuilder
        sendLogsToJS(logsBuilder.toString()); // Отправляем логи в JS
    }

    private void sendEvent(String eventName, WritableMap params) {
        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
    }

    public BluetoothScanModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        bluetoothAdapter = getBluetoothAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    private final BroadcastReceiver discoverDevicesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                WritableMap deviceInfo = Arguments.createMap();
                deviceInfo.putString("name", device.getName());
                deviceInfo.putString("address", device.getAddress());

                sendEvent("DeviceFound", deviceInfo);
            }
        }
    };

    @Override
    public String getName() {
        return "BluetoothScanModule";
    }

    @ReactMethod
    public void enableBluetooth() 
    {
        if (bluetoothAdapter == null) {
            Log.d("BluetoothScanModule", "Устройство не поддерживает Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            sendLogsToJS("Bluetooth is disabled. Turning on");
            if (checkBluetoothConnectPermission()) {
                sendLogsToJS("Check Bluetooth Scan Permission");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                getCurrentActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                Log.d("BluetoothScanModule", "Bluetooth enableBluetooth called");
            } else {
                sendLogsToJS("requestBluetoothScanPermission()");
                requestBluetoothConnectPermission();
            }
        }
    }

    @ReactMethod
    public void makeDeviceDiscoverable(int duration)// Проверяем наличие разрешения перед началом сканирования
    {
        int scanMode = bluetoothAdapter.getScanMode();
        boolean isDiscoverable = scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;

        if (bluetoothAdapter == null) {
            Log.d("BluetoothScanModule", "Устройство не поддерживает Bluetooth");
            return;
        }

        if (isDiscoverable) {
            return;
        }
        
        if (checkBluetoothScanPermission()) {
            sendLogsToJS("Making device discoverable for " + duration + " seconds.");
            Log.d("BluetoothScanModule", "Making device discoverable for " + duration + " seconds.");
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
            getCurrentActivity().startActivity(discoverableIntent);
            sendLogsToJS("Canceling makeDeviceDiscoverable()");
        } else {
            requestBluetoothScanPermission();
        }
    }

    @ReactMethod //Тестим метод
    public void discoverDevices() {
        if (bluetoothAdapter != null) {
            // if (!bluetoothAdapter.isEnabled()){
            //     makeDeviceDiscoverable(300);
            // }

            if (checkBluetoothScanPermission()) {
                if (scanning) {
                // Если уже выполняется сканирование, остановите его
                stopDeviceDiscovery();
            }

            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopDeviceDiscovery(); // Остановить сканирование по истечении SCAN_PERIOD
                }
            }, SCAN_PERIOD);

            // Начать сканирование Bluetooth устройств
            foundDevices.clear(); // Очистить список найденных устройств перед началом нового сканирования
            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);

            sendLogsToJS("Device discovery started.");
                // if (bluetoothAdapter.isDiscovering()) {
                //     bluetoothAdapter.cancelDiscovery();
                //     Log.d("BluetoothScanModule", "Canceling discovery.");
                //     sendLogsToJS("Canceling discovery.");

                //     // Проверяем наличие разрешения перед началом сканирования
                //     if (checkBluetoothScanPermission()) {
                //         sendLogsToJS("Start discovery.");
                //         bluetoothAdapter.startDiscovery();
                //         IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                //         getReactApplicationContext().registerReceiver(discoverDevicesReceiver, discoverDevicesIntent);
                //         sendLogsToJS("Canceling discovery_1.");
                //     } else {
                //         requestBluetoothScanPermission();
                //     }
                // }

                // if (!bluetoothAdapter.isDiscovering()) {
                //     // Проверяем наличие разрешения перед началом сканирования
                //     if (checkBluetoothScanPermission()) {
                //         sendLogsToJS("Start discovery.");
                //         bluetoothAdapter.startDiscovery();
                //         IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                //         getReactApplicationContext().registerReceiver(discoverDevicesReceiver, discoverDevicesIntent);
                //         sendLogsToJS("Canceling discovery_2.");
                //     } else {
                //         requestBluetoothScanPermission();
                //     }
                // }
            } else {
                requestBluetoothScanPermission();
            }
        }
    }

    // Метод для остановки сканирования устройств
    private void stopDeviceDiscovery() {
        if (scanning && bluetoothAdapter != null) {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);

            sendLogsToJS("Device discovery stopped.");

            // Отправить информацию о найденных устройствах в JS
            updateDeviceList();
        }
    }

    // Обработчик событий обнаружения устройств
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && device.getName().startsWith("OXY")) {
                sendLogsToJS("Device OXY find: " + device.getName());
                // Добавить устройство в список найденных устройств
                if (!foundDevices.contains(device)) {
                    foundDevices.add(device);
                }
            }
        }
    };

    private void updateDeviceList() {
        WritableArray devicesArray = Arguments.createArray();

        for (BluetoothDevice device : foundDevices) {
            WritableMap deviceInfo = Arguments.createMap();
            deviceInfo.putString("name", device.getName());
            deviceInfo.putString("address", device.getAddress());
            devicesArray.pushMap(deviceInfo);

            sendLogsToJS("Device found: " + device.getName());
        }

        // Создать событие и отправить информацию в JS
        WritableMap resultMap = Arguments.createMap();
        resultMap.putArray("devices", devicesArray);
        sendEvent("DevicesFound", resultMap);
    }

    @ReactMethod
    public void getPairedDevices(Callback callback) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        WritableArray pairedDevicesArray = Arguments.createArray();

        sendLogsToJS("getPairedDevices..");

        for (BluetoothDevice device : pairedDevices) {
            WritableMap deviceInfo = Arguments.createMap();
            deviceInfo.putString("name", device.getName());
            deviceInfo.putString("address", device.getAddress());
            pairedDevicesArray.pushMap(deviceInfo);

            sendLogsToJS("Paired device found: " + device.getName());
        }

        callback.invoke(pairedDevicesArray);
    }

    @ReactMethod //Должен получать список сопряженных устройств
    public void findDevicesWithNamePrefix(String namePrefix) {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            WritableArray devicesArray = Arguments.createArray();

            for (BluetoothDevice device : pairedDevices) {
                if (device.getName() != null && device.getName().startsWith(namePrefix)) {
                    WritableMap deviceInfo = Arguments.createMap();
                    deviceInfo.putString("name", device.getName());
                    deviceInfo.putString("address", device.getAddress());
                    devicesArray.pushMap(deviceInfo);

                    sendLogsToJS("Device found with name prefix " + namePrefix + ": " + device.getName());
                }
            }

            WritableMap resultMap = Arguments.createMap();
            resultMap.putArray("devices", devicesArray);

            sendEvent("DevicesFound", resultMap);
        }
    }
    
    @ReactMethod //Метод сопряжения
    public void pairWithDevice(String deviceName, String deviceAddress) {
        sendLogsToJS("pairWithDevice != null");
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        
        if (bluetoothAdapter != null) {
            sendLogsToJS("bluetoothAdapter != null");
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

            if (device != null) {
                sendLogsToJS("device != null");
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // Создаем BroadcastReceiver для обработки событий сопряжения
                    BroadcastReceiver pairingReceiver = new BroadcastReceiver() {
                        public void onReceive(Context context, Intent intent) {
                            String action = intent.getAction();

                            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                                // Выполняем сопряжение с устройством
                                device.setPairingConfirmation(true);
                                context.unregisterReceiver(this); // Удаляем BroadcastReceiver после сопряжения
                            }
                        }
                    };

                    IntentFilter pairingIntentFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
                    reactContext.registerReceiver(pairingReceiver, pairingIntentFilter);
                    
                    // Запускаем сопряжение
                    device.createBond();
                }
            }
        }
    }

    @ReactMethod
    public BluetoothAdapter getBluetoothAdapter() {
        BluetoothAdapter adapter = null;
        try {
            android.bluetooth.BluetoothManager bluetoothManager =
                    (android.bluetooth.BluetoothManager) reactContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                adapter = bluetoothManager.getAdapter();
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return adapter;
    }

    private void sendLogsToJS(String logs) {
        WritableMap params = Arguments.createMap();
        params.putString("logs", logs);

        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("LogsUpdated", params);
    }

    // Метод для проверки разрешения на выполнение операции сканирования Bluetooth
    private boolean checkBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int permission = ContextCompat.checkSelfPermission(reactContext, Manifest.permission.BLUETOOTH_SCAN);
            return permission == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // Если версия Android ниже Android 12, считаем, что разрешение есть
        }
    }

    // Метод для запроса разрешения на выполнение операции сканирования Bluetooth
    private void requestBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            sendLogsToJS("Build.VERSION.SDK_INT >= Build.VERSION_CODES.S");
            ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_CODE_BLUETOOTH_SCAN);
        }
    }

    // Метод для проверки разрешения на выполнение операции включения Bluetooth
    private boolean checkBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int permission = ContextCompat.checkSelfPermission(reactContext, Manifest.permission.BLUETOOTH_CONNECT);
            return permission == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // Если версия Android ниже Android 12, считаем, что разрешение есть
        }
    }

    // Метод для запроса разрешения на выполнение операции включения Bluetooth
    private void requestBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            sendLogsToJS("Build.VERSION.SDK_INT >= Build.VERSION_CODES.S");
            ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CODE_BLUETOOTH_CONNECT);
        }
    }

    // Метод для обработки результатов запроса разрешения
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == REQUEST_CODE_BLUETOOTH_SCAN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено, выполняйте сканирование Bluetooth
                // discoverDevices();
            } else {
                // Разрешение не предоставлено, обработайте это ситуацию соответствующим образом
                Log.d("BluetoothScanModule", "Permission for BLUETOOTH_SCAN not granted.");
                sendLogsToJS("Permission for BLUETOOTH_SCAN not granted.");
            }
        }
    }

    @ReactMethod
    public void sendData(final int fanLevel) {
        sendLogsToJS("Send fanLevel: " + fanLevel);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null) {
                    // Bluetooth не поддерживается на этом устройстве
                    return null;
                }

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice("08:3A:F2:4B:C5:B2");
                UUID serviceUUID = UUID.fromString("92758440-9725-11E9-B475-0800200C9A66");
                UUID fanLevelUUID = UUID.fromString("92758442-9725-11E9-B475-0800200C9A66");
                BluetoothSocket socket = null;

                try {
                    socket = device.createRfcommSocketToServiceRecord(serviceUUID);
                    socket.connect();

                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(fanLevel); // Отправка значения fanLevel
                    outputStream.close();
                } catch (IOException e) {
                    sendLogsToJS("Error1: " + e.getMessage());
                    // Обработка ошибок
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            sendLogsToJS("Error2: " + e.getMessage());
                            // Обработка ошибок
                        }
                    }
                }

                return null;
            }
        }.execute();
    }

    @ReactMethod
    public void readFanLevel(String deviceAddress, final Promise promise) {
        fanLevelPromise = promise; // Сохраняем Promise в переменную класса
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
        bluetoothGatt = device.connectGatt(getReactApplicationContext(), false, gattCallback);

        if (bluetoothGatt == null) {
            fanLevelPromise.reject("CONNECTION_ERROR", "Unable to connect to the device.");
            return;
        }

        bluetoothGatt.connect();
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(fanLevelServiceUUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(fanLevelCharacteristicUUID);
                    if (characteristic != null) {
                        gatt.readCharacteristic(characteristic);
                    } else {
                        fanLevelPromise.reject("CHARACTERISTIC_NOT_FOUND", "Fan Level characteristic not found.");
                    }
                } else {
                    fanLevelPromise.reject("SERVICE_NOT_FOUND", "Fan Level service not found.");
                }
            } else {
                fanLevelPromise.reject("SERVICE_DISCOVERY_FAILED", "Service discovery failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().equals(fanLevelCharacteristicUUID)) {
                    // int fanLevelValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    // Log.d("BluetoothScanModule", "Fan Level: " + fanLevelValue);
                    byte[] fanLevelBytes = characteristic.getValue();
                    String fanLevelString = new String(fanLevelBytes);
                    Log.d("BluetoothScanModule", "Fan Level: " + fanLevelString);
                    fanLevelPromise.resolve(fanLevelString); // Разрешаем Promise
                    gatt.disconnect();
                } else {
                    fanLevelPromise.reject("INVALID_CHARACTERISTIC", "Unexpected characteristic read.");
                    Log.d("BluetoothScanModule", "INVALID_CHARACTERISTIC: Unexpected characteristic read.");
                }
            } else {
                fanLevelPromise.reject("CHARACTERISTIC_READ_FAILED", "Characteristic read failed with status: " + status);
                Log.d("BluetoothScanModule", "CHARACTERISTIC_READ_FAILED, Characteristic read failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Handle notifications or indications if needed
        }
    };

    @ReactMethod
    public void writeFanLevel(String deviceAddress, final Promise promise) {
        fanLevelPromise = promise;
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
        bluetoothGatt = device.connectGatt(getReactApplicationContext(), false, gattCallbackWrite);

        if (bluetoothGatt == null) {
            promise.reject("CONNECTION_ERROR", "Unable to connect to the device.");
            Log.d("BluetoothScanModule", "CONNECTION_ERROR, Unable to connect to the device.");
            return;
        }
        bluetoothGatt.connect();
    }

        private BluetoothGattCallback gattCallbackWrite = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                String fanLevel = "15";
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(fanLevelServiceUUID);
                    if (service != null) {
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(fanLevelCharacteristicUUID);
                        if (characteristic != null) {
                            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                            characteristic.setValue(fanLevel);
                            boolean success = gatt.writeCharacteristic(characteristic);
                            if (success) {
                                fanLevelPromise.resolve("Write operation successful. " + fanLevel + " : " + characteristic);
                                Log.d("BluetoothScanModule", "Write operation successful");
                            } else {
                                fanLevelPromise.reject("WRITE_FAILED", "Write operation failed");
                                Log.d("BluetoothScanModule", "WRITE_FAILED, Write operation failed");
                            }
                            gatt.disconnect();
                        } else {
                            fanLevelPromise.reject("CHARACTERISTIC_NOT_FOUND", "Fan Level characteristic not found.");
                        }
                    } else {
                        fanLevelPromise.reject("SERVICE_NOT_FOUND", "Fan Level service not found.");
                    }
                } else {
                    fanLevelPromise.reject("SERVICE_DISCOVERY_FAILED", "Service discovery failed with status: " + status);
                }
            }
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                // Handle notifications or indications if needed
            }
        };
}
