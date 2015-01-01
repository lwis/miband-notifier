package com.lewisjuggins.miband.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import com.google.common.base.Objects;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AsyncBluetoothGatt extends BluetoothGattCallback {

	private BluetoothDevice device;
	private Context context;
	private boolean autoConnect;
	private BluetoothGatt gatt;

	// References to all the deferred objects which will pass around the success/failure state of async actions.
	private DeferredObject<Void, Integer, Void> connect;
	private DeferredObject<Void, Integer, Void> disconnect;
	private DeferredObject<Void, Integer, Void> discoverServices;
	private DeferredObject<Void, Integer, Void> executeReliableWrite;
	private DeferredObject<Integer, Integer, Void> readRemoteRssi;
	private HashMap<CharacteristicKey, DeferredObject<BluetoothGattCharacteristic, Integer, Void>> readCharacteristic;
	private HashMap<CharacteristicKey, DeferredObject<BluetoothGattCharacteristic, Integer, Void>> writeCharacteristic;
	private HashMap<CharacteristicKey, DeferredObject<Void, Void, BluetoothGattCharacteristic>> changeCharacteristic;
	private HashMap<DescriptorKey, DeferredObject<BluetoothGattDescriptor, Integer, Void>> readDescriptor;
	private HashMap<DescriptorKey, DeferredObject<BluetoothGattDescriptor, Integer, Void>> writeDescriptor;

	// Key for uniquely identifying a characteristic is composed of:
	// - Characteristic UUID + instance ID
	// - Parent service UUID + instance ID
	private class CharacteristicKey {
		private UUID service;
		private int serviceInstance;
		private UUID characteristic;
		private int charInstance;
		public CharacteristicKey(BluetoothGattCharacteristic characteristic) {
			this.service = characteristic.getService().getUuid();
			this.serviceInstance = characteristic.getService().getInstanceId();
			this.characteristic = characteristic.getUuid();
			this.charInstance = characteristic.getInstanceId();
		}
		@Override
		public int hashCode() {
			return Objects.hashCode(service, serviceInstance, characteristic, charInstance);
		}
		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o == this) {
				return true;
			}
			if (!(o instanceof CharacteristicKey)) {
				return false;
			}
			CharacteristicKey other = (CharacteristicKey)o;
			return Objects.equal(service, other.service) &&
					Objects.equal(serviceInstance, other.serviceInstance) &&
					Objects.equal(characteristic, other.characteristic) &&
					Objects.equal(charInstance, other.charInstance);
		}
	}

	// Key for uniquely identifying a descriptor is composed of:
	// - Descriptor UUID
	// - Parent characteristic UUID + instance ID
	// - Parent service UUID + instance ID
	private class DescriptorKey {
		private UUID service;
		private int serviceInstance;
		private UUID characteristic;
		private int charInstance;
		private UUID descriptor;
		public DescriptorKey(BluetoothGattDescriptor descriptor) {
			this.service = descriptor.getCharacteristic().getService().getUuid();
			this.serviceInstance = descriptor.getCharacteristic().getService().getInstanceId();
			this.characteristic = descriptor.getCharacteristic().getUuid();
			this.charInstance = descriptor.getCharacteristic().getInstanceId();
			this.descriptor = descriptor.getUuid();
		}
		@Override
		public int hashCode() {
			return Objects.hashCode(service, serviceInstance, characteristic, charInstance, descriptor);
		}
		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o == this) {
				return true;
			}
			if (!(o instanceof DescriptorKey)) {
				return false;
			}
			DescriptorKey other = (DescriptorKey)o;
			return Objects.equal(service, other.service) &&
					Objects.equal(serviceInstance, other.serviceInstance) &&
					Objects.equal(characteristic, other.characteristic) &&
					Objects.equal(charInstance, other.charInstance) &&
					Objects.equal(descriptor, other.descriptor);
		}
	}

	public AsyncBluetoothGatt(BluetoothDevice device, Context context, boolean autoConnect) {
		this.device = device;
		this.context = context;
		this.autoConnect = autoConnect;
		readCharacteristic = new HashMap<CharacteristicKey, DeferredObject<BluetoothGattCharacteristic, Integer, Void>>();
		writeCharacteristic = new HashMap<CharacteristicKey, DeferredObject<BluetoothGattCharacteristic, Integer, Void>>();
		readDescriptor = new HashMap<DescriptorKey, DeferredObject<BluetoothGattDescriptor, Integer, Void>>();
		writeDescriptor = new HashMap<DescriptorKey, DeferredObject<BluetoothGattDescriptor, Integer, Void>>();
		changeCharacteristic = new HashMap<CharacteristicKey, DeferredObject<Void, Void, BluetoothGattCharacteristic>>();
	}

	private void checkConnected() {
		if (gatt == null) {
			throw new RuntimeException("GATT is not connected.");
		}
	}

	private void resetDeferreds() {
		// TODO Stop in progress deferreds before resetting?
		connect = new DeferredObject<Void, Integer, Void>();
		disconnect = new DeferredObject<Void, Integer, Void>();
		discoverServices = null;
		executeReliableWrite = null;
		readRemoteRssi = null;
		readCharacteristic.clear();
		writeCharacteristic.clear();
		changeCharacteristic.clear();
		readDescriptor.clear();
		writeDescriptor.clear();
	}

	// Return GATT instance.
	public BluetoothGatt getGatt() {
		return gatt;
	}

	// Connect or reconnect to the device and return a promise for its completion.
	public Promise<Void, Integer, Void> connect() {
		// Handle connecting for the first time.
		if (connect == null) {
			resetDeferreds();
			gatt = device.connectGatt(context, autoConnect, this);
			if (gatt == null) {
				// Immediate error if the connect failed to return a gatt.
				connect.reject(null);
			}
		}
		// Reconnect if connect is called again.
		else if (!connect.isPending()) {
			resetDeferreds();
			if (!gatt.connect()) {
				connect.reject(null);
			}
		}
		return connect.promise();
	}

	// Return the disconnected promise.
	public Promise<Void, Integer, Void> disconnected() {
		return disconnect;
	}

	// Start service discovery and return a promise for its completion.
	public Promise<Void, Integer, Void> discoverServices() {
		checkConnected();
		// If there's already a request in flight, return the current promise for results.
		if (discoverServices != null && discoverServices.isPending()) {
			return discoverServices.promise();
		}
		// Start service discovery.
		discoverServices = new DeferredObject<Void, Integer, Void>();
		if (!gatt.discoverServices()) {
			// Immediate error if the service discovery failed to start.
			discoverServices.reject(null);
		}
		return discoverServices.promise();
	}

	// Request remote signal strength and return a promise for its completion.
	public Promise<Integer, Integer, Void> readRemoteRssi() {
		checkConnected();
		// If there's already a request in flight, return the current promise for results.
		if (readRemoteRssi != null && readRemoteRssi.isPending()) {
			return readRemoteRssi.promise();
		}
		// Request remote RSSI and return a promise for the results.
		readRemoteRssi = new DeferredObject<Integer, Integer, Void>();
		if (!gatt.readRemoteRssi()) {
			// Immediate error if the read RSSI call failed.
			readRemoteRssi.reject(null);
		}
		return readRemoteRssi.promise();
	}

	// Execute reliable write transaction and return a promise for its completion.
	public Promise<Void, Integer, Void> executeReliableWrite() {
		checkConnected();
		// If there's already a request in flight, return the current promise for results.
		if (executeReliableWrite != null && executeReliableWrite.isPending()) {
			return executeReliableWrite.promise();
		}
		// Execute reliable write and return promise for result.
		executeReliableWrite = new DeferredObject<Void, Integer, Void>();
		if (!gatt.executeReliableWrite()) {
			executeReliableWrite.reject(null);
		}
		return executeReliableWrite.promise();
	}

	public void requestConnectionPriority(int priority)
	{
		gatt.requestConnectionPriority(priority);
	}

	// Start reliable write transaction.
	public boolean beginReliableWrite() {
		return gatt.beginReliableWrite();
	}

	// Abort in progress reliable write transaction.
	public void abortReliableWrite() {
		gatt.abortReliableWrite();
		// TODO Should you really reject any in progress deferred?
		if (executeReliableWrite != null && executeReliableWrite.isPending()) {
			executeReliableWrite.reject(null);
		}
	}

	// Disconnect from the device.
	public void disconnect() {
		checkConnected();
		gatt.disconnect();
		if (disconnect != null && disconnect.isPending()) {
			// Resolve disconnect deferred.
			disconnect.resolve(null);
		}
	}

	// Close the device.
	public void close() {
		disconnect();
		gatt.close();
		gatt = null;
	}

	// Get service with associated UUID.
	public BluetoothGattService getService(UUID uuid) {
		checkConnected();
		// Check that services have been discovered.
		if (discoverServices == null || discoverServices.isPending() || discoverServices.isRejected()) {
			throw new RuntimeException("Service discovery was not done or failed.");
		}
		return gatt.getService(uuid);
	}

	// Get list of services.
	public List<BluetoothGattService> getServices() {
		checkConnected();
		// Check that services have been discovered.
		if (discoverServices == null || discoverServices.isPending() || discoverServices.isRejected()) {
			throw new RuntimeException("Service discovery was not done or failed.");
		}
		return gatt.getServices();
	}

	// Get device instance.
	public BluetoothDevice getDevice() {
		return gatt.getDevice();
	}

	// Read characteristic and return promise for its completion.
	public Promise<BluetoothGattCharacteristic, Integer, Void> readCharacteristic(BluetoothGattCharacteristic characteristic) {
		checkConnected();
		// If there's already a request in flight, return the current promise for results.
		DeferredObject<BluetoothGattCharacteristic, Integer, Void> deferred = readCharacteristic.get(new CharacteristicKey(characteristic));
		if (deferred != null && deferred.isPending()) {
			return deferred.promise();
		}
		// Read descriptor and return a promise for the results.
		deferred = new DeferredObject<BluetoothGattCharacteristic, Integer, Void>();
		if (!gatt.readCharacteristic(characteristic)) {
			deferred.reject(null);
		}
		readCharacteristic.put(new CharacteristicKey(characteristic), deferred);
		return deferred.promise();
	}

	// Write characteristic and return promise for its completion.
	public Promise<BluetoothGattCharacteristic, Integer, Void> writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		checkConnected();
		// If there's already a request in flight, return the current promise for results.
		DeferredObject<BluetoothGattCharacteristic, Integer, Void> deferred = writeCharacteristic.get(new CharacteristicKey(characteristic));
		if (deferred != null && deferred.isPending()) {
			return deferred.promise();
		}
		// Read descriptor and return a promise for the results.
		deferred = new DeferredObject<BluetoothGattCharacteristic, Integer, Void>();
		if (!gatt.writeCharacteristic(characteristic)) {
			deferred.reject(null);
		}
		writeCharacteristic.put(new CharacteristicKey(characteristic), deferred);
		return deferred.promise();
	}

	// Enable or disable notifications for characteristic changes.  The returned promise will notify
	// characteristic changes through its progress update.
	public Promise<Void, Void, BluetoothGattCharacteristic> setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
		checkConnected();
		DeferredObject<Void, Void, BluetoothGattCharacteristic> deferred = changeCharacteristic.get(new CharacteristicKey(characteristic));
		// Handle when enable has already been called.
		if (enable && deferred != null && deferred.isPending()) {
			// Return the in progress deferred.
			return deferred.promise();
		}
		// Handle disable for an already established notification.
		else if (!enable && deferred != null) {
			// Finish the current deferred.
			deferred.resolve(null);
			changeCharacteristic.remove(new CharacteristicKey(characteristic));
			// Disable notifications.
			deferred = new DeferredObject<Void, Void, BluetoothGattCharacteristic>();
			if (!gatt.setCharacteristicNotification(characteristic, false)) {
				deferred.reject(null);
			}
			else {
				deferred.resolve(null);
			}
			// Return a deferred to give success/failure of the disable.
			// This deferred is immediately resolved or rejected because there is no async indication
			// of the setCharacteristicNotification finishing (only progress updates).
			return deferred.promise();
		}
		// Handle enabling notifications.
		else if (enable) {
			// Setup and return the deferred for receiving progress of notification changes.
			deferred = new DeferredObject<Void, Void, BluetoothGattCharacteristic>();
			if (!gatt.setCharacteristicNotification(characteristic, true)) {
				deferred.reject(null);
			}
			changeCharacteristic.put(new CharacteristicKey(characteristic), deferred);
			return deferred.promise();
		}
		// Ignore disabling a notification that isn't enabled.
		else {
			// Return a successfull result immediately.
			deferred = new DeferredObject<Void, Void, BluetoothGattCharacteristic>();
			deferred.resolve(null);
			return deferred.promise();
		}
	}

	// Read descriptor value and return a promise for its completion.
	public Promise<BluetoothGattDescriptor, Integer, Void> readDescriptor(BluetoothGattDescriptor descriptor) {
		checkConnected();
		// If there's already a request in flight, return the current promise for results.
		DeferredObject<BluetoothGattDescriptor, Integer, Void> deferred = readDescriptor.get(new DescriptorKey(descriptor));
		if (deferred != null && deferred.isPending()) {
			return deferred.promise();
		}
		// Read descriptor and return a promise for the results.
		deferred = new DeferredObject<BluetoothGattDescriptor, Integer, Void>();
		if (!gatt.readDescriptor(descriptor)) {
			deferred.reject(null);
		}
		readDescriptor.put(new DescriptorKey(descriptor), deferred);
		return deferred.promise();
	}

	// Write descriptor value and return a promise for its completion.
	public Promise<BluetoothGattDescriptor, Integer, Void> writeDescriptor(BluetoothGattDescriptor descriptor) {
		checkConnected();
		// If there's already a request in flight, return the current promise for results.
		DeferredObject<BluetoothGattDescriptor, Integer, Void> deferred = writeDescriptor.get(new DescriptorKey(descriptor));
		if (deferred != null && deferred.isPending()) {
			return deferred.promise();
		}
		// Write descriptor and return a promise for the results.
		deferred = new DeferredObject<BluetoothGattDescriptor, Integer, Void>();
		if (!gatt.writeDescriptor(descriptor)) {
			deferred.reject(null);
		}
		writeDescriptor.put(new DescriptorKey(descriptor), deferred);
		return deferred.promise();
	}

	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
		super.onConnectionStateChange(gatt, status, newState);
		// TODO Check gatt passed in equals the expected gatt?
		if (newState == BluetoothGatt.STATE_CONNECTED && connect != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				connect.resolve(null);
			}
			else {
				connect.reject(status);
			}
		}
		else if (newState == BluetoothGatt.STATE_DISCONNECTED && disconnect != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				disconnect.resolve(null);
			}
			else {
				disconnect.reject(status);
			}
		}
	}

	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		super.onServicesDiscovered(gatt, status);
		if (discoverServices != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				discoverServices.resolve(null);
			}
			else {
				discoverServices.reject(status);
			}
		}
	}

	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicRead(gatt, characteristic, status);
		DeferredObject<BluetoothGattCharacteristic, Integer, Void> deferred = readCharacteristic.get(new CharacteristicKey(characteristic));
		if (deferred != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				deferred.resolve(characteristic);
			}
			else {
				deferred.reject(status);
			}
		}
	}

	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		super.onCharacteristicChanged(gatt, characteristic);
		DeferredObject<Void, Void, BluetoothGattCharacteristic> deferred = changeCharacteristic.get(new CharacteristicKey(characteristic));
		if (deferred != null) {
			// Send a progress update with the changed characteristic.
			deferred.notify(characteristic);
		}
	}

	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicRead(gatt, characteristic, status);
		DeferredObject<BluetoothGattCharacteristic, Integer, Void> deferred = writeCharacteristic.get(new CharacteristicKey(characteristic));
		if (deferred != null) {
			// Resolve or reject the deferred based on success or failure of the characteristic write.
			if (status == BluetoothGatt.GATT_SUCCESS) {
				deferred.resolve(characteristic);
			}
			else {
				deferred.reject(status);
			}
		}
	}

	@Override
	public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		super.onDescriptorRead(gatt, descriptor, status);
		DeferredObject<BluetoothGattDescriptor, Integer, Void> deferred = readDescriptor.get(new DescriptorKey(descriptor));
		if (deferred != null) {
			// Resolve or reject the deferred based on success or failure of the descriptor read.
			if (status == BluetoothGatt.GATT_SUCCESS) {
				deferred.resolve(descriptor);
			}
			else {
				deferred.reject(status);
			}
		}
	}

	@Override
	public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
		super.onReliableWriteCompleted(gatt, status);
		if (executeReliableWrite != null) {
			// Resolve or reject the deferred based on success or failure of the reliable write.
			if (status == BluetoothGatt.GATT_SUCCESS) {
				executeReliableWrite.resolve(null);
			}
			else {
				executeReliableWrite.reject(status);
			}
		}
	}

	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		super.onDescriptorWrite(gatt, descriptor, status);
		DeferredObject<BluetoothGattDescriptor, Integer, Void> deferred = writeDescriptor.get(new DescriptorKey(descriptor));
		if (deferred != null) {
			// Resolve or reject the deferred based on success or failure of the descriptor write.
			if (status == BluetoothGatt.GATT_SUCCESS) {
				deferred.resolve(descriptor);
			}
			else {
				deferred.reject(status);
			}
		}
	}

	@Override
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
		super.onReadRemoteRssi(gatt, rssi, status);
		if (readRemoteRssi != null) {
			// Resolve or reject the deferred based on success or failure of the RSSI read.
			if (status == BluetoothGatt.GATT_SUCCESS) {
				readRemoteRssi.resolve(rssi);
			}
			else {
				readRemoteRssi.reject(status);
			}
		}
	}
}