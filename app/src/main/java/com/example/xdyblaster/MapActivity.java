package com.example.xdyblaster;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ZoomControls;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.example.xdyblaster.util.DataViewModel;
import com.example.xdyblaster.util.SharedPreferencesUtils;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.jessyan.autosize.internal.CustomAdapt;
import utils.SerialPortUtils;


public class MapActivity extends AppCompatActivity implements CustomAdapt {

    @BindView(R.id.bt)
    Button bt;
    @BindView(R.id.button)
    Button button;
    @BindView(R.id.buttons)
    Button buttons;
    @BindView(R.id.tv_wd)
    TextView tvWd;
    @BindView(R.id.tv_jd)
    TextView tvJd;
    private DataViewModel dataViewModel;
    private SerialPortUtils serialPortUtils;

    public class SDKReceiver extends BroadcastReceiver {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }

            if (action.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
                // ??????????????????????????????
                Log.e("map", "key ????????????! ????????? :"
                        + intent.getIntExtra(SDKInitializer.SDK_BROADTCAST_INTENT_EXTRA_INFO_KEY_ERROR_CODE, 0)
                        + " ; ???????????? ???"
                        + intent.getStringExtra(SDKInitializer.SDK_BROADTCAST_INTENT_EXTRA_INFO_KEY_ERROR_MESSAGE));
            } else if (action.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK)) {
                Log.e("map", "key ????????????! ????????????????????????");
            } else if (action.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
                Log.e("map", "????????????");
            }
        }
    }

    private SDKReceiver mReceiver;

    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private MyLocationConfiguration.LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker, ma;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;
    private SensorManager mSensorManager;
    //private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;

    TextureMapView mMapView;
    BaiduMap mBaiduMap;
    RadioGroup.OnCheckedChangeListener radioButtonListener;
    Button requestLocButton;
    boolean isFirstLoc = true; // ??????????????????
    private MyLocationData locData;
    private float direction;
    private LatLng latLng = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //?????????SDK????????????????????????context???????????????ApplicationContext
        //?????????????????????setContentView??????????????????
        SDKInitializer.initialize(getApplicationContext());
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        serialPortUtils = SerialPortUtils.getInstance(this);
        dataViewModel = new ViewModelProvider(serialPortUtils.mActivity).get(DataViewModel.class);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_OK);
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver, iFilter);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//???????????????????????????
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
        // ???????????????
        mMapView = (TextureMapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // ??????????????????
        mBaiduMap.setMyLocationEnabled(true);
        View child = mMapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)) {
            child.setVisibility(View.INVISIBLE);
        }

        // ???????????????
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // ??????gps
        option.setCoorType("bd09ll"); // ??????????????????
        option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();

        ImageView v = new ImageView(this);
        v.setImageDrawable(getDrawable(R.drawable.icon_gcoding));
        ma = BitmapDescriptorFactory.fromViewWithDpi(v, 360);


        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mBaiduMap.clear();

                MarkerOptions option = new MarkerOptions().icon(ma).position(latLng);

                //????????????

                option.animateType(MarkerOptions.MarkerAnimateType.none);

                //??????????????????Marker????????????

                mBaiduMap.addOverlay(option);

                //??????Marker????????????ZIndex

                option.zIndex(0);
            }

            @Override
            public void onMapPoiClick(MapPoi mapPoi) {

            }
        });
//        if (serialPortUtils.latLng != null) {
        tvJd.setText(String.format("??????:%.1f", serialPortUtils.lng));
        tvWd.setText(String.format("??????:%.1f", serialPortUtils.lat));
//        }

    }


    /**
     * ??????SDK????????????
     */
    public class MyLocationListenner extends BDAbstractLocationListener {

        @SuppressLint("DefaultLocale")
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view ???????????????????????????????????????
            if (location == null || mMapView == null) {
                return;
            }
            latLng = new LatLng(location.getLatitude(), location.getLongitude());

            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            if ((mCurrentLat >= 0.1) || (mCurrentLon >= 0.1)) {
                tvJd.setText(String.format("??????:%.1f", mCurrentLon));
                tvWd.setText(String.format("??????:%.1f", mCurrentLat));
                serialPortUtils.lat = mCurrentLat;
                serialPortUtils.lng = mCurrentLon;
                serialPortUtils.newLatlng = true;
                SharedPreferencesUtils.setParam(MapActivity.this, "lat", String.format("%.6f", mCurrentLat));
                SharedPreferencesUtils.setParam(MapActivity.this, "lng", String.format("%.6f", mCurrentLon));
            }
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // ?????????????????????????????????????????????????????????0-360
                    .direction(mCurrentDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // ?????????????????????
        mLocClient.stop();
        // ??????????????????
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean isBaseOnWidth() {
        return true;
    }

    @Override
    public float getSizeInDp() {
        return 500;
    }

    @OnClick({R.id.bt, R.id.button, R.id.buttons})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.bt:
                if (latLng == null)
                    break;
                //??????????????????????????????
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
                break;
            case R.id.button:
                //????????????
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.buttons:
                //????????????
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                break;
        }
    }


}
