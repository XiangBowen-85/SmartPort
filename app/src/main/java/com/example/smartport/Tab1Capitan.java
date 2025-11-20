package com.example.smartport;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.UUID;

import com.example.smartport.R;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Tab1Capitan extends Fragment {

    private Button btnOpenGate;
    private TextView tvStatus;
    private MqttAndroidClient mqttClient;
    private final String CLIENT_ID = "android_captain_" + UUID.randomUUID().toString();
    private final String TOPIC = "smartport/gate/main_gate";
    private final String BROKER = "tcp://broker.emqx.io:1883";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRequestInProgress = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab1_capitan, container, false);

        btnOpenGate = view.findViewById(R.id.btnOpenGate);
        tvStatus = view.findViewById(R.id.tvStatus);

        connectMqtt();

        btnOpenGate.setOnClickListener(v -> {
            if (isRequestInProgress) return;
            sendOpenCommand();
        });

        return view;
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

        String payload = "OPEN_NOW";   // Arduino 收到这串字符就开门
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