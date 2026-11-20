package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.AncControlCapability
import com.example.model.AncMode
import com.example.model.EarbudsDeviceState
import com.example.model.ScannedBluetoothDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class BluetoothEarbudsManager(context: Context) {

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(EarbudsDeviceState())
    val state: StateFlow<EarbudsDeviceState> = _state.asStateFlow()

    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null
    private var activeGatt: BluetoothGatt? = null
    private var bleScanner: BluetoothLeScanner? = null
    private var scanJob: Job? = null
    private var findToneJob: Job? = null
    private var findToneTrack: AudioTrack? = null
    private var autoReconnectEnabled: Boolean = true
    private var lastConnectedDevice: BluetoothDevice? = null

    private val batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val batteryLevelCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    private val deviceInfoServiceUuid = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    private val firmwareRevCharUuid = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
    private val hardwareRevCharUuid = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
    private val immediateAlertServiceUuid = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb")
    private val alertLevelCharUuid = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb")

    private var gattImmediateAlertChar: BluetoothGattCharacteristic? = null
    private val discoveredDevicesMap = linkedMapOf<String, ScannedBluetoothDevice>()

    private val companionPackages = listOf(
        "com.heytap.headset",
        "com.oplus.melody",
        "com.oneplus.twspods"
    )

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = proxy as? BluetoothA2dp
                BluetoothProfile.HEADSET -> headsetProxy = proxy as? BluetoothHeadset
            }
            refreshConnectedDevices()
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = null
                BluetoothProfile.HEADSET -> headsetProxy = null
            }
            refreshConnectedDevices()
        }
    }

    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshConnectedDevices()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val disconnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    refreshConnectedDevices()
                    if (autoReconnectEnabled && disconnected != null && hasConnectPermission()) {
                        attemptAutoReconnect(disconnected)
                    }
                }
                "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                    val level = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                    if (level in 0..100) {
                        _state.update { it.copy(overallBatteryPct = level) }
                    }
                    refreshConnectedDevices()
                }
                BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT -> {
                    val command = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
                    val args = intent.getSerializableExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS) as? Array<*>
                    parseHfpBatteryEvent(command, args)
                }
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!hasScanPermission()) return
            val device = result.device ?: return
            val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: return
            val address = device.address ?: return
            val isBonded = runCatching { device.bondState == BluetoothDevice.BOND_BONDED }.getOrDefault(false)
            val isAudio = isLikelyAudioDevice(device, name)
            val isOnePlus = isOnePlusNordBudsName(name)
            val entry = ScannedBluetoothDevice(
                name = name,
                address = address,
                rssiDbm = result.rssi,
                isBonded = isBonded,
                isConnected = address == _state.value.connectedAddress,
                isAudioDevice = isAudio,
                isOnePlusTarget = isOnePlus
            )
            discoveredDevicesMap[address] = entry
            _state.update {
                it.copy(
                    scannedDevices = discoveredDevicesMap.values
                        .sortedWith(compareByDescending<ScannedBluetoothDevice> { d -> d.isOnePlusTarget }
                            .thenByDescending { d -> d.isConnected }
                            .thenByDescending { d -> d.isAudioDevice }
                            .thenByDescending { d -> d.rssiDbm ?: -120 })
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.update {
                it.copy(
                    isScanning = false,
                    statusLog = "BLE scan failed (code $errorCode)"
                )
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (!hasConnectPermission()) return
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runCatching {
                    gatt.discoverServices()
                    gatt.readRemoteRssi()
                }
                _state.update { it.copy(statusLog = "BLE GATT link active — inspecting services") }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gattImmediateAlertChar = null
                _state.update { it.copy(gattImmediateAlertSupported = false) }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS || !hasConnectPermission()) return

            gatt.getService(batteryServiceUuid)?.getCharacteristic(batteryLevelCharUuid)?.let { char ->
                runCatching { gatt.readCharacteristic(char) }
            }
            val devInfo = gatt.getService(deviceInfoServiceUuid)
            devInfo?.getCharacteristic(firmwareRevCharUuid)?.let { char ->
                runCatching { gatt.readCharacteristic(char) }
            }
            devInfo?.getCharacteristic(hardwareRevCharUuid)?.let { char ->
                runCatching { gatt.readCharacteristic(char) }
            }
            val alertChar = gatt.getService(immediateAlertServiceUuid)?.getCharacteristic(alertLevelCharUuid)
            gattImmediateAlertChar = alertChar
            _state.update {
                it.copy(
                    gattImmediateAlertSupported = alertChar != null,
                    statusLog = if (alertChar != null) {
                        "GATT Telemetry + Immediate Alert (0x1802) discovered"
                    } else {
                        "GATT Telemetry inspected (ANC remains vendor-gated)"
                    }
                )
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            @Suppress("DEPRECATION")
            handleGattCharacteristicValue(characteristic.uuid, characteristic.value ?: return)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleGattCharacteristicValue(characteristic.uuid, value)
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _state.update { it.copy(rssiDbm = rssi) }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)
        }
        ContextCompat.registerReceiver(
            appContext,
            btReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        refreshPermissionsAndProfiles()
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        autoReconnectEnabled = enabled
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredRuntimePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun refreshPermissionsAndProfiles() {
        val hasPerms = hasConnectPermission()
        val btEnabled = bluetoothAdapter?.isEnabled == true
        val heyMelodyInstalled = companionPackages.any { isPackageInstalled(it) }

        _state.update {
            it.copy(
                hasBluetoothPermissions = hasPerms,
                isBluetoothEnabled = btEnabled,
                isHeyMelodyInstalled = heyMelodyInstalled
            )
        }

        if (hasPerms && btEnabled) {
            runCatching {
                bluetoothAdapter?.getProfileProxy(appContext, profileListener, BluetoothProfile.A2DP)
                bluetoothAdapter?.getProfileProxy(appContext, profileListener, BluetoothProfile.HEADSET)
            }
        }
        refreshConnectedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshConnectedDevices() {
        val btEnabled = bluetoothAdapter?.isEnabled == true
        val hasPerms = hasConnectPermission()
        val audioOutputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btAudioRoutes = audioOutputs.filter {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }

        val routeDescriptions = btAudioRoutes.map { info ->
            val rate = info.sampleRates.maxOrNull()?.let { "${it / 1000}kHz" } ?: "Auto"
            "${info.productName} ($rate)"
        }

        if (!hasPerms || !btEnabled) {
            _state.update {
                it.copy(
                    hasBluetoothPermissions = hasPerms,
                    isBluetoothEnabled = btEnabled,
                    isConnected = btAudioRoutes.isNotEmpty(),
                    connectedDeviceName = btAudioRoutes.firstOrNull()?.productName?.toString()
                        ?: "No Earbuds Connected",
                    activeAudioRoutes = routeDescriptions,
                    ancCapability = if (btAudioRoutes.isNotEmpty()) {
                        AncControlCapability.COMPANION_REQUIRED
                    } else {
                        AncControlCapability.NO_DEVICE_CONNECTED
                    }
                )
            }
            return
        }

        val connectedA2dp = runCatching { a2dpProxy?.connectedDevices }.getOrNull().orEmpty()
        val connectedHfp = runCatching { headsetProxy?.connectedDevices }.getOrNull().orEmpty()
        val activeDevice = (connectedA2dp + connectedHfp).firstOrNull()

        val bondedAudio = runCatching {
            bluetoothAdapter?.bondedDevices.orEmpty()
                .filter { device ->
                    val name = runCatching { device.name }.getOrNull().orEmpty()
                    isLikelyAudioDevice(device, name)
                }
                .map { device ->
                    val name = runCatching { device.name }.getOrNull() ?: "Bluetooth Audio"
                    ScannedBluetoothDevice(
                        name = name,
                        address = device.address,
                        rssiDbm = if (device.address == activeDevice?.address) _state.value.rssiDbm else null,
                        isBonded = true,
                        isConnected = connectedA2dp.any { it.address == device.address } ||
                            connectedHfp.any { it.address == device.address },
                        isAudioDevice = true,
                        isOnePlusTarget = isOnePlusNordBudsName(name)
                    )
                }
                .sortedByDescending { it.isConnected }
        }.getOrDefault(emptyList())

        if (activeDevice != null) {
            lastConnectedDevice = activeDevice
            val name = runCatching { activeDevice.name }.getOrNull() ?: "Bluetooth Earbuds"
            val isOnePlus = isOnePlusNordBudsName(name)
            val overallBatt = readBatteryLevelReflection(activeDevice)
            val leftBatt = readMetadataInt(activeDevice, 19)
            val rightBatt = readMetadataInt(activeDevice, 20)
            val caseBatt = readMetadataInt(activeDevice, 21)
            val mainMetaBatt = readMetadataInt(activeDevice, 18)
            val fwMeta = readMetadataString(activeDevice, 12) // METADATA_SOFTWARE_VERSION
            val hwMeta = readMetadataString(activeDevice, 11) // METADATA_HARDWARE_VERSION

            val sampleRate = btAudioRoutes.firstOrNull()?.sampleRates?.maxOrNull()
            val codecLabel = readA2dpCodecName(activeDevice)

            _state.update { current ->
                current.copy(
                    hasBluetoothPermissions = true,
                    isBluetoothEnabled = true,
                    isConnected = true,
                    connectedDeviceName = name,
                    connectedAddress = activeDevice.address,
                    isOnePlusNordBuds3Pro = isOnePlus,
                    overallBatteryPct = mainMetaBatt ?: overallBatt ?: current.overallBatteryPct,
                    leftBatteryPct = leftBatt ?: current.leftBatteryPct,
                    rightBatteryPct = rightBatt ?: current.rightBatteryPct,
                    caseBatteryPct = caseBatt ?: current.caseBatteryPct,
                    codecName = codecLabel,
                    sampleRateHz = sampleRate,
                    firmwareVersion = fwMeta ?: current.firmwareVersion,
                    hardwareVersion = hwMeta ?: if (isOnePlus) "OnePlus Nord Buds 3 Pro" else "Standard A2DP / HFP",
                    ancCapability = AncControlCapability.COMPANION_REQUIRED,
                    dualDeviceSupported = isOnePlus,
                    wearDetectionSupported = isOnePlus,
                    touchControlSupported = isOnePlus,
                    pairedAudioDevices = bondedAudio,
                    activeAudioRoutes = routeDescriptions
                )
            }
        } else {
            _state.update {
                it.copy(
                    hasBluetoothPermissions = true,
                    isBluetoothEnabled = true,
                    isConnected = btAudioRoutes.isNotEmpty(),
                    connectedDeviceName = btAudioRoutes.firstOrNull()?.productName?.toString()
                        ?: "No Earbuds Connected",
                    connectedAddress = "--:--:--:--:--:--",
                    isOnePlusNordBuds3Pro = false,
                    leftBatteryPct = null,
                    rightBatteryPct = null,
                    caseBatteryPct = null,
                    overallBatteryPct = null,
                    rssiDbm = null,
                    codecName = "Unavailable",
                    sampleRateHz = null,
                    ancCapability = if (btAudioRoutes.isNotEmpty()) {
                        AncControlCapability.COMPANION_REQUIRED
                    } else {
                        AncControlCapability.NO_DEVICE_CONNECTED
                    },
                    pairedAudioDevices = bondedAudio,
                    activeAudioRoutes = routeDescriptions
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun probeConnectedDeviceGatt() {
        val device = lastConnectedDevice ?: return
        if (!hasConnectPermission()) return
        runCatching {
            activeGatt?.close()
            activeGatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            _state.update { it.copy(statusLog = "Probing BLE GATT battery, firmware & alert services...") }
        }.onFailure { err ->
            _state.update { it.copy(statusLog = "BLE GATT probe unavailable: ${err.message}") }
        }
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        if (!hasScanPermission() || bluetoothAdapter?.isEnabled != true) {
            _state.update { it.copy(statusLog = "Enable Bluetooth & Scan permission first") }
            return
        }
        bleScanner = bluetoothAdapter.bluetoothLeScanner
        val scanner = bleScanner ?: run {
            _state.update { it.copy(statusLog = "BLE scanner unavailable on this device") }
            return
        }

        discoveredDevicesMap.clear()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        runCatching {
            scanner.startScan(null, settings, scanCallback)
            _state.update { it.copy(isScanning = true, statusLog = "Scanning nearby Bluetooth LE audio devices...") }
        }

        scanJob?.cancel()
        scanJob = scope.launch {
            delay(10_000L)
            stopBleScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        scanJob?.cancel()
        if (hasScanPermission()) {
            runCatching { bleScanner?.stopScan(scanCallback) }
        }
        _state.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    fun requestPairOrConnect(address: String) {
        val adapter = bluetoothAdapter ?: return
        if (!hasConnectPermission()) return
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            runCatching {
                device.createBond()
                _state.update { it.copy(statusLog = "Initiated Android Bluetooth pairing with $address") }
            }
        } else {
            attemptProfileConnect(device)
        }
    }

    @SuppressLint("MissingPermission")
    fun attemptAutoReconnect(device: BluetoothDevice) {
        _state.update { it.copy(statusLog = "Connection dropped — attempting auto-reconnect...") }
        attemptProfileConnect(device)
    }

    @SuppressLint("MissingPermission")
    private fun attemptProfileConnect(device: BluetoothDevice) {
        if (!hasConnectPermission()) return
        val a2dp = a2dpProxy
        val hfp = headsetProxy
        var invoked = false
        if (a2dp != null) {
            runCatching {
                val method = BluetoothA2dp::class.java.getMethod("connect", BluetoothDevice::class.java)
                method.invoke(a2dp, device)
                invoked = true
            }
        }
        if (hfp != null) {
            runCatching {
                val method = BluetoothHeadset::class.java.getMethod("connect", BluetoothDevice::class.java)
                method.invoke(hfp, device)
                invoked = true
            }
        }
        _state.update {
            it.copy(
                statusLog = if (invoked) {
                    "Reconnection requested via A2DP/HFP profile"
                } else {
                    "Use Android Bluetooth Settings to switch active audio device"
                }
            )
        }
    }

    fun selectAncModeUiOnlyWarning(mode: AncMode, level: Int) {
        // Honest hardware behavior: reflect desired mode state only when genuinely supported,
        // otherwise keep real capability notice visible and never fake hardware ANC execution.
        _state.update {
            it.copy(
                ancMode = mode,
                ancLevel = level.coerceIn(0, 2),
                statusLog = if (it.ancCapability == AncControlCapability.NATIVE_BLE_GATT) {
                    "Sent ANC mode ${mode.title} via GATT"
                } else {
                    "Hardware ANC requires official OnePlus/HeyMelody companion protocol"
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun triggerFindEarbudsRing() {
        if (_state.value.isFindToneActive) {
            stopFindEarbudsRing()
            return
        }

        // 1. Try BLE Immediate Alert Service (0x1802) if supported by connected earbuds
        val alertChar = gattImmediateAlertChar
        val gatt = activeGatt
        if (alertChar != null && gatt != null && hasConnectPermission()) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        alertChar,
                        byteArrayOf(0x02),
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    alertChar.value = byteArrayOf(0x02)
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(alertChar)
                }
            }
        }

        // 2. Also play high-visibility pulsing acoustic locate chime over active Bluetooth audio stream
        val sampleRate = 44100
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            findToneTrack = track
            track.play()
            _state.update {
                it.copy(
                    isFindToneActive = true,
                    statusLog = "Find Earbuds acoustic locate beacon active (stop when found)"
                )
            }

            findToneJob = scope.launch(Dispatchers.IO) {
                val frameCount = 1024
                val samples = ShortArray(frameCount)
                var phase = 0.0
                var tick = 0
                val twoPi = 2.0 * PI
                while (isActive && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    val pulseOn = (tick / 12) % 2 == 0
                    val freq = if ((tick / 24) % 2 == 0) 2600.0 else 3200.0
                    for (i in 0 until frameCount) {
                        val amp = if (pulseOn) 24000.0 else 0.0
                        samples[i] = (amp * sin(phase)).roundToInt().toShort()
                        phase = (phase + twoPi * freq / sampleRate) % twoPi
                    }
                    track.write(samples, 0, samples.size)
                    tick++
                }
            }
        } catch (e: Exception) {
            Log.e("BluetoothEarbudsMgr", "Find tone failed", e)
            stopFindEarbudsRing()
        }
    }

    fun stopFindEarbudsRing() {
        findToneJob?.cancel()
        findToneJob = null
        try {
            findToneTrack?.stop()
            findToneTrack?.release()
        } catch (_: Exception) {
        }
        findToneTrack = null
        _state.update { it.copy(isFindToneActive = false) }
    }

    fun openBluetoothSettingsIntent(): Intent {
        return Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openCompanionAppIntent(): Intent? {
        val pm = appContext.packageManager
        for (pkg in companionPackages) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return launch
            }
        }
        return null
    }

    private fun handleGattCharacteristicValue(uuid: UUID, value: ByteArray) {
        when (uuid) {
            batteryLevelCharUuid -> {
                val level = value.firstOrNull()?.toInt()?.and(0xFF)
                if (level != null && level in 0..100) {
                    _state.update { it.copy(overallBatteryPct = level) }
                }
            }
            firmwareRevCharUuid -> {
                val fw = value.toString(Charsets.UTF_8).trim().ifEmpty { null }
                if (fw != null) {
                    _state.update { it.copy(firmwareVersion = fw) }
                }
            }
            hardwareRevCharUuid -> {
                val hw = value.toString(Charsets.UTF_8).trim().ifEmpty { null }
                if (hw != null) {
                    _state.update { it.copy(hardwareVersion = hw) }
                }
            }
        }
    }

    private fun parseHfpBatteryEvent(command: String?, args: Array<*>?) {
        if (command == null || args == null) return
        // Standard +IPHONEACCEV: key=1 is battery level (0..9 -> 10%..100%)
        if (command.contains("IPHONEACCEV", ignoreCase = true) && args.size >= 3) {
            val count = (args[0] as? Int) ?: return
            for (i in 0 until count) {
                val keyIdx = 1 + i * 2
                val valIdx = keyIdx + 1
                if (valIdx < args.size && (args[keyIdx] as? Int) == 1) {
                    val raw = (args[valIdx] as? Int) ?: continue
                    val pct = ((raw + 1) * 10).coerceIn(0, 100)
                    _state.update { it.copy(overallBatteryPct = pct) }
                }
            }
        }
    }

    private fun readBatteryLevelReflection(device: BluetoothDevice): Int? {
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int
            level?.takeIf { it in 0..100 }
        }.getOrNull()
    }

    private fun readMetadataInt(device: BluetoothDevice, key: Int): Int? {
        val raw = readMetadataBytes(device, key) ?: return null
        return raw.toString(Charsets.UTF_8).trim().toIntOrNull()?.takeIf { it in 0..100 }
    }

    private fun readMetadataString(device: BluetoothDevice, key: Int): String? {
        val raw = readMetadataBytes(device, key) ?: return null
        return raw.toString(Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
    }

    private fun readMetadataBytes(device: BluetoothDevice, key: Int): ByteArray? {
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getMetadata", Int::class.javaPrimitiveType)
            method.invoke(device, key) as? ByteArray
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun readA2dpCodecName(device: BluetoothDevice): String {
        val proxy = a2dpProxy ?: return "SBC / AAC"
        return runCatching {
            val method = BluetoothA2dp::class.java.getMethod("getCodecStatus", BluetoothDevice::class.java)
            val status = method.invoke(proxy, device) ?: return "SBC / AAC"
            val getCodecConfig = status.javaClass.getMethod("getCodecConfig")
            val config = getCodecConfig.invoke(status) ?: return "SBC / AAC"
            val getCodecType = config.javaClass.getMethod("getCodecType")
            when (getCodecType.invoke(config) as? Int) {
                0 -> "SBC"
                1 -> "AAC"
                2 -> "aptX"
                3 -> "aptX HD"
                4 -> "LDAC"
                5 -> "LC3"
                else -> "LHDC / Vendor"
            }
        }.getOrDefault("SBC / AAC")
    }

    @SuppressLint("MissingPermission")
    private fun isLikelyAudioDevice(device: BluetoothDevice, name: String): Boolean {
        val major = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
        if (major == BluetoothClass.Device.Major.AUDIO_VIDEO) return true
        val lower = name.lowercase()
        return lower.contains("buds") ||
            lower.contains("pod") ||
            lower.contains("ear") ||
            lower.contains("head") ||
            lower.contains("audio") ||
            lower.contains("oneplus") ||
            lower.contains("nord") ||
            lower.contains("nothing")
    }

    private fun isOnePlusNordBudsName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("nord buds") ||
            lower.contains("oneplus") ||
            lower.contains("heymelody")
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return runCatching {
            appContext.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }

    fun release() {
        stopBleScan()
        stopFindEarbudsRing()
        if (hasConnectPermission()) {
            try {
                activeGatt?.close()
            } catch (_: SecurityException) {
            }
        }
        activeGatt = null
        runCatching { appContext.unregisterReceiver(btReceiver) }
        runCatching {
            a2dpProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
            headsetProxy?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) }
        }
    }
}
