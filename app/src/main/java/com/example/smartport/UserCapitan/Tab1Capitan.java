package com.example.smartport.UserCapitan;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.util.UUID;

import com.example.smartport.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Tab1Capitan extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private Button btnOpenGate;
    private TextView tvStatus;
    private MqttAndroidClient mqttClient;
    private final String CLIENT_ID = "android_captain_" + UUID.randomUUID().toString();
    private final String TOPIC = "smartport/gate/main_gate";
    private final String BROKER = "tcp://broker.emqx.io:1883";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRequestInProgress = false;
    // =========================
    //  SENSORES: Shake -> AlertDialog (独立功能)
    // =========================
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener accelListener;

    // 建议参数：一般 2.0 ~ 2.7 之间比较好触发
    private static final float SHAKE_G_FORCE_THRESHOLD = 2.3f;
    private static final long SHAKE_DEBOUNCE_MS = 1500L;

    // “连续多次超过阈值”更稳：在窗口内达到次数才算shake
    private static final long SHAKE_WINDOW_MS = 600L;
    private static final int SHAKE_MIN_COUNT = 2;

    private long lastShakeUptimeMs = 0L;
    private long shakeWindowStartUptimeMs = 0L;
    private int shakeCountInWindow = 0;
    private AlertDialog dangerDialog;
    private GoogleMap mapa;
    private final LatLng UPV = new LatLng(39.481106, -0.340987); // 你旧代码里的点 :contentReference[oaicite:8]{index=8}
    private static final int REQ_LOC = 2001;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tab1_capitan, container, false);

        btnOpenGate = view.findViewById(R.id.btnOpenGate);
        tvStatus = view.findViewById(R.id.tvStatus);

        connectMqtt();

        // 开门按钮逻辑保持不变（与摇一摇无关）
        btnOpenGate.setOnClickListener(v -> {
            if (isRequestInProgress) return;
            sendOpenCommand();
        });

        // 初始化加速度计
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        accelListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event == null || event.values == null || event.values.length < 3) return;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float gX = x / SensorManager.GRAVITY_EARTH;
                float gY = y / SensorManager.GRAVITY_EARTH;
                float gZ = z / SensorManager.GRAVITY_EARTH;

                float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

                if (gForce > SHAKE_G_FORCE_THRESHOLD) {
                    long now = SystemClock.uptimeMillis();

                    // 全局防抖：短时间内不重复触发弹窗
                    if (now - lastShakeUptimeMs < SHAKE_DEBOUNCE_MS) return;

                    // 时间窗口统计：窗口内达到一定次数才算 shake
                    if (shakeWindowStartUptimeMs == 0L || now - shakeWindowStartUptimeMs > SHAKE_WINDOW_MS) {
                        shakeWindowStartUptimeMs = now;
                        shakeCountInWindow = 1;
                    } else {
                        shakeCountInWindow++;
                    }

                    if (shakeCountInWindow >= SHAKE_MIN_COUNT) {
                        lastShakeUptimeMs = now;
                        shakeWindowStartUptimeMs = 0L;
                        shakeCountInWindow = 0;

                        // ✅ 摇一摇只做“危险警告”，不碰开门逻辑
                        showDangerWarningDialog();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }
        };

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;

        mapa.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mapa.getUiSettings().setZoomControlsEnabled(false);
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(UPV, 15));

        mapa.addMarker(new MarkerOptions()
                .position(UPV)
                .title("UPV")
                .snippet("Universidad Politécnica de Valencia")
                .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_compass))
                .anchor(0.5f, 0.5f));

        mapa.setOnMapClickListener(this);

        // 地理定位（蓝点）：需要运行时权限
        enableMyLocationIfPermitted();
    }

    private void enableMyLocationIfPermitted() {
        if (mapa == null) return;

        boolean fine = ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOC);
            return;
        }

        mapa.setMyLocationEnabled(true);
        mapa.getUiSettings().setCompassEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOC) {
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocationIfPermitted();
                    return;
                }
            }
            Toast.makeText(getContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapClick(LatLng puntoPulsado) {
        if (mapa == null) return;
        mapa.addMarker(new MarkerOptions().position(puntoPulsado)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
    }


    @Override
    public void onResume() {
        super.onResume();

        // 重要：确认传感器存在 & 注册监听
        if (sensorManager == null) {
            Toast.makeText(getContext(), "SensorManager不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        if (accelerometer == null) {
            Toast.makeText(getContext(), "该设备没有加速度传感器", Toast.LENGTH_SHORT).show();
            return;
        }

        sensorManager.registerListener(accelListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        // 用于确认：页面可见时确实完成了注册（你现在没反应时，用它判断关键）
        Toast.makeText(getContext(), "Sensores activos (acelerómetro)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null && accelListener != null) {
            sensorManager.unregisterListener(accelListener);
        }
    }

    private void showDangerWarningDialog() {
        if (!isAdded() || getContext() == null) return;

        // 防止连弹多个
        if (dangerDialog != null && dangerDialog.isShowing()) return;

        dangerDialog = new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Advertencia")
                .setMessage("El entorno actual es peligroso.")
                .setCancelable(true)
                .setPositiveButton("ENTENDIDO", (dialog, which) -> dialog.dismiss())
                .create();

        dangerDialog.show();
    }


    private void connectMqtt() {
        mqttClient = new MqttAndroidClient(getContext(), BROKER, CLIENT_ID);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        try {
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(getContext(), "MQTT 已连接", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getContext(), "MQTT 连接失败", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void sendOpenCommand() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            Toast.makeText(getContext(), "MQTT 未连接，正在重连...", Toast.LENGTH_SHORT).show();
            connectMqtt();
            return;
        }

        String payload = "OPEN";   // Arduino 收到这串字符就开门
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);

        try {
            // 主题必须和 Arduino 订阅的一模一样！
            mqttClient.publish("smartport/gate/main_gate", message);

            // 下面这些 UI 反馈不动
            isRequestInProgress = true;
            btnOpenGate.setEnabled(false);
            btnOpenGate.setText("ENVIADO");
            tvStatus.setText("Puerta abriéndose...");
            tvStatus.setVisibility(View.VISIBLE);
            handler.postDelayed(this::resetButton, 4000);

            Toast.makeText(getContext(), "¡Puerta abierta por App!", Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            Toast.makeText(getContext(), "Envío fallido", Toast.LENGTH_SHORT).show();
            resetButton();
        }
    }



    private void resetButton() {
        if (getActivity() == null) return;

        isRequestInProgress = false;
        btnOpenGate.setEnabled(true);
        btnOpenGate.setText("ABRIR\nCOMPUERTA");
        tvStatus.setVisibility(View.GONE);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
            } catch (MqttException ignored) {}
        }
        handler.removeCallbacksAndMessages(null);
    }
}