package com.example.bluechat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private BluetoothAdapter bluetoothAdapter=null;
    private ChatUtils chatUtils = null;

    private ListView listMainChat;
    private EditText edCreateMessage;
    private Button btnSendMessage;
    private FrameLayout frameArrowBtn;

    private EditText editText;
    private FrameLayout btn;
    private RecyclerView recyclerView;
    private ArrayAdapter<String> adapterMainChat;

    private final int LOCATION_PERMISSION_REQUEST = 101;
    private final int SELECT_DEVICE = 102;
    public static  final  int MESSAGE_STATE_CHANGE = 0;
    public static  final  int MESSAGE_READ = 1;
    public static  final  int MESSAGE_WRITE = 2;
    public static  final  int MESSAGE_DEVICE_NAME = 3;
    public static  final  int MESSAGE_TOAST = 4;
    public static  final String DEVICE_NAME = "deviceName";
    public static final String TOAST ="toast";

    private String connectedDevice=null;


    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what){
                case MESSAGE_STATE_CHANGE:
                    switch (message.arg1){
                        case ChatUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case ChatUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ChatUtils.STATE_CONNECTED:
                            setState("Connected: "+connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) message.obj;
                    String inputBuffer = new String(buffer,0,message.arg1);
                    adapterMainChat.add("=>     "+inputBuffer);
//                    adapterMainChat.add(connectedDevice+": "+inputBuffer);
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 =(byte[]) message.obj;
                    String outputBuffer = new String(buffer1);
                    adapterMainChat.add("<=     "+outputBuffer);
//                    adapterMainChat.add("Me: "+outputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context,connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context,message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setState(CharSequence subTitle){
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
        context = this;

        initBluetooth();
        chatUtils = new ChatUtils(context, handler);
        init();
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void init(){
        listMainChat = findViewById(R.id.conversation);
        edCreateMessage = findViewById(R.id.msg);
//        btnSendMessage = findViewById(R.id.send);
        frameArrowBtn = findViewById(R.id.sentArrow);

//        recyclerView = findViewById(R.id.chatRecyclerView);
//        btn = findViewById(R.id.BtnFrame);
//        editText = findViewById(R.id.editText);

        adapterMainChat = new ArrayAdapter<String >(context, R.layout.messages_layout);
        listMainChat.setAdapter(adapterMainChat);
        
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        String [] array = {"Hello","Ram","How are You?","I am going to Agra, Lucknow, GomtiNagar"};
//        CustomAdapter cd = new CustomAdapter(array);
//        recyclerView.setAdapter(cd);

        frameArrowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String our_message = edCreateMessage.getText().toString();
                if (!our_message.isEmpty()){
                    edCreateMessage.setText("");
//                    edCreateMessage.setGravity(Gravity.END);
                    chatUtils.writemsg(our_message.getBytes());
                }else {
                    Toast.makeText(context, "Establish Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_devices:
                showDevicesList();
//                Toast.makeText(context, "Clicked for Search Devices", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_enable_bluetooth:
                    enableBluetooth();
//                Toast.makeText(context,"Clicked for Enable Bluetooth",Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void showDevicesList() {
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(intent,SELECT_DEVICE);
    }

    private void enableBluetooth() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
           if(Build.VERSION.SDK_INT <= 31){
               if (!bluetoothAdapter.isEnabled()) {
//            Toast.makeText(context, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
//             bluetoothAdapter.enable();
                   Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                   startActivityForResult(enableBtIntent, LOCATION_PERMISSION_REQUEST);
               }else {
                   if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                       Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                       discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                       context.startActivity(discoveryIntent);
                   }
               }
           }else {
               if (!bluetoothAdapter.isEnabled()){
                   Toast.makeText(context, "Open Bluetooth", Toast.LENGTH_SHORT).show();
               }else {
                   Toast.makeText(context, "Bluetooth is already opened", Toast.LENGTH_SHORT).show();
               }
           }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetooth();
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required, please grant!")
                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                enableBluetooth();
                            }
                        })
                        .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                MainActivity.this.finish();
                            }
                        }).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            assert data != null;
            String address = data.getStringExtra("deviceAddress");
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(address));
//            Toast.makeText(context, "Address: " + address, Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void onDestroy(){
        super.onDestroy();
        if(chatUtils!=null){
            chatUtils.stop();
        }
    }

}