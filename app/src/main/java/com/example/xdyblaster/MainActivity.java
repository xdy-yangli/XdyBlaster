package com.example.xdyblaster;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;
import com.example.xdyblaster.ble.BleManager;
import com.example.xdyblaster.fragment.FragmentVolt;
import com.example.xdyblaster.http.OKHttpUpdateHttpService;
import com.example.xdyblaster.system.BleActivity;
import com.example.xdyblaster.system.SystemActivity;
import com.example.xdyblaster.util.CommDetonator;
import com.example.xdyblaster.util.DataViewModel;
import com.example.xdyblaster.util.DetonatorSetting;
import com.example.xdyblaster.util.InfoDialog;
import com.example.xdyblaster.util.KeyReceiver;
import com.example.xdyblaster.util.ObservVolt;
import com.xuexiang.xhttp2.XHttp;
import com.xuexiang.xhttp2.XHttpSDK;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.entity.UpdateError;
import com.xuexiang.xupdate.listener.OnUpdateFailureListener;
import com.xuexiang.xupdate.utils.UpdateUtils;
import com.zhy.http.okhttp.OkHttpUtils;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import android_serialport_api.SerialPortFinder;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.jessyan.autosize.internal.CustomAdapt;
import okhttp3.OkHttpClient;
import pub.devrel.easypermissions.EasyPermissions;
import utils.SerialPortUtils;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_1;
import static android.view.KeyEvent.KEYCODE_2;
import static android.view.KeyEvent.KEYCODE_3;
import static android.view.KeyEvent.KEYCODE_4;
import static android.view.KeyEvent.KEYCODE_5;
import static android.view.KeyEvent.KEYCODE_6;
import static android.view.KeyEvent.KEYCODE_7;
import static android.view.KeyEvent.KEYCODE_8;
import static android.view.KeyEvent.KEYCODE_9;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static com.example.xdyblaster.util.CommDetonator.COMM_DELAY;
import static com.example.xdyblaster.util.CommDetonator.COMM_GET_BATT;
import static com.example.xdyblaster.util.CommDetonator.COMM_READ_DEV_ID;
import static com.example.xdyblaster.util.CommDetonator.COMM_READ_DEV_VER;
import static com.example.xdyblaster.util.CommDetonator.COMM_RESET;
import static com.example.xdyblaster.util.CommDetonator.COMM_STOP_OUTPUT;
import static com.example.xdyblaster.util.CommDetonator.COMM_UPDATE;
import static com.example.xdyblaster.util.FileFunc.checkFileExists;
import static com.example.xdyblaster.util.FileFunc.getSystemModel;
import static com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_NEW_VERSION;

public class MainActivity extends AppCompatActivity implements CustomAdapt, EasyPermissions.PermissionCallbacks, SerialPortUtils.OnDataReceiveListener, SerialPortUtils.OnKeyDataListener {


    public static final int REQUESTCODE_FROM_ACTIVITY = 1000;
    public static final int REQUESTCODE_NEW_FILE = 1001;
    public static final int REQUESTCODE_GET_PASSWORD = 1002;
    public static final int REQUEST_CODE_SCAN = 0x0000c0de;
    public static final int REQUESTCODE_BLE_CONNECT = 1003;
    public static final String action = "broadcast.action";
    public static final String actionScan = "broadcast.scan.barcode";
    public static final String actionStartScan = "broadcast.start.scan";

    public final static String[] perms = {
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};


    //    @BindView(R.id.imageView)
//    ImageView imageView;
    @BindView(R.id.textView_file)
    TextView textViewFile;

    @BindView(R.id.layout_delay_prj)
    LinearLayout layoutDelayPrj;
    @BindView(R.id.layout_single_test)
    LinearLayout layoutSingleTest;
    @BindView(R.id.layout_net)
    LinearLayout layoutNet;
    @BindView(R.id.layout_charge)
    LinearLayout layoutCharge;
    @BindView(R.id.layout_authorize)
    LinearLayout layoutAuthorize;
    @BindView(R.id.layout_setting)
    LinearLayout layoutSetting;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.divider6)
    View divider6;
    @BindView(R.id.icon5)
    ImageView icon5;
    @BindView(R.id.icon6)
    ImageView icon6;
    @BindView(R.id.divider7)
    View divider7;
