package com.linkway.Fragment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.linkway.Interface.MessageReturnInterface;
import com.linkway.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by DA_LYL on 2017/08/02.
 * 蓝牙搜索界面
 */

public class BluetoothFragment extends Fragment {
    private RecyclerView rv_BlueToothAddress;
    private Button btn_Search;
    private BluetoothFragmentAdapter bluetoothFragmentAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevices;//存储蓝牙的数组
    private BluetoothAdapter bluetoothAdapter;//扫描蓝牙的对象
    private BluetoothAdapter.LeScanCallback leScanCallback;//蓝牙扫描回调
    private MyHandler myHandler;
    private MessageReturnInterface messageReturnInterface;
    private boolean blueToothEnable=false;
    public BluetoothFragment(MessageReturnInterface messageReturnInterface){
        this.messageReturnInterface=messageReturnInterface;
    }
    public BluetoothFragment(){}
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View inflate = View.inflate(getActivity(), R.layout.fragment_bluetooth, null);
        getViewObject(inflate);
        return inflate;
    }
    private void getViewObject(View view) {
        rv_BlueToothAddress = (RecyclerView)view.findViewById(R.id.fragment_bluetooth_BlueToothAddress);
        btn_Search = (Button)view.findViewById(R.id.fragment_bluetooth_Search);
    }
    @Override
    public void onStart() {
        super.onStart();
        initData();
        initEvent();
    }
    private void initData() {
        myHandler=new MyHandler(this);
        bluetoothDevices=new ArrayList<>();
        leScanCallback = new LeScanCallback();
        bluetoothAdapter=((BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        rv_BlueToothAddress.setLayoutManager(new LinearLayoutManager(getActivity()));
        rv_BlueToothAddress.setAdapter(bluetoothFragmentAdapter = new BluetoothFragmentAdapter());
    }
    private void initEvent() {
        btn_Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int bluetooth = isBluetooth();
                if(bluetooth != 0){
                    if(bluetooth==1){
                        myHandler.sendMessage(myHandler.obtainMessage(2,"当前手机不能使用蓝牙,请更换手机"));
                    }else if(bluetooth==2){
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, 0);
                    }
                    return;
                }
                startScanLeDevice();
            }
        });
    }
    //搜索蓝牙
    private void startScanLeDevice() {
        if(!blueToothEnable){
            bluetoothDevices.clear();
            myHandler.sendEmptyMessage(2);
            //开启蓝牙搜索
            blueToothEnable=true;
            btn_Search.setText(getResources().getString(R.string.fragment_bluetooth_InSearch));
            bluetoothAdapter.startLeScan(leScanCallback);
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {stopScanLeDevice();}
            }, 10000);
        }else{
            myHandler.sendMessage(myHandler.obtainMessage(0,"正在扫描蓝牙中..."));
        }
    }
    //停止搜索蓝牙
    private void stopScanLeDevice(){
        if(blueToothEnable){
            blueToothEnable = false;
            btn_Search.setText(getResources().getString(R.string.fragment_bluetooth_Search));
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
    //蓝牙搜索回调
    private class LeScanCallback implements BluetoothAdapter.LeScanCallback{
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            bluetoothDevices.add(device);
            HashSet h = new HashSet(bluetoothDevices);
            bluetoothDevices.clear();
            bluetoothDevices.addAll(h);
            myHandler.sendEmptyMessage(2);
        }
    }
    private void rootClick(int position){
        stopScanLeDevice();
        BluetoothDevice bluetoothDevice = bluetoothDevices.get(position);
        messageReturnInterface.OkMessage(bluetoothDevice.getAddress());
        myHandler.sendMessage(myHandler.obtainMessage(0,"蓝牙保存成功:"+bluetoothDevice.getName()));
        getActivity().finish();
    }
    //判断蓝牙是否能用
    public int isBluetooth(){
        int isBluetooth=0;
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            isBluetooth=1;
        }
        BluetoothAdapter bluetoothAdapter =
                ((BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            isBluetooth=2;
        }
        return isBluetooth;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        bluetoothFragmentAdapter=null;
        bluetoothDevices=null;
        bluetoothAdapter.stopLeScan(leScanCallback);
        leScanCallback=null;
        bluetoothAdapter=null;
        myHandler=null;
    }
    //Adapter
    private class BluetoothFragmentAdapter extends RecyclerView.Adapter<BluetoothFragmentAdapter.MyViewHolder> {
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.item_bluetooth_fragment, parent, false));
        }
        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {
            MyViewHolder myViewHolder = (MyViewHolder)holder;
            BluetoothDevice bluetoothDevice = bluetoothDevices.get(position);
            myViewHolder.text_Name.setText(bluetoothDevice.getName());
            myViewHolder.text_Address.setText(bluetoothDevice.getAddress());
            myViewHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {rootClick(position);}
            });
        }
        @Override
        public int getItemCount() {return bluetoothDevices.size();}
        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView text_Name;
            LinearLayout linearLayout;
            TextView text_Address;
            private MyViewHolder(View view) {
                super(view);
                text_Name= (TextView) itemView.findViewById(R.id.item_blueTooth_fragment_Name);
                text_Address=(TextView)itemView.findViewById(R.id.item_blueTooth_fragment_Address);
                linearLayout=(LinearLayout)itemView.findViewById(R.id.item_blueTooth_fragment_Root);
            }
        }
    }
    //handler
    private class MyHandler extends Handler{
        private final WeakReference<BluetoothFragment> baseActivityWeakReference;
        MyHandler(BluetoothFragment fragment) {baseActivityWeakReference = new WeakReference<>(fragment);}
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==2){
                bluetoothFragmentAdapter.notifyDataSetChanged();
            }else{
                Toast.makeText(getActivity(),msg.obj.toString(),Toast.LENGTH_SHORT).show();
            }
        }
    }
}
