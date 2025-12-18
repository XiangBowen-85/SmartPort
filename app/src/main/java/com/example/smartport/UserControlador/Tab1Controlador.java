package com.example.smartport.UserControlador;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartport.R;
import com.example.smartport.UserTransportista.CargoAdapter;
import com.example.smartport.UserTransportista.Cargo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Tab1Controlador extends Fragment {

    private static final String TAG = "Tab1Controlador";

    // Views
    private RecyclerView recyclerViewCargos;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextInputEditText etDate;
    private Button btnClearFilter;
    private FloatingActionButton fabAddCargo;

    // Adapter and Data
    private CargoAdapter cargoAdapter;
    private List<Cargo> cargoList;
    private List<Cargo> allCargosList; // Store all cargos for filtering

    // Firebase
    private FirebaseFirestore db;
    private ListenerRegistration cargoListener;

    // Date filter
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab1_controlador, container, false);

        initViews(view);
        setupDatePicker();
        setupRecyclerView();
        setupFloatingActionButton();
        loadCargosFromFirestore();

        return view;
    }

    private void initViews(View view) {
        recyclerViewCargos = view.findViewById(R.id.recyclerViewCargos);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        etDate = view.findViewById(R.id.et_date);
        btnClearFilter = view.findViewById(R.id.btn_clear_filter);
        fabAddCargo = view.findViewById(R.id.fabAddCargo);

        db = FirebaseFirestore.getInstance();
        cargoList = new ArrayList<>();
        allCargosList = new ArrayList<>();

        // Initialize date formatter
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        selectedDate = Calendar.getInstance();
    }

    private void setupDatePicker() {
        // Set current date as default
        etDate.setText(dateFormat.format(selectedDate.getTime()));

        // Date picker dialog
        etDate.setOnClickListener(v -> showDatePickerDialog());

        // Clear filter button
        btnClearFilter.setOnClickListener(v -> clearDateFilter());
    }

    private void setupFloatingActionButton() {
        fabAddCargo.setOnClickListener(v -> showAddCargoDialog());
    }

    private void showAddCargoDialog() {
        final Dialog addCargoDialog = new Dialog(getContext());
        addCargoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        addCargoDialog.setContentView(R.layout.dialog_add_cargo);
        addCargoDialog.setCancelable(true);

        // 设置弹窗宽度
        Window window = addCargoDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // 初始化视图
        TextInputLayout tilImagenUrl = addCargoDialog.findViewById(R.id.tilImagenUrl);
        TextInputLayout tilTipo = addCargoDialog.findViewById(R.id.tilTipo);
        TextInputLayout tilPesoTotal = addCargoDialog.findViewById(R.id.tilPesoTotal);
        TextInputLayout tilDescripcion = addCargoDialog.findViewById(R.id.tilDescripcion);


        TextInputEditText etImagenUrl = addCargoDialog.findViewById(R.id.etImagenUrl);
        TextInputEditText etTipo = addCargoDialog.findViewById(R.id.etTipo);
        TextInputEditText etPesoTotal = addCargoDialog.findViewById(R.id.etPesoTotal);
        TextInputEditText etDescripcion = addCargoDialog.findViewById(R.id.etDescripcion);

        TextView tvError = addCargoDialog.findViewById(R.id.tvError);
        Button btnCancel = addCargoDialog.findViewById(R.id.btnCancel);
        Button btnSave = addCargoDialog.findViewById(R.id.btnSave);

        // 取消按钮
        btnCancel.setOnClickListener(v -> addCargoDialog.dismiss());

        // 保存按钮
        btnSave.setOnClickListener(v -> {
            // 获取输入数据
            String imagenUrl = etImagenUrl.getText().toString().trim();
            String tipo = etTipo.getText().toString().trim();
            String pesoTotalStr = etPesoTotal.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            // 验证必填字段
            if (tipo.isEmpty()) {
                tilTipo.setError("El tipo es obligatorio");
                return;
            } else {
                tilTipo.setError(null);
            }

            // 验证重量是否为数字
            double pesoTotal = 0.0;
            if (!pesoTotalStr.isEmpty()) {
                try {
                    pesoTotal = Double.parseDouble(pesoTotalStr);
                } catch (NumberFormatException e) {
                    tilPesoTotal.setError("El peso debe ser un número válido");
                    return;
                }
            }
            tilPesoTotal.setError(null);

            // 创建新的 cargo 对象
            Cargo newCargo = new Cargo();
            newCargo.setImagenUrl(imagenUrl.isEmpty() ? "" : imagenUrl);
            newCargo.setTipo(tipo);
            newCargo.setPesoTotal(pesoTotal);
            newCargo.setEnviado(false); // 默认 pendiente
            newCargo.setDescripcion(descripcion.isEmpty() ? "" : descripcion);
            newCargo.setFechaCreacion(Timestamp.now()); // 当前时间
            newCargo.setSecurityChecked(false);
            newCargo.setVisibleToTransportista(false);
            newCargo.setSecurityStatus("PENDING");


            // 保存到 Firestore
            // 关键：生成自增 ID 并保存
            generateNextCargoIdAndSave(newCargo, addCargoDialog);
        });

        addCargoDialog.show();
    }

    private void generateNextCargoIdAndSave(Cargo cargo, Dialog dialog) {
        Button btnSave = dialog.findViewById(R.id.btnSave);
        TextView tvError = dialog.findViewById(R.id.tvError);

        btnSave.setEnabled(false);
        btnSave.setText("Generando ID...");

        // 第一步：找出当前最大的编号
        db.collection("cargos")
                .orderBy("id", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int nextNumber = 1;

                    if (!querySnapshot.isEmpty()) {
                        String lastId = querySnapshot.getDocuments().get(0).getId();
                        if (lastId != null && lastId.startsWith("cargo_")) {
                            try {
                                String numStr = lastId.substring("cargo_".length());
                                int lastNum = Integer.parseInt(numStr);
                                nextNumber = lastNum + 1;
                            } catch (Exception ignored) {}
                        }
                    }

                    // 第二步：用事务尝试写入，从 nextNumber 开始，一直试到成功为止
                    tryCreateCargoWithNumber(cargo, nextNumber, dialog);

                })
                .addOnFailureListener(e -> {
                    tvError.setText("Error de red");
                    tvError.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(true);
                    btnSave.setText("Guardar");
                });
    }

    // 新增这个递归方法，自动解决冲突
    private void tryCreateCargoWithNumber(Cargo cargo, int number, Dialog dialog) {
        String candidateId = String.format("cargo_%03d", number);

        db.runTransaction(transaction -> {
            DocumentReference ref = db.collection("cargos").document(candidateId);
            DocumentSnapshot snapshot = transaction.get(ref);

            if (snapshot.exists()) {
                // 这个 ID 已经被别人抢走了，直接返回 null 表示失败
                return null;
            }

            // 成功！写入数据
            cargo.setId(candidateId);
            transaction.set(ref, cargo);
            return candidateId;

        }).addOnSuccessListener(result -> {
            if (result != null) {
                // 成功创建
                Log.d(TAG, "Cargamento creado → " + result);
                dialog.dismiss();
                Toast.makeText(getContext(), "Creado: " + result, Toast.LENGTH_SHORT).show();
            } else {
                // 被抢了，自动 +1 再试一次（递归）
                tryCreateCargoWithNumber(cargo, number + 1, dialog);
            }
        }).addOnFailureListener(e -> {
            // 网络错误等真正失败
            TextView tvError = dialog.findViewById(R.id.tvError);
            tvError.setText("Error de red");
            tvError.setVisibility(View.VISIBLE);
            Button btnSave = dialog.findViewById(R.id.btnSave);
            btnSave.setEnabled(true);
            btnSave.setText("Guardar");
        });
    }

    private void saveCargoToFirestore(Cargo cargo, Dialog dialog) {
        // 显示加载状态
        Button btnSave = dialog.findViewById(R.id.btnSave);
        btnSave.setEnabled(false);
        btnSave.setText("Guardando...");

        TextView tvError = dialog.findViewById(R.id.tvError);

        // 检查文档是否已存在
        db.collection("cargos").document(cargo.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // 文档已存在
                        tvError.setText("El ID del cargamento ya existe");
                        tvError.setVisibility(View.VISIBLE);
                        btnSave.setEnabled(true);
                        btnSave.setText("Guardar");
                    } else {
                        // 文档不存在，可以保存
                        db.collection("cargos").document(cargo.getId())
                                .set(cargo)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Cargo agregado exitosamente: " + cargo.getId());
                                    dialog.dismiss();
                                    // 显示成功消息
                                    if (getContext() != null) {
                                        android.widget.Toast.makeText(getContext(),
                                                "Cargamento agregado exitosamente",
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error agregando cargo: " + e.getMessage());
                                    tvError.setText("Error al guardar: " + e.getMessage());
                                    tvError.setVisibility(View.VISIBLE);
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Guardar");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error verificando ID: " + e.getMessage());
                    tvError.setText("Error al verificar ID: " + e.getMessage());
                    tvError.setVisibility(View.VISIBLE);
                    btnSave.setEnabled(true);
                    btnSave.setText("Guardar");
                });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    etDate.setText(dateFormat.format(selectedDate.getTime()));
                    applyDateFilter();
                    btnClearFilter.setVisibility(View.VISIBLE);
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void clearDateFilter() {
        selectedDate = Calendar.getInstance(); // Reset to current date
        etDate.setText(dateFormat.format(selectedDate.getTime()));
        btnClearFilter.setVisibility(View.GONE);

        // Show all cargos
        cargoList.clear();
        cargoList.addAll(allCargosList);
        cargoAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void applyDateFilter() {
        if (allCargosList.isEmpty()) return;

        cargoList.clear();

        // Convert selected date to start and end of day
        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        Date startDate = startOfDay.getTime();
        Date endDate = endOfDay.getTime();

        Log.d(TAG, "Filtering from: " + startDate + " to: " + endDate);

        // Filter cargos by date
        for (Cargo cargo : allCargosList) {
            if (cargo.getFechaCreacion() != null) {
                Date cargoDate = cargo.getFechaCreacion().toDate();

                // Check if cargo date is within the selected day
                if (!cargoDate.before(startDate) && !cargoDate.after(endDate)) {
                    cargoList.add(cargo);
                }
            }
        }

        cargoAdapter.notifyDataSetChanged();
        updateEmptyState();

        Log.d(TAG, "Filtered " + cargoList.size() + " cargos for date: " + dateFormat.format(selectedDate.getTime()));
    }

    private void setupRecyclerView() {
        cargoAdapter = new CargoAdapter(cargoList, getContext());
        recyclerViewCargos.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCargos.setAdapter(cargoAdapter);
    }

    private void loadCargosFromFirestore() {
        showLoading(true);

        // 实时监听cargos集合的变化，按创建时间降序排列
        cargoListener = db.collection("cargos")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    // 1) 先记录错误，但不要立刻 return
                    if (error != null) {
                        Log.e(TAG, "Error loading cargos: " + error.getMessage());
                    }

                    // 2) 如果 value 为空（真的拿不到任何快照），才显示 Error
                    if (value == null) {
                        showLoading(false);
                        tvEmpty.setText("Error al cargar cargamentos");
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerViewCargos.setVisibility(View.GONE);
                        return;
                    }

                    // 3) 有快照就正常渲染（哪怕 error != null）
                    allCargosList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            Cargo cargo = doc.toObject(Cargo.class);
                            cargo.setId(doc.getId());
                            allCargosList.add(cargo);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing cargo document: " + doc.getId(), e);
                        }
                    }

                    // 4) 刷新列表
                    if (btnClearFilter.getVisibility() == View.VISIBLE) {
                        applyDateFilter();
                    } else {
                        cargoList.clear();
                        cargoList.addAll(allCargosList);
                        cargoAdapter.notifyDataSetChanged();
                    }

                    // 5) 最后再 showLoading(false)，它会调用 updateEmptyState
                    showLoading(false);

                    // 可选：如果有 error，但你仍然显示了数据，可以用 Toast 提示而不盖住 UI
                    // if (error != null) Toast.makeText(getContext(), "Aviso: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (cargoList.isEmpty()) {
            if (btnClearFilter.getVisibility() == View.VISIBLE) {
                tvEmpty.setText("No hay cargamentos para la fecha seleccionada");
            } else {
                tvEmpty.setText("No hay cargamentos disponibles");
            }
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerViewCargos.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        } else {
            recyclerViewCargos.setVisibility(View.VISIBLE);
            updateEmptyState();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 移除监听器避免内存泄漏
        if (cargoListener != null) {
            cargoListener.remove();
        }
    }
}