package utils;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.example.xdyblaster.ble.BleManager;
import com.example.xdyblaster.util.FileFunc;
import com.example.xdyblaster.util.SharedPreferencesUtils;
import com.example.xdyblaster.util.UartData;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

//import android_serialport_api.SerialPort;

import cn.pda.serialport.SerialPort;

import static com.example.xdyblaster.util.AppConstants.DEV_TEST_MODE;
import static com.example.xdyblaster.util.AppConstants.DEV_UPDATE;
import static com.example.xdyblaster.util.AppConstants.MODBUS_CMD;
import static com.example.xdyblaster.util.AppConstants._KEY_BARCODE;
import static com.example.xdyblaster.util.AppConstants._KEY_KEYDATA;
import static com.example.xdyblaster.util.AppConstants.keyTab;

/**
 * Created by WangChaowei on 2017/12/7.
 */

public class SerialPortUtils {

    private volatile static SerialPortUtils mInstance = null;
    private Context mContext;
    public FragmentActivity mActivity;
    public BleManager bleManager = null;
    public boolean serialPortOk = false;
    public boolean bleOk = false;
    public byte[] bleReceiveBuffer = new byte[32];
    public int bleReceiveByte;
    public BleManager.OnBleListener onBleListener;

    private final String TAG = "SerialPortUtils";
//    private String pathBlaster = "/dev/ttyMT1";
//    private String pathKey = "/dev/ttyMT2";
//    public String pathBlaster = "/dev/ttyS2";
//    public String pathKey = "/dev/ttyS3";


    private int baudrate = 115200;
    public boolean serialPortStatus = false; //????????????????????????
    public boolean keyThread, threadStatus, busy, sendStop; //???????????????????????????????????????


    public SerialPort serialPort = null;
    public InputStream inputStream = null;
    public OutputStream outputStream = null;

    public SerialPort serialPortKey = null;
    public InputStream inputStreamKey = null;
    public OutputStream outputStreamKey = null;

    public OnKeyDataListener onKeyDataListener = null;
    public UartData uartData;
    public byte[] mReceiveBuffer = new byte[20];
    public int comPort, ioPort;

    public LocationManager locationManager;
    public Location latLng = null;
    public boolean newLatlng = false;
    public double lng, lat;

