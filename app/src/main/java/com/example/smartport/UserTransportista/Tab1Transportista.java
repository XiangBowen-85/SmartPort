package com.example.smartport.UserTransportista;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartport.R;
import com.google.android.material.textfield.TextInputEditText;
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

public class Tab1Transportista extends Fragment {

    private static final String TAG = "Tab1Fragment";

    // Views
    private RecyclerView recyclerViewCargos;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextInputEditText etDate;
    private Button btnClearFilter;

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
        View view = inflater.inflate(R.layout.tab1_transportista, container, false);

        initViews(view);
        setupDatePicker();
        setupRecyclerView();
        loadCargosFromFirestore();

        return view;
    }

    private void initViews(View view) {
        recyclerViewCargos = view.findViewById(R.id.recyclerViewCargos);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        etDate = view.findViewById(R.id.et_date);
        btnClearFilter = view.findViewById(R.id.btn_clear_filter);

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

        // Optional: Set date range if needed
        // datePickerDialog.getDatePicker().setMinDate(minDate);
        // datePickerDialog.getDatePicker().setMaxDate(maxDate);

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
                    showLoading(false);

                    if (error != null) {
                        Log.e(TAG, "Error loading cargos: " + error.getMessage());
                        tvEmpty.setText("Error al cargar cargamentos");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        allCargosList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Cargo cargo = doc.toObject(Cargo.class);
                                cargo.setId(doc.getId()); // 设置文档ID
                                allCargosList.add(cargo);

                                Log.d(TAG, "Loaded cargo: " + cargo.getTipo() +
                                        " Date: " + (cargo.getFechaCreacion() != null ?
                                        cargo.getFechaCreacion().toDate() : "null"));
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing cargo document: " + doc.getId(), e);
                            }
                        }

                        // Apply current filter or show all
                        if (btnClearFilter.getVisibility() == View.VISIBLE) {
                            applyDateFilter(); // Re-apply filter with new data
                        } else {
                            cargoList.clear();
                            cargoList.addAll(allCargosList);
                            cargoAdapter.notifyDataSetChanged();
                            updateEmptyState();
                        }

                        Log.d(TAG, "Total cargos loaded: " + allCargosList.size());

                    } else {
                        allCargosList.clear();
                        cargoList.clear();
                        cargoAdapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(View.VISIBLE);
                        Log.d(TAG, "No cargos found in Firestore");
                    }
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