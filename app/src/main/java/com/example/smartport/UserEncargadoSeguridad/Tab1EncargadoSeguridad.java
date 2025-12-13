package com.example.smartport.UserEncargadoSeguridad;

import android.app.AlertDialog;
import android.graphics.Color;
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

import com.example.smartport.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Tab1EncargadoSeguridad extends Fragment {

    // ===== Firestore：完全对齐你截图字段 =====
    private static final String COLLECTION_CARGOS = "cargos";
    private static final String FIELD_CREATED_AT = "fechaCreacion"; // 你项目里就是 fechaCreacion
    private static final String FIELD_EDITED_WEIGHT = "pesoTotal";  // Controlador编辑重量就是 pesoTotal

    // ===== 规则 =====
    private static final double DIFF_THRESHOLD_KG = 500.0;

    // ===== MQTT：按 Tab1Capitan 的写法统一 =====
    private final String BROKER = "tcp://broker.emqx.io:1883";
    private final String CLIENT_ID = "android_security_" + UUID.randomUUID();

    // 你可以按你的传感器实际改 topic（如果你还没定，就先用这两个）
    private final String TOPIC_WEIGHT_REQUEST  = "smartport/weight/request";
    private final String TOPIC_WEIGHT_RESPONSE = "smartport/weight/response";

    private TextView tvCargoId, tvEditedWeight, tvStatus;
    private Button btnGetRealWeight;

    private FirebaseFirestore db;
    private ListenerRegistration cargoListener;

    private MqttAndroidClient mqttClient;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRequestInProgress = false;

    // 当前最新 cargo
    private String currentCargoId = null;
    private double currentEditedKg = -1;

    // 防串单：本次请求的 cargoId
    private String pendingCargoId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.tab1_encargadoseguridad, container, false);

        tvCargoId = view.findViewById(R.id.tvCargoId);
        tvEditedWeight = view.findViewById(R.id.tvEditedWeight);
        tvStatus = view.findViewById(R.id.tvStatus);
        btnGetRealWeight = view.findViewById(R.id.btnGetRealWeight);

        db = FirebaseFirestore.getInstance();

        connectMqtt();
        listenLatestCargo();

        btnGetRealWeight.setOnClickListener(v -> {
            if (isRequestInProgress) return;

            if (currentCargoId == null) {
                tvStatus.setText("Estado: no hay cargo nuevo.");
                return;
            }

            sendWeightRequest();
        });

        return view;
    }

    // ======================= MQTT（与 Capitan 同风格） =======================

    private void connectMqtt() {
        mqttClient = new MqttAndroidClient(requireContext(), BROKER, CLIENT_ID);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        try {
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(getContext(), "MQTT 已连接", Toast.LENGTH_SHORT).show();
                    subscribeWeightResponse();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getContext(), "MQTT 连接失败", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "MQTT 连接异常", Toast.LENGTH_LONG).show();
        }
    }

    private void subscribeWeightResponse() {
        if (mqttClient == null || !mqttClient.isConnected()) return;

        try {
            mqttClient.subscribe(TOPIC_WEIGHT_RESPONSE, 1, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    onWeightResponse(payload);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "订阅重量返回失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendWeightRequest() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            Toast.makeText(getContext(), "MQTT 未连接，正在重连...", Toast.LENGTH_SHORT).show();
            connectMqtt();
            return;
        }

        // UI：防重复点击
        isRequestInProgress = true;
        btnGetRealWeight.setEnabled(false);
        btnGetRealWeight.setText("SOLICITANDO...");
        tvStatus.setText("Estado: solicitando peso real...");

        pendingCargoId = currentCargoId;

        // 推荐请求 payload 为 JSON（含 cargoId）
        String payload = "{\"cargoId\":\"" + pendingCargoId + "\"}";
        MqttMessage msg = new MqttMessage(payload.getBytes());
        msg.setQos(1);

        try {
            mqttClient.publish(TOPIC_WEIGHT_REQUEST, msg);

            // 超时恢复（避免无回包卡死）
            handler.postDelayed(() -> {
                if (isRequestInProgress) {
                    tvStatus.setText("Estado: sin respuesta del sensor (timeout).");
                    resetRequestButton();
                }
            }, 6000);

        } catch (MqttException e) {
            e.printStackTrace();
            tvStatus.setText("Estado: envío fallido.");
            resetRequestButton();
        }
    }

    private void resetRequestButton() {
        isRequestInProgress = false;
        btnGetRealWeight.setEnabled(true);
        btnGetRealWeight.setText("Obtener peso real (MQTT)");
    }

    // ======================= Firestore：监听最新 cargo（按 fechaCreacion） =======================

    private void listenLatestCargo() {
        tvStatus.setText("Estado: escuchando cargos nuevos...");

        cargoListener = db.collection(COLLECTION_CARGOS)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        tvStatus.setText("Estado: error Firebase: " + e.getMessage());
                        return;
                    }

                    if (snap == null || snap.isEmpty()) {
                        currentCargoId = null;
                        currentEditedKg = -1;
                        tvCargoId.setText("Cargo ID: -");
                        tvEditedWeight.setText("Peso (editado): - kg");
                        tvStatus.setText("Estado: no hay cargos.");
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    currentCargoId = doc.getId();

                    Double edited = doc.getDouble(FIELD_EDITED_WEIGHT); // pesoTotal
                    currentEditedKg = edited != null ? edited : -1;

                    tvCargoId.setText("Cargo ID: " + currentCargoId);
                    tvEditedWeight.setText("Peso (editado): " + formatKg(currentEditedKg));
                    tvStatus.setText("Estado: cargo nuevo recibido.");
                });
    }

    // ======================= 收到真实重量：弹窗+红绿+添加/不添加 =======================

    private void onWeightResponse(String payload) {
        ParsedWeight parsed = parseWeightPayload(payload);

        // 防串单：如果回包含 cargoId，则必须匹配 pendingCargoId
        if (parsed.cargoId != null && pendingCargoId != null && !pendingCargoId.equals(parsed.cargoId)) {
            return;
        }

        if (parsed.realKg < 0 || pendingCargoId == null) return;

        double diff = Math.abs(parsed.realKg - currentEditedKg);
        boolean bigDiff = diff > DIFF_THRESHOLD_KG;

        requireActivity().runOnUiThread(() -> {
            tvStatus.setText("Estado: peso real recibido.");
            resetRequestButton();

            showDecisionDialog(pendingCargoId, currentEditedKg, parsed.realKg, diff, bigDiff);
            pendingCargoId = null;
        });
    }

    private void showDecisionDialog(String cargoId, double editedKg, double realKg, double diffKg, boolean bigDiff) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_real_weight, null, false);

        TextView tvId = dialogView.findViewById(R.id.tvDialogCargoId);
        TextView tvEdited = dialogView.findViewById(R.id.tvDialogEdited);
        TextView tvReal = dialogView.findViewById(R.id.tvDialogReal);
        TextView tvDiff = dialogView.findViewById(R.id.tvDialogDiff);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);
        Button btnNotAdd = dialogView.findViewById(R.id.btnNotAdd);

        tvId.setText("Cargo ID: " + cargoId);
        tvEdited.setText("Peso (editado): " + formatKg(editedKg));
        tvReal.setText("Peso (real): " + formatKg(realKg));
        tvDiff.setText("Diferencia: " + formatKg(diffKg));

        int color = bigDiff ? Color.RED : Color.parseColor("#2E7D32");
        tvReal.setTextColor(color);
        tvDiff.setTextColor(color);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnAdd.setOnClickListener(v -> {
            dialog.dismiss();
            saveDecision(cargoId, realKg, diffKg, true);
        });

        btnNotAdd.setOnClickListener(v -> {
            dialog.dismiss();
            saveDecision(cargoId, realKg, diffKg, false);
        });

        dialog.show();
    }

    private void saveDecision(String cargoId, double realKg, double diffKg, boolean add) {
        tvStatus.setText("Estado: guardando decisión...");

        Map<String, Object> update = new HashMap<>();
        // 新增字段（不会影响你原有字段）
        update.put("pesoReal", realKg);
        update.put("diferenciaPeso", diffKg);
        update.put("securityChecked", true);
        update.put("visibleToTransportista", add);

        db.collection(COLLECTION_CARGOS)
                .document(cargoId)
                .update(update)
                .addOnSuccessListener(unused ->
                        tvStatus.setText("Estado: guardado (" + (add ? "Añadido" : "No añadido") + ")."))
                .addOnFailureListener(err ->
                        tvStatus.setText("Estado: error al guardar: " + err.getMessage()));
    }

    // ======================= 工具方法 =======================

    private String formatKg(double kg) {
        if (kg < 0) return "- kg";
        return String.format("%.1f kg", kg);
    }

    /** 兼容两种回包：1) 纯数字  2) JSON：{"cargoId":"cargo_001","pesoReal":1234.5} */
    private ParsedWeight parseWeightPayload(String payload) {
        ParsedWeight pw = new ParsedWeight();
        String s = payload == null ? "" : payload.trim();

        // 1) 纯数字： "1234.5"
        try {
            pw.realKg = Double.parseDouble(s);
            return pw;
        } catch (Exception ignored) {}

        // 2) JSON（推荐你的传感器发这种）
        try {
            JSONObject o = new JSONObject(s);
            pw.cargoId = o.optString("cargoId", null);

            // 你回包字段可以叫 pesoReal / realWeight / peso
            if (o.has("pesoReal")) pw.realKg = o.optDouble("pesoReal", -1);
            else if (o.has("realWeight")) pw.realKg = o.optDouble("realWeight", -1);
            else if (o.has("peso")) pw.realKg = o.optDouble("peso", -1);
            else pw.realKg = -1;

            return pw;
        } catch (Exception e) {
            pw.realKg = -1;
            return pw;
        }
    }

    private static class ParsedWeight {
        String cargoId = null;
        double realKg = -1;
    }

    // ======================= 生命周期清理（同 Capitan） =======================

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (cargoListener != null) cargoListener.remove();
        handler.removeCallbacksAndMessages(null);

        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
            } catch (MqttException ignored) {}
        }
    }
}