//    @BindView(R.id.layout_update)
//    LinearLayout layoutUpdate;


    private SerialPortUtils serialPortUtils;
    private BleManager bleManager;
    private KeyReceiver keyReceiver;
    private LinkedHashMap<String, String> params;
    private DataViewModel dataViewModel;
    private SerialPortFinder serialPortFinder;
    boolean f1, f2;
    public int lcdWidth;
    InfoDialog infoDialog;
    public byte[] fileData;
    public int fileLen;


    FragmentVolt frVolt;
    CommTask commTask;
    int commErr = 0;
    Timer battTimer = null;
    TimerTask timerTask;
    LocationClient mLocClient;
    // public MyLocationListenner myListener = new MyLocationListenner();
    LatLng latLng = null;
    LocationClientOption option = new LocationClientOption();

    LocationManager locationManager;
//    Thread keyThread = new Thread(new Runnable() {
//        @Override
//        public void run() {
//            while (true) {
//                if (dataViewModel.keyF3 == 1) {
//                    dataViewModel.keyF3 = 0;
//                    Message message = new Message();
//                    message.what = 133;
//                    message.arg1 = 1;
//                    if (dataViewModel.keyHandler != null)
//                        dataViewModel.keyHandler.sendMessage(message);
//                }
//            }
//        }
//    });


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (!commTask.running) {
                        commTask.cancel(true);
                        commTask = new CommTask(MainActivity.this);
                        if (dataViewModel.devId.isEmpty())
                            commTask.execute(1, COMM_STOP_OUTPUT, COMM_READ_DEV_ID);
                        else if (dataViewModel.devVer.equals("xxxx"))
                            commTask.execute(1, COMM_STOP_OUTPUT, COMM_READ_DEV_VER);
                        else if (dataViewModel.devVer.isEmpty())
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDialog();
                                }
                            });
                            //commTask.execute(1, COMM_STOP_OUTPUT, COMM_READ_DEV_VER);
                        else
                            commTask.execute(1, COMM_STOP_OUTPUT);
                    }
                    break;
                case 131:
                    if (msg.arg1 == 1) {
                        f1 = true;
                    } else {
                        f1 = false;
                    }
                    break;

                case 132:
                    if (msg.arg1 == 1) {
                        f2 = true;
                    } else {
                        f2 = false;
                    }
                    break;
            }
        }
    };
    ObservVolt observVolt;
    boolean firstBoot;
    private SDKReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("start", "Boot ??????????????????");
        dataViewModel = new ViewModelProvider(this).get(DataViewModel.class);
        serialPortUtils = SerialPortUtils.getInstance(this);
        SDKInitializer.initialize(getApplicationContext());

//        serialPortFinder = new SerialPortFinder();
//        String[] strings = serialPortFinder.getAllDevices();
        if (getSystemModel().equals("ax6737_65_n") || getSystemModel().equals("SG7100") || getSystemModel().equals("k71v1_64_bsp")) {
            try {
                if (getSystemModel().equals("k71v1_64_bsp")) {
                    serialPortUtils.comPort = 0;
                    serialPortUtils.ioPort = 166;
                } else {
                    serialPortUtils.comPort = 13;
                    serialPortUtils.ioPort = 96;
                }
                //mSerialPort.setGPIOlow(96);
                //mSerialPort.power3v3on();
                //mSerialPort.power_3v3off();
                //mSerialPort.close(13);
            } catch (Exception e) {
                e.printStackTrace();

            }
            lcdWidth = 500;
            serialPortUtils.openSerialPortBlaster("/dev/ttyMT1", "/dev/ttyMT2");

        } else {
            serialPortUtils.openSerialPortBlaster("/dev/ttyS2", "/dev/ttyS3");
            lcdWidth = 500;
        }
        super.onCreate(savedInstanceState);
        //alPortUtils.serialPortOk=false;

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //setNavigationBarVisible(this, true);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        serialPortUtils.setOnDataReceiveListener(this);
        serialPortUtils.onKeyDataListener = this;
        serialPortUtils.mActivity = this;

        dataViewModel.dataChanged = false;
        dataViewModel.overCurrent.setValue(0);
        dataViewModel.romErr.setValue(0);
        dataViewModel.setFileName("");
        dataViewModel.enMonitorVolt = true;
        dataViewModel.battStatus = 0;
        dataViewModel.fileLoaded = false;
        dataViewModel.ver = serialPortUtils.comPort;
        dataViewModel.devVer = "xxxx";
        dataViewModel.devId = "";
