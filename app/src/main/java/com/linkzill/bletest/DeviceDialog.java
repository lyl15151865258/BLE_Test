package com.linkzill.bletest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.linkzill.bletest.adapter.DeviceAdapter;
import com.linkzill.bletest.interfaces.IDeviceSearchListener;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.log.ViseLog;

import java.util.ArrayList;

public class DeviceDialog extends PopupWindow {

    private View contentView;
    private Context mContext;
    private ListView deviceLv;
    private Button cancelBtn;

    private IDeviceSearchListener iDeviceSearchListener;

    private DeviceAdapter deviceAdapter;
    private ArrayList<BluetoothLeDevice> deviceList = new ArrayList<>();

    public DeviceDialog(Context context, IDeviceSearchListener listener) {
        super(context);
        this.mContext = context;
        this.iDeviceSearchListener = listener;

        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setOutsideTouchable(false);
        setFocusable(false);

//        setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(mContext, R.color.colorPrimary)));
        contentView = LayoutInflater.from(context).inflate(R.layout.dialog_device_popupview, null, false);
        setContentView(contentView);

        initView();
    }

    private void initView(){
        deviceLv = contentView.findViewById(R.id.device_lv);
        cancelBtn = contentView.findViewById(R.id.cancel_btn);

        deviceAdapter = new DeviceAdapter(mContext, deviceList);
        deviceLv.setAdapter(deviceAdapter);

        deviceLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(iDeviceSearchListener != null){
                    iDeviceSearchListener.deviceSelected(deviceList.get(position));
                }
                closeDialog();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iDeviceSearchListener.deviceScanCancel();
                closeDialog();
            }
        });
    }

    public void showDialog(View view){
        //this.showAsDropDown(view);
        clearListView();
        showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    public void addDevice(BluetoothLeDevice device){
        boolean ishave = false;
        if(!deviceList.contains(device)){

            for (BluetoothLeDevice de: deviceList
                 ) {
                if(de.getAddress().equals(device.getAddress())){
                    ishave = true;
                    break;
                }
            }

            if(!ishave){
                deviceList.add(device);
                deviceAdapter.notifyDataSetChanged();
            }
        }
    }

    private void closeDialog(){
        this.dismiss();
    }

    private void clearListView(){
        if(deviceList.size() > 0){
            deviceList.clear();
            deviceAdapter.notifyDataSetChanged();
        }
    }

}
