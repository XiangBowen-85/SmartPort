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
import com.example.smartport.R;

public class Tab1Capitan extends Fragment {

    private Button btnOpenGate;
    private TextView tvStatus;
    private FirebaseFirestore db;
    private DocumentReference gateDocRef;
    private ListenerRegistration gateListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRequestInProgress = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab1_capitan, container, false);

        btnOpenGate = view.findViewById(R.id.btnOpenGate);
        tvStatus = view.findViewById(R.id.tvStatus);

        db = FirebaseFirestore.getInstance();
        // 假设你的 Firestore 路径是： gates/main_gate/control
        gateDocRef = db.collection("gates").document("main_gate");

        btnOpenGate.setOnClickListener(v -> triggerGateOpen());

        // 实时监听闸门状态（可选：当 Arduino 确认开门后恢复按钮）
        startGateStatusListener();

        return view;
    }

    private void triggerGateOpen() {
        if (isRequestInProgress) return;

        isRequestInProgress = true;
        btnOpenGate.setEnabled(false);
        btnOpenGate.setText("ENVIADO");
        tvStatus.setText("Solicitud enviada... Esperando apertura");
        tvStatus.setVisibility(View.VISIBLE);

        HashMap<String, Object> data = new HashMap<>();
        data.put("open_request", true);
        data.put("requested_by", "captain_app"); // 可选：标识是谁发的请求
        data.put("timestamp", System.currentTimeMillis());

        gateDocRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "¡Solicitud enviada!", Toast.LENGTH_SHORT).show();

                    // 3秒后自动恢复（防止卡死，即使 Arduino 没响应）
                    handler.postDelayed(() -> resetButton(), 4000);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        if (getActivity() == null) return;

        isRequestInProgress = false;
        btnOpenGate.setEnabled(true);
        btnOpenGate.setText("ABRIR\nCOMPUERTA");
        tvStatus.setText("");
        tvStatus.setVisibility(View.GONE);
    }

    private void startGateStatusListener() {
        gateListener = gateDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) {
                return;
            }

            Boolean openRequest = snapshot.getBoolean("open_request");
            // 如果 Arduino 已处理请求并写回 false，恢复按钮
            if (openRequest == null || !openRequest) {
                if (isRequestInProgress) {
                    handler.post(() -> {
                        tvStatus.setText("¡Compuerta abierta!");
                        tvStatus.setTextColor(getResources().getColor(R.color.green, null));
                        handler.postDelayed(this::resetButton, 2000);
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gateListener != null) {
            gateListener.remove();
        }
        handler.removeCallbacksAndMessages(null);
    }
}