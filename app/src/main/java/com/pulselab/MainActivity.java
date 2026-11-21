package com.pulselab;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BLE_PERMISSION_REQUEST = 42;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String> diagnostics = new ArrayList<>();
    private BluetoothLeScanner scanner;
    private ScanCallback callback;
    private BluetoothGatt gatt;
    private BluetoothAdapter adapter;
    private boolean scanning = false, demo = false, stopped = false;
    private int tick = 0;
    private TextView mode, heart, battery, hrv, connection, log;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff8fafc);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(25),dp(20),dp(25));
        scroll.addView(root);
        root.addView(text("Pulse Lab",30,0xff172638));
        root.addView(text("WHOOP 5.0 · Experimental companion",14,0xff63748b));
        root.addView(spacer(20));
        mode = text("MODE: DEMO · SYNTHETIC DATA",15,0xff087e78);
        root.addView(mode);
        root.addView(spacer(12));
        heart = tile(root,"HEART RATE","No live reading");
        battery = tile(root,"BAND BATTERY","No live reading");
        hrv = tile(root,"HRV / RECOVERY","Unavailable · No validated RR intervals");
        connection = tile(root,"CONNECTION","Not connected");
        button(root,"Run demo",v -> startDemo());
        button(root,"Scan WHOOP (experimental)",v -> startScanning());
        button(root,"Disconnect",v -> disconnect());
        root.addView(spacer(12));
        root.addView(text("Diagnostic log · Device identifiers hidden",17,0xff172638));
        log = text("",12,0xff48586d);
        root.addView(log);
        root.addView(spacer(12));
        root.addView(text("Live mode only scans and discovers Bluetooth services. It does not decode WHOOP proprietary measurements. No internet permission. Synthetic values are not real health measurements.",12,0xff63748b));
        setContentView(scroll);
        BluetoothManager manager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
        startDemo();
    }

    private int dp(int x) { return (int)(x*getResources().getDisplayMetrics().density+0.5f); }
    private View spacer(int h) {
        View v=new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));
        return v;
    }
    private TextView text(String value,int size,int color) {
        TextView t=new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }
    private TextView tile(LinearLayout parent,String label,String value) {
        LinearLayout container=new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(15),dp(12),dp(15),dp(12));
        container.setBackgroundColor(0xffeaf0f6);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=dp(10);
        parent.addView(container,p);
        container.addView(text(label,12,0xff58697d));
        TextView result=text(value,19,0xff172638);
        container.addView(result);
        return result;
    }
    private void button(LinearLayout parent,String title,View.OnClickListener click) {
        Button button=new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setOnClickListener(click);
        parent.addView(button,new LinearLayout.LayoutParams(-1,dp(52)));
    }
    private void note(String message) {
        String safe=message.replaceAll("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}","[hidden]");
        diagnostics.add(safe);
        while(diagnostics.size()>16)diagnostics.remove(0);
        if(log!=null) log.setText(android.text.TextUtils.join("\n",diagnostics));
    }
    private final Runnable demoUpdate=new Runnable() {
        @Override public void run() {
            if(!demo || stopped)return;
            heart.setText((65+tick%9)+" bpm · SYNTHETIC");
            battery.setText((80-tick%5)+"% · SYNTHETIC");
            tick++;
            handler.postDelayed(this,1500);
        }
    };
    private void startDemo() {
        stopScan();
        closeGatt();
        demo=true;
        mode.setText("MODE: DEMO · SYNTHETIC DATA");
        connection.setText("Demo mode");
        hrv.setText("Unavailable · No validated RR intervals");
        note("Demo mode running (synthetic)");
        handler.removeCallbacks(demoUpdate);
        handler.post(demoUpdate);
    }
    private boolean hasPermissions() {
        if(Build.VERSION.SDK_INT>=31) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED &&
                   checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void requestBlePermissions() {
        if(Build.VERSION.SDK_INT>=31)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},BLE_PERMISSION_REQUEST);
        else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},BLE_PERMISSION_REQUEST);
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==BLE_PERMISSION_REQUEST) {
            if(hasPermissions())startScanning();
            else {connection.setText("Bluetooth permission denied");note("Permission denied");}
        }
    }
    private void startScanning() {
        demo=false;
        handler.removeCallbacks(demoUpdate);
        mode.setText("MODE: LIVE · EXPERIMENTAL");
        heart.setText("Unavailable · proprietary data undecoded");
        battery.setText("Unavailable · proprietary data undecoded");
        hrv.setText("Unavailable · No validated RR intervals");
        if(!hasPermissions()) {requestBlePermissions();return;}
        if(adapter==null || !adapter.isEnabled()) {
            connection.setText("Bluetooth unavailable or disabled");
            note("Bluetooth unavailable");
            return;
        }
        stopScan();
        closeGatt();
        scanner=adapter.getBluetoothLeScanner();
        if(scanner==null) {connection.setText("BLE scanner unavailable");return;}
        connection.setText("Scanning for WHOOP...");
        note("BLE scan requested");
        callback=new ScanCallback() {
            @Override public void onScanResult(int type,ScanResult result) {
                BluetoothDevice device=result.getDevice();
                if(device==null)return;
                String name=null;
                try {name=device.getName();} catch(SecurityException ex){note("Bluetooth permission revoked");return;}
                if(name==null && result.getScanRecord()!=null)name=result.getScanRecord().getDeviceName();
                if(name==null || !name.toUpperCase(Locale.US).contains("WHOOP"))return;
                note("WHOOP-named BLE device found");
                stopScan();
                connection.setText("Connecting to WHOOP...");
                try {gatt=device.connectGatt(MainActivity.this,false,gattCallback,BluetoothDevice.TRANSPORT_LE);}
                catch(SecurityException e){connection.setText("Bluetooth permission revoked");note("Connect permission revoked");}
            }
            @Override public void onScanFailed(int code) {
                scanning=false;
                connection.setText("Scan failed: "+code);
                note("BLE scan failure code "+code);
            }
        };
        try {
            scanner.startScan(callback);
            scanning=true;
            handler.postDelayed(() -> {
                if(scanning) {stopScan();connection.setText("Scan timeout · WHOOP not found");note("Scan timed out after 15 seconds");}
            },15000);
        } catch(SecurityException e) {connection.setText("Bluetooth permission revoked");note("Scan permission revoked");}
          catch(IllegalStateException e) {connection.setText("Bluetooth scanner unavailable");note("BLE state error");}
    }
    private final BluetoothGattCallback gattCallback=new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt instance,int status,int state) {
            runOnUiThread(() -> {
                if(state==BluetoothProfile.STATE_CONNECTED && status==BluetoothGatt.GATT_SUCCESS) {
                    connection.setText("Connected · discovering services");
                    note("GATT connected");
                    try {instance.discoverServices();}
                    catch(SecurityException e){note("Service discovery permission revoked");}
                } else if(state==BluetoothProfile.STATE_DISCONNECTED) {
                    connection.setText("Disconnected (GATT status "+status+")");
                    note("GATT disconnected: "+status);
                    closeGatt();
                }
            });
        }
        @Override public void onServicesDiscovered(BluetoothGatt instance,int status) {
            runOnUiThread(() -> {
                if(status!=BluetoothGatt.GATT_SUCCESS) {
                    connection.setText("Service discovery error: "+status);
                    note("GATT discovery error "+status);
                    return;
                }
                List<BluetoothGattService> services=instance.getServices();
                connection.setText("Connected · "+services.size()+" services discovered");
                note("Discovered "+services.size()+" GATT services; no proprietary decoding");
            });
        }
    };
    private void stopScan() {
        if(scanner!=null && callback!=null && scanning) {
            try {scanner.stopScan(callback);}catch(Exception ignored){}
        }
        scanning=false;
        callback=null;
    }
    private void closeGatt() {
        if(gatt!=null) {
            try {gatt.disconnect();gatt.close();}catch(Exception ignored){}
            gatt=null;
        }
    }
    private void disconnect() {
        demo=false;
        handler.removeCallbacks(demoUpdate);
        stopScan();
        closeGatt();
        mode.setText("MODE: DISCONNECTED");
        connection.setText("Disconnected");
        heart.setText("No live reading");
        battery.setText("No live reading");
        note("Disconnected by user");
    }
    @Override protected void onDestroy() {
        stopped=true;
        handler.removeCallbacksAndMessages(null);
        stopScan();
        closeGatt();
        super.onDestroy();
    }
}