//        try {
//            InputStream is = getAssets().open("code.bin");
//            int fileLen = ((is.available() + 7) / 8) * 8;
//            byte[] fileData = new byte[12];
//            if (fileLen > 0x600) {
//                is.skip(0x500);
//                is.read(fileData, 0, 12);
//                is.close();
//                dataViewModel.devVer = new String(fileData);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //comm = Comm.getInstance(this);
        //comm.setDataViewModel(dataViewModel);

//        dataViewModel.drawableG = ContextCompat.getDrawable(this, R.mipmap.a5);
//        dataViewModel.drawableR = ContextCompat.getDrawable(this, R.mipmap.a5);
//        dataViewModel.drawableB = ContextCompat.getDrawable(this, R.mipmap.a5);
//        dataViewModel.drawableGray = ContextCompat.getDrawable(this, R.mipmap.a5);
//        dataViewModel.drawableG = tintDrawable(dataViewModel.drawableG, ContextCompat.getColor(this, R.color.colorGreen));
//        dataViewModel.drawableR = tintDrawable(dataViewModel.drawableR, ContextCompat.getColor(this, R.color.colorRed));
//        dataViewModel.drawableB = tintDrawable(dataViewModel.drawableB, ContextCompat.getColor(this, R.color.colorBlue));
//        dataViewModel.drawableGray = tintDrawable(dataViewModel.drawableGray, ContextCompat.getColor(this, R.color.colorBlack3));

        if (EasyPermissions.hasPermissions(MainActivity.this, perms)) {  //?????????????????????????????????????????????????????????
            bleManager = BleManager.getInstance(MainActivity.this);
            serialPortUtils.bleManager = BleManager.getInstance(MainActivity.this);
            serialPortUtils.initLocation();


        } else {                    //?????????????????????????????????
            EasyPermissions.requestPermissions(MainActivity.this, "????????????????????????", 1, perms);
        }


        SharedPreferences mShare = getSharedPreferences("setting", Context.MODE_PRIVATE);
        SharedPreferences.Editor mEdit = mShare.edit();
        int v = mShare.getInt("file", 0);
        if (v != 0) {
            if (!checkFileExists(mShare.getString("filename", "default.net")))
                v = 0;
        }
        if (v == 0) {
            DetonatorSetting setting = new DetonatorSetting();
            setting.setHole("0");
            setting.setHoleDelay("100");
            setting.setRow("1");
            setting.setRowDelay("0");
            setting.setCnt("1");
            setting.setRowSequence(false);
//            FileFunc.makeDetonatorFile("default.net", setting);
            mEdit.putInt("file", 1);
            mEdit.putString("filename", "default.net");
            mEdit.apply();
            String str = mShare.getString("filename", "default.net");
            setOpenFileName(str);
        } else {
            String str = mShare.getString("filename", "default.net");
            setOpenFileName(str);
        }
        v = mShare.getInt("f1f2mode", -1);
        if (v == -1) {
            mEdit.putInt("f1", 50);
            mEdit.putInt("f2", 100);
            mEdit.putInt("f1f2mode", 0);
            mEdit.putInt("area", 0);
            mEdit.apply();
        }
        float volt = mShare.getFloat("volt", 0.0f);
        if (volt == 0) {
            mEdit.putFloat("volt", 24.0f);
            mEdit.apply();
            volt = 24.0f;
        }
        dataViewModel.defaultVolt = (int) (volt * 100.0);

        FragmentManager fragMe = getSupportFragmentManager();
        frVolt = (FragmentVolt) fragMe.findFragmentById(R.id.fr_volt);

        observVolt = new ObservVolt(this, frVolt, getSupportFragmentManager());
        dataViewModel.volt.observe(this, observVolt);
        commTask = new CommTask(this);
        commTask.execute(1, COMM_READ_DEV_ID, COMM_READ_DEV_VER, COMM_GET_BATT);
        dataViewModel.volt.setValue(100);
        firstBoot = true;

        // ?????? SDK ???????????????