    private LocationListener locationListener = new LocationListener() {
        /**
         * ???????????????????????????:??????????????????????????????????????????Provider?????????????????????????????????????????????
         * @param location
         */
        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(Location location) {
            latLng = location;
            double r = FileFunc.Distance(lat, lng, latLng.getLatitude(), latLng.getLongitude());
            if (r > 100) {
                SharedPreferencesUtils.setParam(mContext, "lat", String.format("%.6f",latLng.getLatitude()));
                SharedPreferencesUtils.setParam(mContext, "lng", String.format("%.6f",latLng.getLongitude()));
            }
            lat = latLng.getLatitude();
            lng = latLng.getLongitude();
            newLatlng = true;
        }

        /**
         * GPS?????????????????????:Provider???disable???????????????????????????GPS?????????
         * @param provider
         * @param status
         * @param extras
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                //GPS??????????????????
                case LocationProvider.AVAILABLE:
                    break;
                //GPS????????????????????????
                case LocationProvider.OUT_OF_SERVICE:
                    break;
                //GPS????????????????????????
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    break;
            }
        }

        /**
         * ???????????????GPS???????????????
         * @param provider
         */
        @Override
        public void onProviderEnabled(String provider) {
        }

        /**
         * ??????????????? GPS???????????????
         * @param provider
         */
        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    public void ResetBlaster()
    {
        serialPort.setGPIOlow(ioPort);
        try {
            Thread.sleep(500);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        serialPort.setGPIOhigh(ioPort);
    }

    private SerialPortUtils(Context context) {
        mContext = context;
        mActivity = (FragmentActivity) context;

        uartData = new UartData();
        bleManager = BleManager.getInstance(mContext);
        onBleListener = new BleManager.OnBleListener() {
            @Override
            public void OnBleScanDevice(BluetoothDevice device, int rssi) {

            }

            @Override
            public void OnBleStopScan() {

            }

            @Override
            public void OnBleConnected(String name, int status) {

            }

            @Override
            public void OnBleReceiveData(byte[] data) {
                if (bleReceiveByte > 19)
                    bleReceiveByte = 0;
                System.arraycopy(data, 0, bleReceiveBuffer, 0, data.length);
                bleReceiveByte += data.length;


            }
        };
    }

    public static SerialPortUtils getInstance(Context context) {
        if (mInstance == null) {
            synchronized (SerialPortUtils.class) {
                if (mInstance == null) {
                    mInstance = new SerialPortUtils(context);
                }
            }
        }
        return mInstance;
    }

    public void initLocation() {
        // ??????????????????
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            // ???????????????????????????????????????GPS
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }


// ??????????????????
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);//?????????????????????????????????????????????????????????location???
        criteria.setAltitudeRequired(false);//???????????????
        criteria.setBearingRequired(false);//???????????????
        criteria.setCostAllowed(true);//???????????????
        criteria.setPowerRequirement(Criteria.POWER_LOW);//?????????

        // ??????????????????????????????
// ??????????????????????????????
        List<String> providerList = locationManager.getProviders(true);
// ???????????????????????????
        String provider = locationManager.getBestProvider(criteria, true);
// ??????????????????
        latLng = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (latLng != null) {
            newLatlng = true;
            SharedPreferencesUtils.setParam(mContext, "lat", String.valueOf(latLng.getLatitude()));
            SharedPreferencesUtils.setParam(mContext, "lng", String.valueOf(latLng.getLongitude()));
            lat = latLng.getLatitude();
            lng = latLng.getLongitude();

        } else {
            newLatlng = false;
            lat = Double.parseDouble((String) Objects.requireNonNull(SharedPreferencesUtils.getParam(mContext, "lat", "0")));
            lng = Double.parseDouble((String) Objects.requireNonNull(SharedPreferencesUtils.getParam(mContext, "lng", "0")));
        }

// ????????????????????????
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 20, locationListener);

    }


    /**
     * ????????????
     *
     * @return serialPort????????????
     */
    public SerialPort openSerialPortBlaster(String pathBlaster, String pathKey) {
        try {

            //serialPort = new SerialPort(new File(pathBlaster), baudrate, 0);
            serialPort = new SerialPort(comPort, baudrate, 0);
            serialPort.scaner_poweron();
            serialPort.setGPIOhigh(ioPort);
            this.serialPortStatus = true;
            threadStatus = false; //????????????

            //???????????????????????????????????????????????????????????????????????????
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();

//            serialPortKey = new SerialPort(new File(pathKey), baudrate, 0);
//            //???????????????????????????????????????????????????????????????????????????
//            inputStreamKey = serialPortKey.getInputStream();
//            outputStreamKey = serialPortKey.getOutputStream();
//            keyThread = true;
//            Thread thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    int length;
//                    byte[] mReceiveBuffer = new byte[200];
//                    while (keyThread) {
//                        try {
//                            length = inputStreamKey.available();
//                            if (length != 0) {
//                                DelayMs(4);
//                                //                            length = inputStreamKey.available();
//                                length = inputStreamKey.read(mReceiveBuffer);
////                                simulateKey(20);
//                                keyDataDecode(mReceiveBuffer, length);
////                                new Thread() {
////                                    @Override
////                                    public void run() {
////                                        execByRuntime("input keyevent 20");
////                                    }
////                                }.start();
//
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            });
//          //  thread.start();
            serialPortOk = true;

        } catch (IOException e) {
            Log.e(TAG, "openSerialPort: ?????????????????????" + e.toString());
            serialPortOk = false;
            return serialPort;
        }
        Log.d(TAG, "openSerialPort: ????????????");
        return serialPort;
    }


    /**
     * ????????????
     */
    public void closeSerialPort() {
        if (serialPortOk)
            try {
                this.keyThread = false;
                inputStream.close();
                outputStream.close();
//            inputStreamKey.close();
//            outputStreamKey.close();
                this.serialPortStatus = false;
                this.threadStatus = true; //????????????
                serialPort.close(comPort);
            } catch (IOException e) {
                Log.e(TAG, "closeSerialPort: ?????????????????????" + e.toString());
                return;
            }
        Log.d(TAG, "closeSerialPort: ??????????????????");
    }

    /**
     * ?????????????????????????????????
     *
     * @param data String????????????
     */
    public void sendSerialPort(byte[] data) {
        try {
            if (data.length > 0) {
                outputStream.write(data);
                outputStream.flush();
                Log.d(TAG, "sendSerialPort: ????????????????????????");
            }
        } catch (IOException e) {
            Log.e(TAG, "sendSerialPort: ???????????????????????????" + e.toString());
        }

    }


    public void DelayMs(int ms) {
        long end;
        end = System.currentTimeMillis() + ms;
        while (true) {
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                Log.e(TAG, "run: ?????????" + e.toString());
            }
            if (end < System.currentTimeMillis())
                return;
        }
    }

    public void DelayMsThread(int ms) {
        long end;
        end = System.currentTimeMillis() + ms;
        while (!sendStop) {
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                Log.e(TAG, "run: ?????????" + e.toString());
            }
            if (end < System.currentTimeMillis())
                return;
        }
    }

    public OnDataReceiveListener onDataReceiveListener = null;

    public interface OnDataReceiveListener {
        void ReceiveNoAck(boolean sendStop);

        void ReceiveAck(byte[] data);
    }

    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }


    public void SsendCommand(final byte cmd, byte rw, byte id) {
        Thread thread;
        if (busy) {
            Log.e("uart", "busy no send");
            return;
        }
        if (cmd != DEV_UPDATE)
            uartData.initData(cmd, rw, id);
        if (bleOk) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] tmp;
                    int length = 0;
                    long time;
                    busy = true;
                    sendStop = false;
                    bleReceiveByte = 0;
                    bleManager.onBleListener = onBleListener;
                    bleManager.SendData(uartData.m_uartCmdBuffer);
                    if ((cmd == DEV_UPDATE) || (cmd == DEV_TEST_MODE))
                        time = System.currentTimeMillis() + 5000;
                    else
                        time = System.currentTimeMillis() + 500;
                    while (!sendStop) {
                        if (bleReceiveByte == 19) {
                            System.arraycopy(bleReceiveBuffer, 0, mReceiveBuffer, 0, 19);
                            break;
                        }
                        if (bleReceiveByte > 19) {
                            break;
                        }
                        DelayMsThread(2);
                        if (time < System.currentTimeMillis()) {
                            bleReceiveByte = 0;
                            break;
                        }
                    }
                    busy = false;
                    if ((bleReceiveByte == 19) && UartData.CheckCrc(mReceiveBuffer, bleReceiveByte) && mReceiveBuffer[MODBUS_CMD] == cmd) {
//                   Log.e("uart", "rece data "+ FileFunc.printHexBinary(mReceiveBuffer));
                        onDataReceiveListener.ReceiveAck(mReceiveBuffer);
                    } else {
                        onDataReceiveListener.ReceiveNoAck(sendStop);
                    }
                }
            });
            thread.start();
        } else if (serialPortOk) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] tmp;
                    int length = 0;
                    long time;
                    busy = true;
                    sendStop = false;
                    //setThreadPriority(THREAD_PRIORITY_URGENT_AUDIO);
                    try {
                        while (inputStream.available() != 0) {
                            length = inputStream.available();
                            if (length != 0) {
                                inputStream.skip(length);
//                            tmp = new byte[length];
//                            inputStream.read(tmp);
                            }
                        }
//                    Log.e("uart", "send cmd " + String.valueOf(uartData.m_uartCmdBuffer[MODBUS_CMD]));
//                    Log.e("uart", "send data "+ FileFunc.printHexBinary(uartData.m_uartCmdBuffer));
                        outputStream.write(uartData.m_uartCmdBuffer, 0, uartData.length);
                        outputStream.flush();
                        if (cmd != DEV_UPDATE)
                            time = System.currentTimeMillis() + 500;
                        else
                            time = System.currentTimeMillis() + 2000;
                        while (!sendStop) {
                            length = inputStream.available();
                            if (length == 19) {
                                inputStream.read(mReceiveBuffer);
                                break;
                            }
                            if (length > 19) {
                                inputStream.skip(length);
//                            tmp = new byte[length];
//                            length = 0;
//                            inputStream.read(tmp);
                                break;
                            }
                            DelayMsThread(2);
                            if (time < System.currentTimeMillis()) {
                                length = 0;
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
//                    tcpManagerListener.ConnectSuccess(AppConstants.TCP_CONNECT_FAIL);
                    }
                    busy = false;

                    if ((length > 0) && UartData.CheckCrc(mReceiveBuffer, length) && mReceiveBuffer[MODBUS_CMD] == cmd) {
//                   Log.e("uart", "rece data "+ FileFunc.printHexBinary(mReceiveBuffer));
                        onDataReceiveListener.ReceiveAck(mReceiveBuffer);
                    } else {
                        onDataReceiveListener.ReceiveNoAck(sendStop);
                    }
                }
            });
            thread.start();
        }
    }

    private void keyDataDecode(byte[] data, int length) {
        switch (data[0]) {
            case _KEY_KEYDATA:
                if (data[2] == 1) {
                    if (onKeyDataListener != null) {
                        if (!onKeyDataListener.onKeyPush(keyTab[data[1]], data[2]))
                            simulateKey(keyTab[data[1]]);
                    } else
                        simulateKey(keyTab[data[1]]);
                }
                break;
            case _KEY_BARCODE:
                if (onKeyDataListener != null) {
                    String str;
                    int len = data[1] & 0xff;
                    str = new String(data, 2, len);
                    onKeyDataListener.onBarcodeScan(str);
                }
                break;
        }

    }

    public interface OnKeyDataListener {
        boolean onKeyPush(int key, int status);

        void onBarcodeScan(String barCode);
    }

    public static String execByRuntime(String cmd) {
        Process process = null;
        BufferedReader bufferedReader = null;
        InputStreamReader inputStreamReader = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            inputStreamReader = new InputStreamReader(process.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            int read;
            char[] buffer = new char[4096];
            StringBuilder output = new StringBuilder();
            while ((read = bufferedReader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != inputStreamReader) {
                try {
                    inputStreamReader.close();
                } catch (Throwable t) {
                }
            }
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (Throwable t) {
                }
            }
            if (null != process) {
                try {
                    process.destroy();
                } catch (Throwable t) {
                }
            }
        }
    }


    public static void simulateKey(final int KeyCode) {
        new Thread() {
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(KeyCode);
                } catch (Exception e) {
//                    Log.e("ERR", e.toString());
                }
            }
        }.start();
    }


}
