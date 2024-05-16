package com.jrfid.network.assistant

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.hzmct.enjoysdk.api.EnjoySDK
import com.hzmct.enjoysdk.transform.McErrorCode
import com.hzmct.enjoysdk.transform.McStateCode
import com.jrfid.network.assistant.ui.theme.JRINetworkAssistantTheme
import com.mc.android.mcethernet.McEthernetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {

    private var controlBoxIp by mutableStateOf(TextFieldValue("192.168.1.101", TextRange(13)))
    private var screenIp by mutableStateOf(TextFieldValue("192.168.1.102", TextRange(13)))
    private var gateway by mutableStateOf(TextFieldValue("192.168.1.1", TextRange(11)))
    private var subnetMask by mutableStateOf(TextFieldValue("255.255.255.0", TextRange(13)))
    private var isSettingUp by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            JRINetworkAssistantTheme {
                Page(
                    controlBoxIp = controlBoxIp,
                    controlBoxIpValueChange = {
                        controlBoxIp = it
                    },
                    screenIp = screenIp,
                    screenIpValueChange = {
                        screenIp = it
                    },
                    gateway = gateway,
                    gatewayValueChange = {
                        gateway = it
                    },
                    subnetMask = subnetMask,
                    subnetMaskValueChange = {
                        subnetMask = it
                    },
                    isSettingUp = isSettingUp,
                    onSettingClick = {
                        settingNetwork()
                    }
                )
            }
        }
    }

    private fun settingNetwork() {
        val controlBoxIp = controlBoxIp.text
        if (controlBoxIp.isEmpty()) {
            showToast(getString(R.string.text_input_control_box_ip))
            return
        }
        if (!controlBoxIp.isIP()) {
            showToast(getString(R.string.text_input_control_box_ip_2))
            return
        }
        val screenIp = screenIp.text
        if (screenIp.isEmpty()) {
            showToast(getString(R.string.text_input_screen_ip))
            return
        }
        if (!screenIp.isIP()) {
            showToast(getString(R.string.text_input_screen_ip_2))
            return
        }
        val gateway = gateway.text
        if (gateway.isEmpty()) {
            showToast(getString(R.string.text_input_gateway))
            return
        }
        if (!gateway.isIP()) {
            showToast(getString(R.string.text_input_gateway_2))
            return
        }
        val subnetMask = subnetMask.text
        if (subnetMask.isEmpty()) {
            showToast(getString(R.string.text_input_subnet_mask))
            return
        }
        if (!subnetMask.isIP()) {
            showToast(getString(R.string.text_input_subnet_mask_2))
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                isSettingUp = true
                val enjoySDK = EnjoySDK(applicationContext)
                var result: Int = 0
                if (!enjoySDK.checkSafeProgramOfSelf()) {
                    if (enjoySDK.securePasswdStatus == McStateCode.MC_SECURE_PASSWD_EMPTY) {
                        enjoySDK.setSecurePasswd("", "Ab88888888")
                    }
                    result = enjoySDK.registSafeProgram("Ab88888888")
                    if (result != McErrorCode.ENJOY_COMMON_SUCCESSFUL) {
                        throw Exception(getString(R.string.text_setting_error))
                    }
                }
                val config = McEthernetConfig().apply {
                    this.mode = McEthernetConfig.STATIC_MODE
                    this.ipV4Address = screenIp
                    this.gateway = gateway
                    this.subnetMask = subnetMask
                    this.dns = "8.8.8.8"
                    this.backupDns = "4.4.4.4"
                }
                result = enjoySDK.setEthernetConfig(config, "null")
                if (result != McErrorCode.ENJOY_COMMON_SUCCESSFUL) {
                    throw Exception(getString(R.string.text_setting_error))
                }
                delay(2000)
                val newIPByteArray = controlBoxIp.split(regex = Regex("\\."), 4).map { it.toInt().toByte() }.toByteArray().reversedArray()
                val newGatewayByteArray = gateway.split(Regex("\\."), 4).map { it.toInt().toByte() }.toByteArray().reversedArray()
                val newSubnetMaskByteArray = subnetMask.split(Regex("\\."), 4).map { it.toInt().toByte() }.toByteArray().reversedArray()
                var macByteArray: ByteArray = ByteArray(6)
                val userPsw = byteArrayOf(0x61, 0x64, 0x6D, 0x69, 0x6E, 0x00, 0x61, 0x64, 0x6D, 0x69, 0x6E, 0x00)
                withTimeout(10000) {
                    var sendData = UDPData.scanDevice()
                    val address = InetAddress.getByName("255.255.255.255")
                    val port = 1500
                    val sendPacket = DatagramPacket(sendData, sendData.size, address, port)
                    val udpSocket = DatagramSocket(null)
                    udpSocket.reuseAddress = true
                    udpSocket.soTimeout = 2000
                    udpSocket.bind(InetSocketAddress(port))
                    while (isActive) {
                        udpSocket.send(sendPacket)
                        val temp = ByteArray(1024)
                        val receivePacket = DatagramPacket(temp, temp.size)
                        udpSocket.receive(receivePacket)
                        if (receivePacket.address == address) {
                            continue
                        }
                        val receivedData = ByteArray(receivePacket.length)
                        System.arraycopy(temp, 0, receivedData, 0, receivedData.size)
                        if (receivedData.size == 36 && receivedData[2] == UDPData.CMD_SCAN_DEVICE) {
                            macByteArray = receivedData.copyOfRange(9, 15)
                            sendData = UDPData.basicSetting(macByteArray, userPsw, newIPByteArray, newGatewayByteArray, newSubnetMaskByteArray)
                            sendPacket.data = sendData
                            break
                        }
                    }
                    //设置基本参数
                    while (isActive) {
                        udpSocket.send(sendPacket)
                        val temp = ByteArray(1024)
                        val receivePacket = DatagramPacket(temp, temp.size)
                        udpSocket.receive(receivePacket)
                        if (receivePacket.address == address) {
                            continue
                        }
                        val receivedData = ByteArray(receivePacket.length)
                        System.arraycopy(temp, 0, receivedData, 0, receivedData.size)
                        if (receivedData.size == 4 && receivedData[2] == UDPData.CMD_BASIC_SET) {
                            break
                        }
                    }
                    //基本设置成功，开始串口设置
                    val serverIp = screenIp.toByteArray().toMutableList()
                    while (serverIp.size < 30) {
                        serverIp.add(0x00)
                    }
                    sendData = UDPData.comSetting(macByteArray, userPsw, serverIp.toByteArray())
                    sendPacket.data = sendData
                    while (isActive) {
                        udpSocket.send(sendPacket)
                        val temp = ByteArray(1024)
                        val receivePacket = DatagramPacket(temp, temp.size)
                        udpSocket.receive(receivePacket)
                        if (receivePacket.address == address) {
                            continue
                        }
                        val receivedData = ByteArray(receivePacket.length)
                        System.arraycopy(temp, 0, receivedData, 0, receivedData.size)
                        if (receivedData.size == 4 && receivedData[2] == UDPData.CMD_COM_SET) {
                            break
                        }
                    }
                    //串口配置成功，开始重启模块
                    sendData = UDPData.rebootDevice(macByteArray, userPsw)
                    sendPacket.data = sendData
                    while (isActive) {
                        udpSocket.send(sendPacket)
                        val temp = ByteArray(1024)
                        val receivePacket = DatagramPacket(temp, temp.size)
                        udpSocket.receive(receivePacket)
                        if (receivePacket.address == address) {
                            continue
                        }
                        val receivedData = ByteArray(receivePacket.length)
                        System.arraycopy(temp, 0, receivedData, 0, receivedData.size)
                        if (receivedData.size == 4 && receivedData[2] == UDPData.CMD_COM_SET) {
                            break
                        }
                    }
                    //重启完成，配网成功
                    withContext(Dispatchers.Main) {
                        showToast(getString(R.string.text_setting_success))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast(getString(R.string.text_setting_error))
                }
            } finally {
                isSettingUp = false
            }
        }
    }

    private fun String.isIP(): Boolean {
        return Pattern.matches("((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)", this)
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun Page(
    controlBoxIp: TextFieldValue,
    controlBoxIpValueChange: (TextFieldValue) -> Unit,
    screenIp: TextFieldValue,
    screenIpValueChange: (TextFieldValue) -> Unit,
    gateway: TextFieldValue,
    gatewayValueChange: (TextFieldValue) -> Unit,
    subnetMask: TextFieldValue,
    subnetMaskValueChange: (TextFieldValue) -> Unit,
    isSettingUp: Boolean,
    onSettingClick: () -> Unit
) {
    val isImeVisible = WindowInsets.isImeVisible
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.app_name))
                },
                navigationIcon = {
                    Icon(
                        painter = painterResource(id = R.mipmap.jri_logo),
                        contentDescription = "",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    onClick = {
                        if (isImeVisible) {
                            softwareKeyboardController?.hide()
                        }
                    }
                )
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        BaseTextFieldTitle(text = stringResource(R.string.control_box_ip))
                        Spacer(modifier = Modifier.height(30.dp))
                        BaseTextFieldTitle(text = stringResource(R.string.screen_ip))
                        Spacer(modifier = Modifier.height(30.dp))
                        BaseTextFieldTitle(text = stringResource(R.string.gateway))
                        Spacer(modifier = Modifier.height(30.dp))
                        BaseTextFieldTitle(text = stringResource(R.string.subnet_mask))
                    }
                    Spacer(modifier = Modifier.width(30.dp))
                    Column {
                        BaseOutlinedTextField(value = controlBoxIp, onValueChange = controlBoxIpValueChange)
                        Spacer(modifier = Modifier.height(30.dp))
                        BaseOutlinedTextField(value = screenIp, onValueChange = screenIpValueChange)
                        Spacer(modifier = Modifier.height(30.dp))
                        BaseOutlinedTextField(value = gateway, onValueChange = gatewayValueChange)
                        Spacer(modifier = Modifier.height(30.dp))
                        BaseOutlinedTextField(value = subnetMask, onValueChange = subnetMaskValueChange, imeAction = ImeAction.Done)
                    }
                }
            }
            item {
                OutlinedButton(
                    modifier = Modifier
                        .padding(top = 50.dp)
                        .size(220.dp, 60.dp),
                    onClick = onSettingClick
                ) {
                    Text(text = stringResource(R.string.btn_text_setting))
                }
            }
        }
    }
    if (isSettingUp) {
        AlertDialog(
            onDismissRequest = { /*TODO*/ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier
                .background(color = Color.White, shape = RoundedCornerShape(10.dp))
                .padding(30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(60.dp))
                Spacer(modifier = Modifier.width(20.dp))
                Text(text = "Waiting for settings...", fontSize = 16.sp, color = Color.Black)
            }
        }
    }
}

