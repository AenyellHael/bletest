import React, { Component } from 'react';
import { View, Button, Text, NativeModules, DeviceEventEmitter, ScrollView } from 'react-native';
const { BluetoothScanModule } = NativeModules;

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      devices: [],
      logs: '',
    };

    // Подписываемся на событие "DeviceFound"
    DeviceEventEmitter.addListener('LogsUpdated', this.updateLogs);
    DeviceEventEmitter.addListener('DeviceFound', this.handleDeviceFound); // Обработчик события DeviceFound
  }

  componentDidMount() {
    BluetoothScanModule.makeDeviceDiscoverable(300);

    // BluetoothScanModule.discoverDevices();
  }

  componentWillUnmount() {
    if (typeof DeviceEventEmitter?.removeListener == "function") {
      DeviceEventEmitter?.removeListener('LogsUpdated', this.updateLogs);
      DeviceEventEmitter?.removeListener('DeviceFound', this.handleDeviceFound);
    }
  }
  
  updateLogs = (event) => {
    const logs = event.logs;
    this.setState({ logs });
  };

  // Обработчик события "DeviceFound"
  handleDeviceFound = (event) => {
    const device = event; // Здесь предполагается, что событие содержит данные об устройстве
    this.updateLogs(`event: ${JSON.stringify(device)}`)
    this.updateLogs(`event.device: ${JSON.stringify(event?.devices)}`)
    this.setState((prevState) => ({
      devices: [...prevState.devices, device], // Добавляем новое устройство в массив устройств
    }));
  };

  // bluetoothCheck() {
  //   // BluetoothScanModule.getBluetoothAdapter();
  //   BluetoothScanModule.enableBluetooth();
  // }

  discoverDevices() {
    // BluetoothScanModule.discoverDevices();
    BluetoothScanModule.discoverDevices();
  }

  makeDeviceDiscoverable() {
    BluetoothScanModule.makeDeviceDiscoverable(300); // Здесь 300 - длительность в секундах
  }

  pairWithDevice = (deviceName, deviceAddress) => {
    BluetoothScanModule.pairWithDevice(deviceName, deviceAddress);
  };

  findDevicesWithNamePrefix() {
    BluetoothScanModule.findDevicesWithNamePrefix('OXY');
  }

  render() {
    return (
      <View>
        <Button title="Find OXY Devices" onPress={() => this.discoverDevices()} />
        
        <ScrollView>
          {this.state.devices.map((device, index) => (
            <View key={index}>
              <Text>{JSON.stringify(device)}</Text>
              {/* <Button
                title={device.name}
                onPress={() => this.pairWithDevice(device.name, device.address)}
              /> */}
              {/* <Text>Address: {device.address}</Text> */}
            </View>
          ))}
        </ScrollView>

        <Text>Logs: {this.state.logs}</Text>
        <Text></Text>
        {/* <Text>Found Devices:</Text>
        {this.state.devices.map((device, index) => (
          <Text key={index}>
            {device.name} - {device.address}
          </Text>
        ))} */}
      </View>
    );
  }
}

export default App;
