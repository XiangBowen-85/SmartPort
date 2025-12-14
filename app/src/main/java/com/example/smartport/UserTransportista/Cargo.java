package com.example.smartport.UserTransportista;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Cargo {
    private static final String TAG = "CargoModel";

    private String id;
    private String imagenUrl;
    private String tipo;
    private double pesoTotal;
    private boolean enviado;
    private String descripcion;
    private Timestamp fechaCreacion;
    private boolean securityChecked;
    private boolean visibleToTransportista;
    private double pesoReal;
    private double diferenciaPeso;
    private String securityStatus; // PENDING / APPROVED / REJECTED / null(legacy)

    public Cargo() {
        // 默认值
        this.pesoTotal = 0.0;
        this.enviado = false;
    }

    // 简化的 setter 方法
    @PropertyName("pesoTotal")
    public void setPesoTotal(Object pesoObj) {
        try {
            if (pesoObj instanceof Double) {
                this.pesoTotal = (Double) pesoObj;
            } else if (pesoObj instanceof Long) {
                this.pesoTotal = ((Long) pesoObj).doubleValue();
            } else if (pesoObj instanceof Integer) {
                this.pesoTotal = ((Integer) pesoObj).doubleValue();
            } else if (pesoObj instanceof String) {
                this.pesoTotal = Double.parseDouble((String) pesoObj);
            } else {
                Log.w(TAG, "Unknown pesoTotal type: " + (pesoObj != null ? pesoObj.getClass().getSimpleName() : "null"));
                this.pesoTotal = 0.0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting pesoTotal: " + pesoObj, e);
            this.pesoTotal = 0.0;
        }
        Log.d(TAG, "pesoTotal set to: " + this.pesoTotal);
    }

    @PropertyName("enviado")
    public void setEnviado(Object enviadoObj) {
        try {
            if (enviadoObj instanceof Boolean) {
                this.enviado = (Boolean) enviadoObj;
            } else if (enviadoObj instanceof String) {
                this.enviado = Boolean.parseBoolean((String) enviadoObj);
            } else {
                Log.w(TAG, "Unknown enviado type: " + (enviadoObj != null ? enviadoObj.getClass().getSimpleName() : "null"));
                this.enviado = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting enviado: " + enviadoObj, e);
            this.enviado = false;
        }
        Log.d(TAG, "enviado set to: " + this.enviado);
    }

    // 其他 getter 和 setter 保持不变
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("imagenUrl")
    public String getImagenUrl() { return imagenUrl; }
    @PropertyName("imagenUrl")
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    @PropertyName("tipo")
    public String getTipo() { return tipo; }
    @PropertyName("tipo")
    public void setTipo(String tipo) { this.tipo = tipo; }

    @PropertyName("pesoTotal")
    public double getPesoTotal() { return pesoTotal; }

    @PropertyName("enviado")
    public boolean isEnviado() { return enviado; }

    @PropertyName("descripcion")
    public String getDescripcion() { return descripcion; }
    @PropertyName("descripcion")
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @PropertyName("fechaCreacion")
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    @PropertyName("fechaCreacion")
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Boolean getSecurityChecked() {
        return securityChecked;
    }

    public void setSecurityChecked(Boolean securityChecked) {
        this.securityChecked = securityChecked;
    }

    public Boolean getVisibleToTransportista() {
        return visibleToTransportista;
    }

    public void setVisibleToTransportista(Boolean visibleToTransportista) {
        this.visibleToTransportista = visibleToTransportista;
    }

    public Double getPesoReal() {
        return pesoReal;
    }

    public void setPesoReal(Double pesoReal) {
        this.pesoReal = pesoReal;
    }

    public Double getDiferenciaPeso() {
        return diferenciaPeso;
    }

    public void setDiferenciaPeso(Double diferenciaPeso) {
        this.diferenciaPeso = diferenciaPeso;
    }

    @PropertyName("securityStatus")
    public String getSecurityStatus() { return securityStatus; }

    @PropertyName("securityStatus")
    public void setSecurityStatus(Object statusObj) {
        if (statusObj == null) {
            this.securityStatus = null; // legacy
            return;
        }
        this.securityStatus = String.valueOf(statusObj);
    }

}