@Composable
private fun BaseTextFieldTitle(text: String) {
    Box(modifier = Modifier.height(60.dp), contentAlignment = Alignment.CenterStart) {
        Text(text = text, style = TextStyle(fontSize = 20.sp, color = Color.Black))
    }
}

@Composable
private fun BaseOutlinedTextField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, imeAction: ImeAction = ImeAction.Next) {
    OutlinedTextField(
        value = value,
        textStyle = TextStyle(fontSize = 22.sp),
        onValueChange = onValueChange,
        modifier = Modifier
            .width(460.dp)
            .height(60.dp),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Black,
            unfocusedIndicatorColor = Color.DarkGray,
            unfocusedContainerColor = Color.Unspecified,
            focusedContainerColor = Color.Unspecified
        ),
        shape = RoundedCornerShape(4.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        )
    )
}

@Preview(showBackground = true, device = Devices.TABLET)
@Composable
fun GreetingPreview() {
    JRINetworkAssistantTheme {
        Page(controlBoxIp = TextFieldValue(),
            controlBoxIpValueChange = {},
            screenIp = TextFieldValue(),
            screenIpValueChange = {},
            gateway = TextFieldValue(),
            gatewayValueChange = {},
            subnetMask = TextFieldValue(),
            subnetMaskValueChange = {},
            isSettingUp = true,
            onSettingClick = {}
        )
    }
}