package com.linkzill.bletest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.linkzill.bletest.interfaces.IDeviceSearchListener;
import com.linkzill.bletest.utils.DataTransform;
import com.linkzill.bletest.utils.Permission;
import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.baseble.utils.BleUtil;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;
import com.zyao89.view.zloading.ZLoadingDialog;
import com.zyao89.view.zloading.Z_TYPE;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int CODE_BLE_RECV = 0x01;
    public final int GPS_REQUEST_CODE = 0x02;
    private final int CODE_BLE_SEND = 0x03;

    private final int send_max_len = 244; //发送最大长度
    private final int send_min_len = 20; //发送最小长度
    private boolean isChangeMTU = false;

    private final String uuid_str = "0000e0ff-3c17-d293-8e48-14fe2e4da212"; //SERVICE UUID
    private final String characteristic_write_str = "0000ffe1-0000-1000-8000-00805f9b34fb"; //WIRTE
    private final String characteristic_notify_str = "0000ffe2-0000-1000-8000-00805f9b34fb"; //NOTIFY

    private Context mContext;
    private View rootView;
    private MyApplication myApplication;

    private TextView deviceTv;
    private Button connBtn;
    private DeviceDialog deviceDialog;
    private Button sendBtn;
    private RadioButton asciiBtn;
    private RadioButton hexBtn;
    private RadioButton recvAsciiBtn;
    private RadioButton recvHexBtn;
    private EditText inputEdit;
    private ListView recvLv;
    private Button clearBtn;
    private ZLoadingDialog connectDialog;
    private TextView recvInfoTv;
    private TextView sendInfoTv;

    private BluetoothLeDevice bleDeivce;
    private DeviceMirror bleDeviceMirror; //发送&接收数据

    private UUID serviceUUID;
    private UUID characteristicWriteUUID;
    private UUID characteristicNotifyUUID;

    private ArrayAdapter<String> recvDataAdapter;
    private ArrayList<String> recvDataList = new ArrayList<>();

    private int recvLen;
    private int sendLen;

    /**
     * 设备选择回调
     */
    private IDeviceSearchListener deviceSearchListener = new IDeviceSearchListener() {
        @Override
        public void deviceSelected(BluetoothLeDevice device) {
            ViseLog.i("%s\n%s", device.getName(), device.getAddress());
            ViseBle.getInstance().stopScan(scanCallback); //停止扫描
            bleDeivce = device;
            connectDialog.show();
            ViseBle.getInstance().connect(device, connectCallback); //连接选择的设备
        }

        @Override
        public void deviceScanCancel() {
            ViseBle.getInstance().stopScan(scanCallback); //停止扫描
        }
    };

    /**
     * 扫描回调
     */
    private ScanCallback scanCallback = new ScanCallback(new IScanCallback() {
        @Override
        public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
            ViseLog.i("BLE device name: %s",bluetoothLeDevice.getName());
            if(deviceDialog != null){
                deviceDialog.addDevice(bluetoothLeDevice);
            }
        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
            ViseLog.i("BLE scan finish");
        }

        @Override
        public void onScanTimeout() {
            ViseLog.i("BLE scan timeout");
        }
    });

    /**
     * 连接回调
     */
    private IConnectCallback connectCallback = new IConnectCallback() {
        @Override
        public void onConnectSuccess(DeviceMirror deviceMirror) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectDialog.cancel();
                    deviceTv.setText(bleDeivce.getAddress());
                    connBtn.setText("断开");
                }
            });
            getDeviceUUID();
            toastInfoUiThread("连接成功");
        }

        @Override
        public void onConnectFailure(BleException exception) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectDialog.cancel();
                    deviceTv.setText("");
                    connBtn.setText("连接");
                }
            });
            toastInfoUiThread("连接失败");
        }

        @Override
        public void onDisconnect(boolean isActive) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    deviceTv.setText("");
                    connBtn.setText("连接");
                }
            });
            toastInfoUiThread("连接断开");
        }
    };

    /**
     * 发送接收回调
     */
    private IBleCallback bleCallback = new IBleCallback() {
        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattInfo, BluetoothLeDevice bluetoothLeDevice) {
            if (bluetoothGattInfo != null && (bluetoothGattInfo.getPropertyType() == PropertyType.PROPERTY_INDICATE
                    || bluetoothGattInfo.getPropertyType() == PropertyType.PROPERTY_NOTIFY)) {
                if (bleDeviceMirror != null) {
                    ViseLog.i("send ok...");
                    bleDeviceMirror.setNotifyListener(bluetoothGattInfo.getGattInfoKey(), recvCallback);
                }
            }
        }

        @Override
        public void onFailure(BleException exception) {

        }
    };

    /**
     * 接收数据回调
     */
    private IBleCallback recvCallback = new IBleCallback() {
        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
            String recv_str;
            recvLen += data.length; //计算接收长度
            if(recvAsciiBtn.isChecked()){
                recv_str = new String(data, StandardCharsets.UTF_8);
            }else{
                recv_str = HexUtil.encodeHexStr(data);
            }

            Message message = handler.obtainMessage();
            message.what = CODE_BLE_RECV;
            message.obj = recv_str;
            message.sendToTarget();
        }

        @Override
        public void onFailure(BleException exception) {

        }
    };

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            switch (msg.what){
                case CODE_BLE_RECV:
                    recvInfoTv.setText(String.format("%s Bytes", recvLen));
                    recvDataList.add(msg.obj.toString());
                    recvDataAdapter.notifyDataSetChanged();
                    break;
                case CODE_BLE_SEND:
                    sendInfoTv.setText(String.format("%s Bytes", sendLen));
                    break;
            }

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = LayoutInflater.from(this).inflate(R.layout.activity_main, null, false);
        setContentView(rootView);
        mContext = this.getApplicationContext();
        myApplication = (MyApplication)mContext;

        //检测定位服务/GPS
        if(!Permission.isLocServiceEnable(this)){
            ViseLog.e("定位未开启");
            openGPSSettings();
        }

        initView();
        enableBluetooth();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ViseBle.getInstance().clear();
    }

    private void initView(){

        recvDataAdapter = new ArrayAdapter<String>(this, R.layout.item_recv, recvDataList);

        deviceDialog = new DeviceDialog(this, deviceSearchListener);
        inputEdit = findViewById(R.id.input_edit);
        recvLv = findViewById(R.id.recv_lv);
        recvInfoTv = findViewById(R.id.recv_info_tv);
        sendInfoTv = findViewById(R.id.send_info_tv);

        connectDialog = new ZLoadingDialog(this);
        connectDialog.setLoadingBuilder(Z_TYPE.DOUBLE_CIRCLE)
                .setLoadingColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .setHintTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setHintText("连接中...")
                .setHintTextSize(13.67f)
                .setCanceledOnTouchOutside(false);

        deviceTv = findViewById(R.id.device_tv);
        connBtn = findViewById(R.id.conn_btn);
        connBtn.setOnClickListener(this);
        sendBtn = findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(this);
        asciiBtn = findViewById(R.id.ascii_btn);
        asciiBtn.setOnClickListener(this);
        hexBtn = findViewById(R.id.hex_btn);
        hexBtn.setOnClickListener(this);
        recvAsciiBtn = findViewById(R.id.recv_ascii_btn);
        recvAsciiBtn.setOnClickListener(this);
        recvHexBtn = findViewById(R.id.recv_hex_btn);
        recvHexBtn.setOnClickListener(this);
        clearBtn = findViewById(R.id.clear_btn);
        clearBtn.setOnClickListener(this);

        recvLv.setAdapter(recvDataAdapter);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.conn_btn:
                if(!Permission.isLocServiceEnable(this)){
                    ViseLog.e("定位未开启");
                    openGPSSettings();
                }else{
                    if(!ViseBle.getInstance().isConnect(bleDeivce)){
                        //未连接，开启扫描设备
                        deviceDialog.showDialog(connBtn);
                        ViseBle.getInstance().startScan(scanCallback);
                    }else{
                        ViseBle.getInstance().disconnect(bleDeivce);
                    }
                }
                break;
            case R.id.send_btn:
                sendData();
                break;
            case R.id.ascii_btn:
            case R.id.hex_btn:
                switchSendMode();
                break;
            case R.id.clear_btn:
                if(recvDataList.size() > 0){
                    recvDataList.clear();
                    recvDataAdapter.notifyDataSetChanged();
                }
                recvLen = 0;
                sendLen = 0;
                recvInfoTv.setText("0 Bytes");
                sendInfoTv.setText("0 Bytes");
                break;
        }
    }

    private void enableBluetooth() {
        if (!BleUtil.isBleEnable(this)) {
            BleUtil.enableBluetooth(this, 1);
        } else {
            boolean isSupport = BleUtil.isSupportBle(this);
            boolean isOpenBle = BleUtil.isBleEnable(this);
            if (isSupport) {
                ViseLog.i("设备支持蓝牙");
            } else {
                ViseLog.i("设备不支持蓝牙");
            }
            if (isOpenBle) {
                Toast.makeText(mContext, "蓝牙已打开", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "蓝牙已关闭", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toastInfoUiThread(String info){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, info, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 获取设备UUID
     */
    private void getDeviceUUID(){
        String uuid_temp;
        bleDeviceMirror = ViseBle.getInstance().getDeviceMirror(bleDeivce);
        //绑定数据通道
        for (final BluetoothGattService gattservice:bleDeviceMirror.getBluetoothGatt().getServices()
        ) {
            ViseLog.i(gattservice.getUuid().toString());
            uuid_temp = gattservice.getUuid().toString();
            if(uuid_temp.equals(uuid_str)){
                //找到设备数传通道
                serviceUUID = gattservice.getUuid();
                for (final BluetoothGattCharacteristic gattCharacteristic: gattservice.getCharacteristics()
                     ) {
                    uuid_temp = gattCharacteristic.getUuid().toString();
                    ViseLog.i(uuid_temp);
                    if(uuid_temp.equals(characteristic_write_str)){
                        //找到写入数据接口
                        characteristicWriteUUID = gattCharacteristic.getUuid();
                    }
                    if(uuid_temp.equals(characteristic_notify_str)){
                        //找到读取数据接口
                        characteristicNotifyUUID = gattCharacteristic.getUuid();
                    }
                }
            }
        }

        //绑定写入通道
        bindChannel(PropertyType.PROPERTY_WRITE, serviceUUID, characteristicWriteUUID, null, bleCallback);
        //绑定接收通道
        bindChannel(PropertyType.PROPERTY_NOTIFY, serviceUUID, characteristicNotifyUUID, null, bleCallback);
        bleDeviceMirror.registerNotify(false);
        //修改MTU长度
        isChangeMTU = bleDeviceMirror.getBluetoothGatt().requestMtu(send_max_len);
        if(isChangeMTU){
            ViseLog.e("修改MTU成功");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "修改MTU成功", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * 绑定通道
     */
    public void bindChannel(PropertyType propertyType, UUID serviceUUID,
                            UUID characteristicUUID, UUID descriptorUUID, IBleCallback callback) {
        if (bleDeviceMirror != null) {
            BluetoothGattChannel bluetoothGattChannel = new BluetoothGattChannel.Builder()
                    .setBluetoothGatt(bleDeviceMirror.getBluetoothGatt())
                    .setPropertyType(propertyType)
                    .setServiceUUID(serviceUUID)
                    .setCharacteristicUUID(characteristicUUID)
                    .setDescriptorUUID(descriptorUUID)
                    .builder();
            bleDeviceMirror.bindChannel(callback, bluetoothGattChannel);
        }
    }

    /**+++++++++++++++++++++++++++++++++++私有函数+++++++++++++++++++++++++++++++++++++**/
    /**
     * 切换发送模式
     */
    private void switchSendMode(){
        if(asciiBtn.isChecked()){
            inputEdit.setText("");
            inputEdit.setFilters(myApplication.cmdAsciiFilter);
        }else if(hexBtn.isChecked()){
            inputEdit.setText("");
            inputEdit.setFilters(myApplication.cmdHexFilter);
        }
    }

    /**
     * 发送数据
     */
    private void sendData(){
        String cmd = inputEdit.getText().toString();
        byte[] result = null;
        if(asciiBtn.isChecked()){
            result = cmd.getBytes(StandardCharsets.UTF_8);
        }else if(hexBtn.isChecked()){
            cmd = DataTransform.checkHexLength(cmd);
            inputEdit.setText(cmd);
            result = DataTransform.hexTobytes(cmd);
        }

        bleWritePack(result);
    }

    /**
     * 跳转GPS设置
     */
    private void openGPSSettings() {
        LayoutInflater factory = LayoutInflater.from(this);  //图层模板生成器句柄
        final View DialogView = factory.inflate(R.layout.dialog_gps_permission, null);  //用sname.xml模板生成视图模板
        androidx.appcompat.app.AlertDialog gps_dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(DialogView)
                // 拒绝, 退出应用
                .setNegativeButton("取消",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })

                .setPositiveButton("设置",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //跳转GPS设置界面
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(intent, GPS_REQUEST_CODE);
                            }
                        })

                .setCancelable(false)
                .show();



        Button btnPositive = gps_dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        Button btnNegative = gps_dialog.getButton(DialogInterface.BUTTON_NEGATIVE);

        btnPositive.setAllCaps(false);
        btnNegative.setAllCaps(false);

        int btnColor = ContextCompat.getColor(this, R.color.colorPrimary);
        btnPositive.setTextColor(btnColor);
        btnNegative.setTextColor(btnColor);

    }

    /**+++++++++++++++++++++++++++++++++++++发送数据+++++++++++++++++++++++++++++++++++++**/
    /**
     * 按字符串发送
     * @param data 字符串
     */
    private void bleWriteData(String data){
        try {
            bleWriteData(data.getBytes("GBK"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 按字节发送
     * @param data 字节数组
     */
    private void bleWriteData(byte[] data){
        if(bleDeviceMirror != null){
            sendLen += data.length;
            bleDeviceMirror.writeData(data);

            Message message = handler.obtainMessage();
            message.what = CODE_BLE_SEND;
            message.sendToTarget();
        }
    }

    /**
     * 大于20字节，分包发送
     * @param data
     */
    private void bleWritePack(byte[] data){
        if (dataInfoQueue != null) {
            dataInfoQueue.clear();
            dataInfoQueue = splitPacketForByte(data);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    send();
                }
            });
        }
    }

    //发送队列，提供一种简单的处理方式，实际项目场景需要根据需求优化
    private Queue<byte[]> dataInfoQueue = new LinkedList<>();
    private void send() {
        if (dataInfoQueue != null && !dataInfoQueue.isEmpty()) {
            if (dataInfoQueue.peek() != null && bleDeviceMirror != null) {
                bleWriteData(dataInfoQueue.poll());
            }
            if (dataInfoQueue.peek() != null) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        send();
                    }
                }, 5);
            }
        }
    }

    /**
     * 数据分包
     *
     * @param data
     * @return
     */
    private Queue<byte[]> splitPacketForByte(byte[] data) {
        Queue<byte[]> dataInfoQueue = new LinkedList<>();
        int pack_len = isChangeMTU ? send_max_len : send_min_len;
//        ViseLog.d("Pack len: %s", pack_len);
        if (data != null) {
            int index = 0;
            do {
                byte[] surplusData = new byte[data.length - index];
                byte[] currentData;
                System.arraycopy(data, index, surplusData, 0, data.length - index);
                if (surplusData.length <= pack_len) {
                    currentData = new byte[surplusData.length];
                    System.arraycopy(surplusData, 0, currentData, 0, surplusData.length);
                    index += surplusData.length;
                } else {
                    currentData = new byte[pack_len];
                    System.arraycopy(data, index, currentData, 0, pack_len);
                    index += pack_len;
                }
                dataInfoQueue.offer(currentData);
            } while (index < data.length);
        }
        return dataInfoQueue;
    }
}