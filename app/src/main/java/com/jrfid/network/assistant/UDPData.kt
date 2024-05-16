package com.jrfid.network.assistant

import java.net.DatagramSocket

object UDPData {
    /**
     * 扫描设备
     */
    const val CMD_SCAN_DEVICE: Byte = 0x01

    /**
     * 重启
     */
    const val CMD_REBOOT_DEVICE: Byte = 0x02

    /**
     * 读取设置
     */
    const val CMD_READ_SET: Byte = 0x03

    /**
     * 基本设置
     */
    const val CMD_BASIC_SET: Byte = 0x05

    /**
     * 串口设置
     */
    const val CMD_COM_SET: Byte = 0x06

    fun checkSum(array: ByteArray, offset: Int, len: Int): Byte {
        var sum = 0
        for (m in offset until (offset + len)) {
            sum += array[m]
        }
        return sum.toByte()
    }

    private fun createCmd(cmd: Byte, data: ByteArray): ByteArray {
        val byteArray = ByteArray(4 + data.size)
        byteArray[0] = (0xFF).toByte()
        byteArray[1] = (data.size + 1).toByte()
        byteArray[2] = cmd
        data.forEachIndexed { index, byte ->
            byteArray[index + 3] = byte
        }
        val sum = checkSum(byteArray, 1, byteArray.size - 1)
        byteArray[byteArray.size - 1] = sum
        return byteArray
    }

    /**
     * 扫描设备
     */
    fun scanDevice(): ByteArray {
        return createCmd(CMD_SCAN_DEVICE, byteArrayOf())
    }

    /**
     * 重启设备
     */
    fun rebootDevice(mac: ByteArray, userPsw: ByteArray): ByteArray {
        val data = mutableListOf<Byte>()
        data.addAll(mac.toList())
        data.addAll(userPsw.toList())
        return createCmd(CMD_REBOOT_DEVICE, data.toByteArray())
    }

    fun basicSetting(mac: ByteArray, userPsw: ByteArray, ip: ByteArray, gateway: ByteArray, subnetMask: ByteArray): ByteArray {
        val data = mutableListOf<Byte>()
        data.addAll(mac.toList())
        data.addAll(userPsw.toList())
        data.addAll(mutableListOf(0x95.toByte(), 0x63.toByte(), 0x03.toByte()))
        //第 8 位为 0：DHCP；1：静态 IP
        //第 6 位为 0：长连接；1：短连接
        //第 5 位为 0：不清理缓存；1：清理缓存
        data.add(0x80.toByte())
        //固定值
        data.addAll(mutableListOf(0x00.toByte(), 0x00.toByte()))
        //HTTP 服务端口
        data.addAll(mutableListOf(0x50.toByte(), 0x00.toByte()))
        //固定值
        data.add(0x00.toByte())
        data.addAll(ip.toList())
        data.addAll(gateway.toList())
        data.addAll(subnetMask.toList())
        //模块名称
        data.addAll(
            mutableListOf(
                0x55.toByte(),
                0x53.toByte(),
                0x52.toByte(),
                0x2D.toByte(),
                0x54.toByte(),
                0x43.toByte(),
                0x50.toByte(),
                0x32.toByte(),
                0x33.toByte(),
                0x32.toByte(),
                0x2D.toByte(),
                0x53.toByte(),
                0x31.toByte(),
                0x00.toByte()
            )
        )
        //固定值
        data.addAll(mutableListOf(0x00.toByte(), 0x00.toByte()))
        data.addAll(userPsw.toList())
        //固定值
        data.add(0x00)
        //设备id
        data.addAll(mutableListOf(0x00.toByte(), 0x00.toByte()))
        //ucIdType
        data.add(0xB0.toByte())
        //设备Mac地址
        data.addAll(mac.toList())
        //DNS 服务器地址
        data.addAll(mutableListOf(0xDE.toByte(), 0xDE.toByte(), 0x43.toByte(), 0xD0.toByte()))
        //短连接断开时间
        data.add(0x03.toByte())
        //固定值
        data.addAll(mutableListOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        return createCmd(CMD_BASIC_SET, data.toByteArray())
    }

    fun comSetting(mac: ByteArray, userPsw: ByteArray, serverIp: ByteArray): ByteArray {
        val data = mutableListOf<Byte>()
        data.addAll(mac.toList())
        data.addAll(userPsw.toList())
        //波特率
        data.addAll(mutableListOf(0x00.toByte(), 0xC2.toByte(), 0x01, 0x00))
        //数据位，校验位，停止位
        data.addAll(mutableListOf(0x08.toByte(), 0x01.toByte(), 0x01.toByte()))
        //固定值
        data.addAll(mutableListOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()))
        //本地端口
        data.addAll(mutableListOf(0x8C.toByte(), 0x4E.toByte()))
        //远程端口
        data.addAll(mutableListOf(0x10.toByte(), 0x27.toByte()))
        data.addAll(serverIp.toList())
        data.addAll(arrayListOf(0x00, 0x00, 0x00, 0x00))
        data.addAll(arrayListOf(0x28.toByte(), 0x01.toByte(), 0x00, 0x04, 0x10, 0x0E))
        data.addAll(arrayListOf(0x00, 0x00, 0x00, 0x90.toByte(), 0x01, 0x00, 0x00))
        return createCmd(CMD_COM_SET, data.toByteArray())
    }

}