package com.linkzill.bletest.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.linkzill.bletest.R;
import com.vise.baseble.model.BluetoothLeDevice;

import java.util.ArrayList;

public class DeviceAdapter extends BaseAdapter {
    private int mCurrentItem=0;
    private boolean isClick=false;

    private Context mContext;
    private ArrayList<BluetoothLeDevice> mList;

    public DeviceAdapter(Context context, ArrayList<BluetoothLeDevice> list){
        this.mContext = context;
        this.mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        return mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view==null){
            view= LayoutInflater.from(mContext).inflate(R.layout.item_device_list,null);
        }

        TextView textView= (TextView) view.findViewById(R.id.name_tv);
        TextView textView1 = (TextView)view.findViewById(R.id.detail_tv);

        textView.setText(mList.get(i).getName());
        textView1.setText(mList.get(i).getAddress());

//        if (mCurrentItem==i&&isClick){
//            textView.setTextColor(Color.parseColor("#ff6600"));
//        }else{
//            textView.setTextColor(Color.parseColor("#000000"));
//        }

        return view;
    }

    public void setCurrentItem(int currentItem){
        this.mCurrentItem=currentItem;
    }

    public void setClick(boolean click){
        this.isClick=click;
    }
}
