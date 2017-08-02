package com.linkway.Utils;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.linkway.Interface.MessageReturnInterface;

import java.util.HashMap;

/**
 * Created by DA_LYL on 2017/08/02.
 * //USB链接Utils
 */

public class UsbConnectionUtils {
    private Context context;
    private MessageReturnInterface messageReturnInterface;
    //USB部分
    private UsbDevice device;
    private UsbInterface Interface2;//设备接口
    private UsbEndpoint epBulkOut,epBulkIn,epControl,epIntEndpointOut,epIntEndpointIn;
    private UsbDeviceConnection connection;
    private byte[] readBuffer;//读取数组
    private boolean isStop=false;

    public UsbConnectionUtils(Context context, MessageReturnInterface messageReturnInterface){
        this.context=context;
        this.messageReturnInterface=messageReturnInterface;
        startUsb();
    }
    //开启USB
    private void startUsb() {
        UsbManager systemService = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        enumerateDevice(systemService);
        getDeviceInterface();
        assignEndpoint(Interface2);
        openDevice(systemService,Interface2);
    }
    //列举设备
    private void enumerateDevice(UsbManager systemService) {
        if (systemService == null) {
            messageReturnInterface.Message("创建UsbManager失败，请重新启动应用");
        } else {
            HashMap<String, UsbDevice> deviceList = systemService.getDeviceList();
            if (!(deviceList.isEmpty())) {
                // deviceList不为空
                for (UsbDevice device : deviceList.values()) {
                    // 输出设备信息
                    //判断是否是自己的设备
                    if (device.getVendorId() == 6790 && (device.getProductId() == 29987
                            || device.getProductId() == 21795)) {
                        this.device = device; // 获取USBDevice
                        break;
                    }
                }
            } else {
                messageReturnInterface.Message("请连接USB设备");
            }
        }
    }
    //查找设备接口
    private void getDeviceInterface(){
        if (device != null) {
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface intF = device.getInterface(i);
                if (i == 0 || i==1) {
                    Interface2 = intF; // 保存设备接口
                }
            }
        } else {
            messageReturnInterface.Message("设备为空");
        }
    }
    //分配端点
    private UsbEndpoint assignEndpoint(UsbInterface mInterface){
        for (int i = 0; i < mInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = mInterface.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epBulkOut = ep;
                } else {
                    epBulkIn = ep;
                }
            }
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
                epControl = ep;
            }
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epIntEndpointOut = ep;
                }
                if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                    epIntEndpointIn = ep;
                }
            }
        }
        if (epBulkOut == null && epBulkIn == null && epControl == null
                && epIntEndpointOut == null && epIntEndpointIn == null) {
            throw new IllegalArgumentException("not endpoint is founded!");
        }
        return epIntEndpointIn;
    }
    //打开设备
    private void openDevice(UsbManager systemService, UsbInterface mInterface){
        if (mInterface != null) {
            UsbDeviceConnection conn = null;
            // 在open前判断是否有连接权限；对于连接权限可以静态分配，也可以动态分配权限
            if (systemService.hasPermission(device)) {
                conn = systemService.openDevice(device);
            }
            if (conn == null) {
                return;
            }
            if (conn.claimInterface(mInterface, true)) {
                connection = conn;
                readBuffer=new byte[64];
                messageReturnInterface.Message("open设备成功");
                isStop=false;
                connection.controlTransfer(0x41,0x9a, 0x1312, 0xb202, null, 0, 0);//Baud rate 115200
                (new Thread(new Runnable() {
                    @Override
                    public void run() {receiveData();}
                })).start();
            } else {
                conn.close();
                messageReturnInterface.Message("无法打开连接通道");
            }
        }
    }
    //子线程接收数据
    private void receiveData(){
        while (!isStop) {
            int i = connection.bulkTransfer(epBulkIn, readBuffer, readBuffer.length,
                    2000);
            if (i > 0) {
                messageReturnInterface.OkMessage(new String(readBuffer));
            }
        }
    }
    //关闭
    public void closeUsb(){
        isStop=true;
        connection.close();
        device=null;
        Interface2=null;
        epBulkOut=null;
        epBulkIn=null;
        epControl=null;
        epIntEndpointOut=null;
        epIntEndpointIn=null;
        connection=null;
        readBuffer=null;
    }
}
