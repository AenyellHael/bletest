import React from 'react';
import { TouchableOpacity, View, StyleSheet, Platform, Linking } from 'react-native';
import Icon from 'react-native-vector-icons/FontAwesome';

const BluetoothSettingsButton = () => {
  const openBluetoothSettings = async () => {
    if (Platform.OS === 'android') {
      try {
        // Открываем настройки Bluetooth на Android
        await Linking.sendIntent("android.settings.BLUETOOTH_SETTINGS");
      } catch (error) {
        console.log('Ошибка при открытии настроек: ', error);
      }
    } else if (Platform.OS === 'ios') {
      try {
        // Открываем настройки Bluetooth на Android
        await Linking.openURL("App-Prefs:root=General&path=Bluetooth");
      } catch (error) {
        console.log('Ошибка при открытии настроек: ', error);
      }
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.button} onPress={openBluetoothSettings}>
        <Icon name="bluetooth" size={50} color="#FFFFFF" />
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    absolute: 1,
    // left: '-17%',
    top: '90%',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
  button: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: '#00B9EA',
    justifyContent: 'center',
    alignItems: 'center',
  },
});

export default BluetoothSettingsButton;
