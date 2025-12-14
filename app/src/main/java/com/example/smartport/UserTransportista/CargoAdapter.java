package com.example.smartport.UserTransportista;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.smartport.R;
import com.example.smartport.UserTransportista.Cargo;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CargoAdapter extends RecyclerView.Adapter<CargoAdapter.CargoViewHolder> {

    private List<Cargo> cargoList;
    private Context context;
    private SimpleDateFormat dateFormat;

    public CargoAdapter(List<Cargo> cargoList, Context context) {
        this.cargoList = cargoList;
        this.context = context;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public CargoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cargo, parent, false);
        return new CargoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CargoViewHolder holder, int position) {
        Cargo cargo = cargoList.get(position);

        // 加载图片
        if (cargo.getImagenUrl() != null && !cargo.getImagenUrl().isEmpty()) {
            Glide.with(context)
                    .load(cargo.getImagenUrl())
                    .placeholder(R.drawable.placeholder_cargo)
                    .into(holder.imgCargo);
        } else {
            holder.imgCargo.setImageResource(R.drawable.placeholder_cargo);
        }

        // 设置数据
        holder.tvTipo.setText(cargo.getTipo());
        holder.tvPeso.setText(String.format("Peso: %.2f kg", cargo.getPesoTotal()));

        // 设置状态
        if (cargo.isEnviado()) {
            holder.tvEstado.setText("Enviado");
            holder.tvEstado.setBackgroundColor(context.getResources().getColor(R.color.green));
        } else {
            holder.tvEstado.setText("Pendiente");
            holder.tvEstado.setBackgroundColor(context.getResources().getColor(R.color.orange));
        }

        // ===== 安全审核状态 =====
        if ("PENDING".equalsIgnoreCase(cargo.getSecurityStatus())) {
            holder.tvSecurity.setVisibility(View.VISIBLE);
            holder.tvSecurity.setText("Sin revisar");
        } else {
            holder.tvSecurity.setVisibility(View.GONE);
        }


        // 设置日期
        if (cargo.getFechaCreacion() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String fecha = dateFormat.format(cargo.getFechaCreacion().toDate());
            holder.tvFecha.setText("Fecha: " + fecha);
        } else {
            holder.tvFecha.setText("Fecha: No disponible");
        }

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            showCargoDetailDialog(cargo);
        });
    }

    private void showCargoDetailDialog(Cargo cargo) {
        final Dialog detailDialog = new Dialog(context);
        detailDialog.setContentView(R.layout.dialog_cargo_detail);
        detailDialog.setCancelable(true);

        // 设置弹窗宽度为屏幕宽度的90%，高度自适应
        Window window = detailDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (getScreenWidth() * 0.90); // 屏幕宽度的90%
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            window.setAttributes(layoutParams);

            // 可选：设置弹窗背景为圆角
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 初始化视图
        ImageView ivDetailImage = detailDialog.findViewById(R.id.ivDetailImage);
        TextView tvDetailTipo = detailDialog.findViewById(R.id.tvDetailTipo);
        TextView tvDetailPeso = detailDialog.findViewById(R.id.tvDetailPeso);
        TextView tvDetailEstado = detailDialog.findViewById(R.id.tvDetailEstado);
        TextView tvDetailFecha = detailDialog.findViewById(R.id.tvDetailFecha);
        TextView tvDetailId = detailDialog.findViewById(R.id.tvDetailId);
        TextView tvDetailDescripcion = detailDialog.findViewById(R.id.tvDetailDescripcion);
        Button btnCloseDetail = detailDialog.findViewById(R.id.btnCloseDetail);

        // 设置货物数据
        // 图片
        if (cargo.getImagenUrl() != null && !cargo.getImagenUrl().isEmpty()) {
            Glide.with(context)
                    .load(cargo.getImagenUrl())
                    .placeholder(R.drawable.placeholder_cargo)
                    .error(R.drawable.placeholder_cargo)
                    .into(ivDetailImage);
        }

        // 基本信息
        tvDetailTipo.setText(cargo.getTipo());
        tvDetailPeso.setText(String.format("%.2f kg", cargo.getPesoTotal()));

        // 状态
        if (cargo.isEnviado()) {
            tvDetailEstado.setText("Enviado");
            tvDetailEstado.setTextColor(context.getResources().getColor(R.color.green));
        } else {
            tvDetailEstado.setText("Pendiente");
            tvDetailEstado.setTextColor(context.getResources().getColor(R.color.orange));
        }

        // 日期
        if (cargo.getFechaCreacion() != null) {
            String fecha = dateFormat.format(cargo.getFechaCreacion().toDate());
            tvDetailFecha.setText(fecha);
        } else {
            tvDetailFecha.setText("No disponible");
        }

        // ID
        if (cargo.getId() != null) {
            tvDetailId.setText(cargo.getId());
        }

        // 描述（放在最后）
        if (cargo.getDescripcion() != null && !cargo.getDescripcion().isEmpty()) {
            tvDetailDescripcion.setText(cargo.getDescripcion());
        } else {
            tvDetailDescripcion.setText("No hay descripción disponible");
        }

        // 关闭按钮
        btnCloseDetail.setOnClickListener(v -> detailDialog.dismiss());

        // 显示弹窗
        detailDialog.show();
    }

    // 添加获取屏幕宽度的方法
    private int getScreenWidth() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (context instanceof Activity) {
            ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        } else {
            // 备用方法
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics.widthPixels;
    }

    @Override
    public int getItemCount() {
        return cargoList.size();
    }

    static class CargoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCargo;
        TextView tvTipo, tvPeso, tvEstado,tvFecha;
        TextView tvSecurity;


        public CargoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCargo = itemView.findViewById(R.id.imgCargo);
            tvTipo = itemView.findViewById(R.id.tvTipo);
            tvPeso = itemView.findViewById(R.id.tvPeso);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvFecha = itemView.findViewById(R.id.tvFecha); // 初始化 tvFecha
            tvSecurity = itemView.findViewById(R.id.tvSecurity);

        }
    }
}