//        IntentFilter iFilter = new IntentFilter();
//        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK);
//        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
//        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
//        mReceiver = new SDKReceiver();
//        registerReceiver(mReceiver, iFilter);

//        mLocClient = new LocationClient(this);
//        mLocClient.registerLocationListener(myListener);
//        option.setOpenGps(true); // ??????gps
//        option.setCoorType("bd09ll"); // ??????????????????
//        option.setScanSpan(1000);
//        mLocClient.setLocOption(option);
//        mLocClient.start();

//        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
//            // ???????????????????????????????????????GPS
//            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intent);
//        }
        dataViewModel.keyHandler = handler;
        dataViewModel.keyHandler = null;
        keyReceiver = new KeyReceiver(this, handler, dataViewModel);
        //     keyThread.start();

        initXHttp();
        initOKHttpUtils();
        initUpdate();
    }


    @Override
    protected void onResume() {
        super.onResume();
        serialPortUtils.onKeyDataListener = this;
        dataViewModel.keyHandler = handler;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (battTimer != null) {
            battTimer.cancel();
            timerTask.cancel();
            battTimer = null;
            timerTask = null;
        }
        commTask.cancel(true);
        serialPortUtils.closeSerialPort();
        if (bleManager != null) {
            bleManager.destroy();
//            bleManager.DisConnect();
        }
        System.exit(0);
        mLocClient.stop();
        unregisterReceiver(keyReceiver);
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            serialPortUtils.onKeyDataListener = MainActivity.this;
            String tmp = dataViewModel.getFileName();
            tmp = tmp.substring(0, tmp.lastIndexOf('.'));
            //textViewFile.setText(getResources().getText(R.string.file_name) + tmp);
            if (firstBoot) {
                firstBoot = false;
                if (!serialPortUtils.serialPortOk) {
                    Intent i = new Intent(MainActivity.this, BleActivity.class);
                    startActivityForResult(i, REQUESTCODE_BLE_CONNECT);
                }
            }


        } else {
            serialPortUtils.onKeyDataListener = null;
            //commTask.cancel(true);
        }

        if (battTimer != null) {
            battTimer.cancel();
            timerTask.cancel();
            battTimer = null;
            timerTask = null;
        }
        if (hasFocus) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = 0;
                    handler.sendMessage(message);
                }
            };
            battTimer = new Timer();
            battTimer.schedule(timerTask, 100, 4000);
        }
    }

    public static boolean checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(activity)) {
            Toast.makeText(activity, "???????????????????????????", Toast.LENGTH_SHORT).show();
            activity.startActivityForResult(
                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + activity.getPackageName())), 0);
            return false;
        }
        return true;
    }

    @OnClick({R.id.layout_single_test, R.id.layout_delay_prj, R.id.layout_net, R.id.layout_authorize, R.id.layout_setting, R.id.layout_charge})
    public void onViewClicked(View view) {
        int viewId = view.getId();
        if (!checkPermission(this))
            return;
        if (viewId != R.id.layout_authorize && viewId != R.id.layout_setting) {
            if (dataViewModel.battPercent < 30) {
                InfoDialog battInfo = new InfoDialog();
                battInfo.setTitle("??????");
                battInfo.setMessage("????????????30%???????????????");
                battInfo.setCancelable(true);
                battInfo.show(getSupportFragmentManager(), "info");
                return;

            }

        }

        switch (view.getId()) {
            case R.id.layout_single_test:
                Intent intent = new Intent(MainActivity.this, SingleActivity.class);
                startActivity(intent);
                break;
            case R.id.layout_delay_prj:

                intent = new Intent(MainActivity.this, DelayPrjActivity.class);
                startActivity(intent);
//                intent = new Intent(MainActivity.this, RegisterAndRecognizeActivity.class);
//                startActivity(intent);
                break;
//            case R.id.layout_open:
//                Intent i = new Intent(MainActivity.this, FilePickerActivity.class);
//                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
//                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
//                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
//                i.putExtra(FilePickerActivity.EXTRA_START_PATH, getSDPath() + "/xdyBlaster");//Environment.getExternalStorageDirectory().getPath());
//                startActivityForResult(i, REQUESTCODE_FROM_ACTIVITY);
//                break;
//            case R.id.layout_edit:
//                if (!dataViewModel.getFileName().isEmpty()) {
//                    intent = new Intent(MainActivity.this, EditActivity.class);
//                    startActivity(intent);
//                }
//                break;
            case R.id.layout_net:
//                Thread thread=new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        MinaClient minaClient=new MinaClient();
//                        minaClient.start();
//                    }
//                });
//                thread.start();

//
                intent = new Intent(MainActivity.this, NetActivity.class);
                startActivity(intent);
                break;
            case R.id.layout_charge:
                intent = new Intent(MainActivity.this, DetonateActivity.class);
                startActivity(intent);
                break;
            case R.id.layout_authorize:
                intent = new Intent(MainActivity.this, AuthorizeActivity.class);
                startActivity(intent);


//                if (!dataViewModel.getFileName().isEmpty()) {
//                    FragmentSelect fragmentSelect = new FragmentSelect();
//                    fragmentSelect.setCancelable(true);
//                    fragmentSelect.show(getSupportFragmentManager(), "select");
//                    fragmentSelect.setOnSelectListener(new FragmentSelect.OnSelectListener() {
//                        @Override
//                        public void onSelect(int index) {
//                            if (index == 1) {
//                                Intent intent = new Intent(MainActivity.this, AddActivity.class);
//                                startActivity(intent);
//                            }
//                            if (index == 2) {
//                                Intent intent = new Intent(MainActivity.this, ScanActivity.class);
//                                startActivity(intent);
//                            }
//                        }
//                    });
//                }
                break;

//            case R.id.layout_update:
//                if (pieVoltage.isShow || serialPortUtils.busy)
//                    break;
//                if (isPowerOn) {
//                    comm.openVolt(0);
//                } else {
//                    SharedPreferences mShare = getSharedPreferences("setting", Context.MODE_PRIVATE);
//                    float v = mShare.getFloat("volt", 24);
//                    comm.openVolt((int) (v * 100));
//                    showVoltAni = 1000;
//                    showCurrentAni = 1000;
//                }
            case R.id.layout_setting:
                intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;

//            case R.id.btPower:
//                if (btPower.isChecked()) {
//                    comm.openVolt(1600);
//                    showVoltAni = 1000;
//                    showCurrentAni = 1000;
//                } else
//                    comm.openVolt(0);
//                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);      //??????easypermission??????????????????
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);  //easypermission ????????????????????????
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("?????????")
                .setMessage("???????????????->??????->PermissionDemo->???????????????????????????????????????????????????????????????")
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, 1);
                    }
                }).show();
    }

    @Override
    public void ReceiveAck(byte[] data) {

    }

    @Override
    public void ReceiveNoAck(boolean sendStop) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        if (requestCode == REQUESTCODE_FROM_ACTIVITY && resultCode == Activity.RESULT_OK) {
//            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
//                // For JellyBean and above
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                    ClipData clip = data.getClipData();
//
//                    if (clip != null) {
//                        for (int i = 0; i < clip.getItemCount(); i++) {
//                            Uri uri = clip.getItemAt(i).getUri();
//                            // Do something with the URI
//                        }
//                    }
//                    // For Ice Cream Sandwich
//                } else {
//                    ArrayList<String> paths = data.getStringArrayListExtra
//                            (FilePickerActivity.EXTRA_PATHS);
//
//                    if (paths != null) {
//                        for (String path : paths) {
//                            Uri uri = Uri.parse(path);
//                            // Do something with the URI
//                        }
//                    }
//                }
//
//            } else {
//                String str = data.getStringExtra("file");
//                str = str.substring(str.lastIndexOf("/") + 1);
//                setOpenFileName(str);
//            }
//        }
//
//        if (requestCode == REQUESTCODE_NEW_FILE && resultCode == Activity.RESULT_OK) {
//            setOpenFileName(data.getStringExtra("file"));
//
//        }
//
//        if (requestCode == REQUESTCODE_GET_PASSWORD && resultCode == Activity.RESULT_OK) {
////            if (data.getBooleanExtra("password", false)) {
////                Intent intent = new Intent(MainActivity.this, ChargeActivity.class);
////                startActivity(intent);
////            }
//
//
//        }

    }

    public void setOpenFileName(String str) {
        dataViewModel.setFileName(str);
        String tmp;
        tmp = str.substring(0, str.lastIndexOf('.'));

//        textViewFile.setText(getResources().getText(R.string.file_name) + tmp);
        dataViewModel.InitData(dataViewModel.fileName);
//        icon4.setImageDrawable(getDrawable(R.mipmap.net_plus_s));
//        icon5.setImageDrawable(getDrawable(R.mipmap.ic_device_hub_white_48dp_s));
//        icon6.setImageDrawable(getDrawable(R.mipmap.ic_flash_on_white_48dp_s));
    }

    @Override
    public boolean onKeyPush(int key, int status) {
        boolean b = false;
        if (status == 1) {
            switch (key) {
                case KEYCODE_1:
                case KEYCODE_2:
                case KEYCODE_3:
                case KEYCODE_4:
                case KEYCODE_5:
                case KEYCODE_6:
                case KEYCODE_7:
                case KEYCODE_8:
                case KEYCODE_9:
                    b = true;
                    break;
            }
            if (b)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (key) {
                            case KEYCODE_1:
                                layoutSingleTest.performClick();
                                break;
                            case KEYCODE_2:
                                layoutDelayPrj.performClick();
                                break;
                            case KEYCODE_3:
                                layoutNet.performClick();
                                break;
                            case KEYCODE_4:
                                layoutCharge.performClick();
                                break;
                            case KEYCODE_5:
                                layoutAuthorize.performClick();
                                break;
                            case KEYCODE_6:
                                layoutSetting.performClick();
                                break;

                        }
                    }
                });
        }
        return b;
    }

    @Override
    public void onBarcodeScan(String barCode) {

    }

    /**
     * ?????????????????????????????????
     */
    public static void hideBottomNav(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(0);
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /**
     * ??????????????? ?????????
     *
     * @param activity
     */
    public static void setNavigationBarVisible(Activity activity, boolean isHide) {
        if (isHide) {
            View decorView = activity.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        } else {
            View decorView = activity.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    //    public void setPowerOffIcon() {
//        ivPower.setImageDrawable(getResources().getDrawable(R.mipmap.ic_power_red_48dp));
//        tvPower.setTextColor(getResources().getColor(R.color.red));
//        tvPower.setText("8.????????????");
//        isPowerOn = false;
//    }
//
//    public void setPowerOnIcon() {
//        ivPower.setImageDrawable(getResources().getDrawable(R.mipmap.ic_power_green_48dp));
//        tvPower.setTextColor(getResources().getColor(R.color.green));
//        tvPower.setText("8.????????????");
//        isPowerOn = true;
//    }
//
    @Override
    public boolean isBaseOnWidth() {
        return true;
    }

    @Override
    public float getSizeInDp() {
        return lcdWidth;
    }


    private class CommTask extends CommDetonator {

        public CommTask(Context context) {
            this.serialPortUtils = SerialPortUtils.getInstance(context);
            this.dataViewModel = new ViewModelProvider(serialPortUtils.mActivity).get(DataViewModel.class);
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (values[0]) {
                case COMM_READ_DEV_ID:
                    if (values[1] == 1) {
                        byte[] b = new byte[11];
                        b[0] = (byte) (values[2] & 0x0ff);
                        b[1] = (byte) ((values[2] >> 8) & 0x0ff);
                        b[2] = (byte) ((values[2] >> 16) & 0x0ff);
                        b[3] = (byte) ((values[2] >> 24) & 0x0ff);
                        b[4] = (byte) (values[3] & 0x0ff);
                        b[5] = (byte) ((values[3] >> 8) & 0x0ff);
                        b[6] = (byte) ((values[3] >> 16) & 0x0ff);
                        b[7] = (byte) ((values[3] >> 24) & 0x0ff);
                        b[8] = (byte) (values[4] & 0x0ff);
                        b[9] = (byte) ((values[4] >> 8) & 0x0ff);
                        b[10] = (byte) ((values[4] >> 16) & 0x0ff);
                        dataViewModel.devId = new String(b);
                        textViewFile.setText("ID:" + dataViewModel.devId);
                        textViewFile.setVisibility(View.VISIBLE);
                    }
                    waitPublish = false;
                    break;
                case COMM_READ_DEV_VER:
                    if (values[1] == 1) {
                        byte[] b = new byte[12];
                        b[0] = (byte) (values[2] & 0x0ff);
                        b[1] = (byte) ((values[2] >> 8) & 0x0ff);
                        b[2] = (byte) ((values[2] >> 16) & 0x0ff);
                        b[3] = (byte) ((values[2] >> 24) & 0x0ff);
                        b[4] = (byte) (values[3] & 0x0ff);
                        b[5] = (byte) ((values[3] >> 8) & 0x0ff);
                        b[6] = (byte) ((values[3] >> 16) & 0x0ff);
                        b[7] = (byte) ((values[3] >> 24) & 0x0ff);
                        b[8] = (byte) (values[4] & 0x0ff);
                        b[9] = (byte) ((values[4] >> 8) & 0x0ff);
                        b[10] = (byte) ((values[4] >> 16) & 0x0ff);
                        b[11] = (byte) ((values[4] >> 24) & 0x0ff);
                        dataViewModel.devVer = new String(b);
                        try {
                            InputStream is;
                            if (UpdateUtils.getVersionCode(MainActivity.this) > 100)
                                is = getAssets().open("code.bin");
                            else
                                is = getAssets().open("code2.bin");
                            int fileLen = ((is.available() + 7) / 8) * 8;
                            byte[] fileData = new byte[12];
                            if (fileLen > 0x600) {
                                is.skip(0x500);
                                is.read(fileData, 0, 12);
                                is.close();
                                String v = new String(fileData);
                                if (!v.equals(dataViewModel.devVer))
                                    dataViewModel.devVer = "";
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        waitPublish = false;
                    }
                    break;
                case COMM_UPDATE:
                    if (values[1] == 1) {
                        if (values[3] == 0) {
                            runOnUiThread(new Runnable() {
                                @SuppressLint("DefaultLocale")
                                @Override
                                public void run() {
                                    try {
                                        infoDialog.progressBar.setMax(fileLen);
                                        infoDialog.progressBar.setProgress(values[2]);
//                                        infoDialog.setMessageTxt(String.format("????????????%d/%d)", values[2], fileLen));
                                        //    infoDialog.setMessageTxt("????????????");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                            });
                        }
                        if (values[3] == 2) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    infoDialog.setMessageTxt("???????????????");
                                    infoDialog.setCancelable(true);
                                }
                            });
                        }
                    }
                    waitPublish = false;
                    break;
                case COMM_RESET:
                    if (values[1] == -1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                infoDialog.setMessageTxt("???????????????");
                                infoDialog.setCancelable(true);
                            }
                        });
                    }
                    break;

            }
            //super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            switch (integer) {
                case 0:
                case -1:
                    break;

            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.e("commTask", "close thread");
            serialPortUtils.sendStop = true;
        }

        @Override
        protected void onCancelled(Integer integer) {
            super.onCancelled(integer);
            Log.e("commTask", "close thread");
            serialPortUtils.sendStop = true;
        }
    }


    //    //??????????????????????????????????????????????????????????????????
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        if (newConfig.fontScale != 1)//????????????
//            getResources();
//        super.onConfigurationChanged(newConfig);
//    }
//
//
//    @Override
//    public Resources getResources() {
//        Resources res = super.getResources();
//        if (res.getConfiguration().fontScale != 1) {//????????????
//            Configuration newConfig = new Configuration();
//            newConfig.setToDefaults();//????????????
//            res.updateConfiguration(newConfig, res.getDisplayMetrics());
//        }
//        return res;
//    }
    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;

    @Override
    public void onAttachedToWindow() {
//????????????onAttachedToWindow?????????FLAG_HOMEKEY_DISPATCHED
        this.getWindow().addFlags(FLAG_HOMEKEY_DISPATCHED);
        super.onAttachedToWindow();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_DPAD_UP && event.getAction() == ACTION_DOWN) {
            if (f1 & f2)
                finish();
            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

//    public class MyLocationListenner extends BDAbstractLocationListener {
//
//        @SuppressLint("DefaultLocale")
//        @Override
//        public void onReceiveLocation(BDLocation location) {
//            // map view ???????????????????????????????????????
//            if (location == null) {
//                return;
//            }
//            latLng = new LatLng(location.getLatitude(), location.getLongitude());
//            dataViewModel.latLng = latLng;
//        }
//
//        public void onReceivePoi(BDLocation poiLocation) {
//        }
//    }

    /**
     * ?????????????????????????????? SDK key ??????????????????????????????
     */
    public class SDKReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            title.setTextColor(Color.RED);
            switch (action) {
                case SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR:
                    // ??????????????????????????????
                    title.setText("key ????????????! ????????? :"
                            + intent.getIntExtra(SDKInitializer.SDK_BROADTCAST_INTENT_EXTRA_INFO_KEY_ERROR_CODE, 0)
                            + " ; ???????????? ???"
                            + intent.getStringExtra(SDKInitializer.SDK_BROADTCAST_INTENT_EXTRA_INFO_KEY_ERROR_MESSAGE));
                    break;
                case SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK:
                    title.setText("key ????????????! ????????????????????????");
                    title.setTextColor(Color.GREEN);
                    break;
                case SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR:
                    title.setText("????????????");
                    break;
            }
        }
    }

    private void initUpdate() {
        XUpdate.get()
                .debug(true)
                .isWifiOnly(false)                                               //??????????????????wifi?????????????????????
                .isGet(true)                                                    //??????????????????get??????????????????
                .isAutoMode(false)                                              //?????????????????????????????????????????????????????????
                .param("versionCode", UpdateUtils.getVersionCode(this))  //??????????????????????????????
                .param("appKey", getPackageName())
                .setOnUpdateFailureListener(new OnUpdateFailureListener() { //?????????????????????????????????
                    @Override
                    public void onFailure(UpdateError error) {
                        error.printStackTrace();
                        if (error.getCode() == CHECK_NO_NEW_VERSION) {          //???????????????????????????
                            Toast.makeText(getApplicationContext(), "??????????????????",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .supportSilentInstall(false)                                     //??????????????????????????????????????????true
                .setIUpdateHttpService(new OKHttpUpdateHttpService())           //????????????????????????????????????????????????
                .init(this.getApplication());                                          //?????????????????????

    }

    private void initXHttp() {
        XHttpSDK.init(this.getApplication());   //????????????????????????????????????????????????
        XHttpSDK.debug("XHttp");  //???????????????????????????
        XHttp.getInstance().setTimeout(20000);
    }

    private void initOKHttpUtils() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20000L, TimeUnit.MILLISECONDS)
                .readTimeout(20000L, TimeUnit.MILLISECONDS)
                .build();
        OkHttpUtils.initClient(okHttpClient);
    }

    private void updateDialog() {
        try {
            InputStream is;
            if (UpdateUtils.getVersionCode(MainActivity.this) > 100)
                is = getAssets().open("code.bin");
            else
                is = getAssets().open("code2.bin");
            //InputStream is = getAssets().open("code.bin");
            fileLen = ((is.available() + 7) / 8) * 8;
            fileData = new byte[fileLen];
            if (is.read(fileData) != 0) {
                dataViewModel.devVer = "xxxx";
                infoDialog = new InfoDialog();
                infoDialog.setTitle("???????????????");
                infoDialog.setMessage("????????????");
                infoDialog.setProgressEnable(true);
                infoDialog.setCancelable(false);
                infoDialog.show(getSupportFragmentManager(), "info");
                commTask.cancel(true);
                commTask = new CommTask(MainActivity.this);
                commTask.setFileData(fileData, fileLen);
                commTask.execute(4, COMM_RESET, COMM_DELAY, COMM_DELAY, COMM_UPDATE, COMM_DELAY, COMM_DELAY, COMM_RESET);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}


