import React, { Component } from 'react';
import { View, Button, Text, NativeModules, DeviceEventEmitter, ScrollView, TextInput } from 'react-native';
const { BluetoothScanModule, FanControlModule } = NativeModules;

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      devices: [],
      logs: '',
      inputData: '',
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
    this.updateLogs(`event: ${JSON.stringify(device)}`);
    this.updateLogs(`event.device: ${JSON.stringify(event?.devices)}`);
    this.setState((prevState) => ({
      devices: [...prevState.devices, device], // Добавляем новое устройство в массив устройств
    }));
  };

  discoverDevices() {// Не факт что это работает
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

  connectOXY() {
    BluetoothScanModule.pairWithDevice("OXY-5288", "08:3A:F2:4B:C5:B2");
  }

  readFanLevel() {
    BluetoothScanModule.readFanLevel('08:3A:F2:4B:C5:B2')
  .then(fanLevelValue => {
    console.log(`Fan Level: ${fanLevelValue}`);
  })
  .catch(error => {
    console.error(`Error reading Fan Level: ${error}`);
  });
  }

  writeFanLevel() {
    BluetoothScanModule.writeFanLevel('08:3A:F2:4B:C5:B2')
      .then(result => {
        console.log(result);
      })
      .catch(error => {
        console.error(error);
      });
  }

  readFanLevelModule() {
    FanControlModule.connectToDevice('08:3A:F2:4B:C5:B2')
    .then((connected) => {
      if (connected) {
        FanControlModule.readFanLevel()
          .then((fanLevel) => {
            console.log('Fan level:', fanLevel);
          })
          .catch((error) => {
            console.error('Error reading fan level:', error);
          });
      } else {
        console.error('Failed to connect to the device');
      }
    })
    .catch((error) => {
      console.error('Error connecting to the device:', error);
    });
  }

  writeFanLevelModule() {
    FanControlModule.writeFanLevel(25)
    .then(() => {
      console.log('Fan level written successfully');
    })
    .catch((error) => {
      console.error('Error writing fan level:', error);
    });
  }

  render() {
    return (
      <View>
        <Button title="Find OXY Devices" onPress={() => this.discoverDevices()} />
        <Button title="OXY-5288" onPress={() => this.connectOXY()} />

        <TextInput
          placeholder="Введите данные для отправки"
          value={this.state.inputData}
          onChangeText={(inputData) => this.setState({ inputData })}
          style={{ borderBottomWidth: 1, marginBottom: 10, paddingHorizontal: 8 }}
        />
        {/* <Button title="Отправить" onPress={this.sendData} /> */}
        <Button title="Read" onPress={this.readFanLevel} />
        <Button title="Write" onPress={this.writeFanLevel} />
        
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
