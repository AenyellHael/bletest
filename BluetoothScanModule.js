import { NativeModules, DeviceEventEmitter } from 'react-native';

const { BluetoothScanModule } = NativeModules;

class BluetoothScanner {
  constructor() {
    this.deviceFoundListener = null;
    this.state = {
      logMessages: [] // Список сообщений для отображения
    };

    DeviceEventEmitter.addListener('onDeviceFound', deviceInfo => {
      if (this.deviceFoundListener) {
        this.deviceFoundListener(deviceInfo);
      }
    });
  }

  startDiscovery() {
    console.log('Начался поиск устройств...');
    BluetoothScanModule.startDiscovery();
  }

  stopDiscovery() {
    console.log('Поиск остановлен');
    BluetoothScanModule.stopDiscovery();
  }

  onDeviceFound(callback) {
    this.deviceFoundListener = callback;
    console.log('Обработчик обнаруженных устройств установлен.');
    // this.addLogMessage('Обработчик обнаруженных устройств установлен.');
  }

  removeDeviceFoundListener() {
    this.deviceFoundListener = null;
    console.log('Обработчик обнаруженных устройств удален.');
    // this.addLogMessage('Обработчик обнаруженных устройств удален.');
  }
}

export default new BluetoothScanner();
