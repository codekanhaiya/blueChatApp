package com.example.bluechat;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    private ListView listPairedDevices, listAvailableDevices;
    private ArrayAdapter<String> arrayAdapterPairedDevices, arrayAdapterAvailableDevices;
    private Context context;
    private ProgressBar progressBar;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
        context = this;
        init();
    }

    private void init() {
        listPairedDevices = findViewById(R.id.list_paired_devices);
        listAvailableDevices = findViewById(R.id.list_available_devices);
        progressBar = findViewById(R.id.progress_circular);

        arrayAdapterPairedDevices = new ArrayAdapter<String>(context, R.layout.device_list_item);
        arrayAdapterAvailableDevices = new ArrayAdapter<String>(context, R.layout.device_list_item);

//        arrayAdapterPairedDevices.add("Kanhaiya ji\nHello \nHow are you ?"); //---> created for example

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices != null && pairedDevices.size() >0){
            for (BluetoothDevice devices : pairedDevices){
                arrayAdapterPairedDevices.add(devices.getName()+"\n"+devices.getAddress());
            }
        }else {
            arrayAdapterPairedDevices.add("No device found");
        }

        listPairedDevices.setAdapter(arrayAdapterPairedDevices);
        listAvailableDevices.setAdapter(arrayAdapterAvailableDevices);

        listPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress",address);
                setResult(Activity.RESULT_OK , intent);
                finish();
            }
        });
        listAvailableDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Intent intent = new Intent();
                intent.putExtra("deviceAddress",address);
                setResult(Activity.RESULT_OK , intent);
                finish();
            }
        });

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(bluetoothDeviceListener, intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(bluetoothDeviceListener, intentFilter1);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_scan_devices) {
            scanDevices();
//                Toast.makeText(context, "Scan Devices Clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    arrayAdapterAvailableDevices.add(device.getName() +"\n"+ device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressBar.setVisibility(View.GONE);
                if(arrayAdapterAvailableDevices.getCount() == 0){
                    Toast.makeText(context,"No new device found", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(context,"Click on the device to start the chat",Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private void scanDevices() {
        progressBar.setVisibility(View.VISIBLE);
        arrayAdapterAvailableDevices.clear();
        Toast.makeText(context, "Scan Started", Toast.LENGTH_SHORT).show();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }
}
