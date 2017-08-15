package com.linkway.Utils;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import com.linkway.Interface.MessageReturnInterface;
import com.linkway.Service.BluetoothLeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by DA_LYL on 2017/08/02.
 * 蓝牙链接
 */

public class BluetoothConnectionUtils {
    private ServiceConnection serviceConnection;//蓝牙连接服务
    private BluetoothLeService bluetoothLeService;
    private Context context;
    private MessageReturnInterface messageReturnInterface;//消息返回
    private String blueToothAddress;
    private Handler handler;
    public BluetoothConnectionUtils(Context context,MessageReturnInterface messageReturnInterface,
                                    String blueToothAddress){
        this.context=context;
        this.messageReturnInterface=messageReturnInterface;
        this.blueToothAddress=blueToothAddress;
        handler=new Handler();
        startBluetooth();
    }
    private  void startBluetooth(){
        if(TextUtils.isEmpty(blueToothAddress)){
            messageReturnInterface.Message("蓝牙地址为空");
            return;
        }
        Intent intent=new Intent(context, BluetoothLeService.class);
        // 判断蓝牙是否正常,正常进行连接,不正常关闭自己
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                bluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();
                if (!bluetoothLeService.initialize()){
                    messageReturnInterface.Message("蓝牙不正常");
                }
                bluetoothLeService.connect(blueToothAddress);
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {bluetoothLeService=null;}
        };
        context.bindService(intent, serviceConnection,BIND_AUTO_CREATE);// 去启动蓝牙服务
        context.registerReceiver(broadcastReceiver,makeGattUpdateIntentFilter());
    }
    /* 意图过滤器 */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    private BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {processData(intent);}
    };
    private void processData( Intent intent){
        String action = intent.getAction();
        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){
            messageReturnInterface.Message("ok:链接成功");
        }else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
            messageReturnInterface.Message("error:链接失败");
            if (bluetoothLeService != null) {
                // 根据蓝牙地址，建立连接
                bluetoothLeService.connect(blueToothAddress);
            }
        }else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
            if( bluetoothLeService != null) {
                List<BluetoothGattService> supportedGattServices = bluetoothLeService.getSupportedGattServices();
                if (supportedGattServices != null) {
                    displayGattServices(supportedGattServices);
                }
            }
        }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)){
            String string = intent.getExtras().getString(BluetoothLeService.EXTRA_DATA);
            messageReturnInterface.OkMessage(string);
        }
    }
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        String unknownServiceString = "unknown_service";
        String unknownCharaString = "unknown_characteristic";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();
        ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
        for (BluetoothGattService gattService : gattServices) {
            // 获取服务列表
            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            gattServiceData.add(currentServiceData);
            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                if (gattCharacteristic.getUuid().toString()
                        .equals("0000ffe1-0000-1000-8000-00805f9b34fb")) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {bluetoothLeService.readCharacteristic(gattCharacteristic);}
                    }, 200);
                    bluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }
                List<BluetoothGattDescriptor> descriptors = gattCharacteristic
                        .getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    bluetoothLeService.getCharacteristicDescriptor(descriptor);
                }
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }
    //在需要关闭连接的地方调用此方法
    public void closeTheBluetoothConnection(){
        context.unbindService(serviceConnection);
        blueToothAddress=null;
        serviceConnection=null;
        bluetoothLeService=null;
        handler=null;
    }
}
