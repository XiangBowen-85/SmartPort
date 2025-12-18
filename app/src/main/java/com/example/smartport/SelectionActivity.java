package com.example.smartport;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartport.UserCapitan.CapitanActivity;
import com.example.smartport.UserControlador.ControladorActivity;
import com.example.smartport.UserEncargadoSeguridad.EncargadoSeguridadActivity;
import com.example.smartport.UserTransportista.TransportistaActivity;
import com.example.smartport.utils.AuthManager;
import com.example.smartport.utils.SessionManager;
import com.example.smartport.utils.UserInitializer;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class SelectionActivity extends AppCompatActivity {

    private Button btnTransportista, btnSeguridad, btnControlador, btnCapitan;
    private String selectedRole = "";
    private SessionManager sessionManager;
    private Toolbar toolbar;
    private ImageButton btnLogout;

    private ImageButton btnAbout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.id_selection); // 使用您提供的布局文件

        // 初始化 SessionManager
        sessionManager = new SessionManager(this);

        // 检查并初始化默认用户
        initializeDefaultUsers();

        // 检查用户是否已经登录，如果是则直接跳转到对应页面
        checkExistingSession();

        // 初始化按钮
        initViews();

        // 设置按钮点击事件
        setupClickListeners();

        // 新增：初始化 ToolBar 和退出按钮
        initToolbar();
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        btnLogout = findViewById(R.id.btn_logout);
        btnAbout = findViewById(R.id.btn_about);

        // 设置标题（可选，如果你想动态改）
        // toolbar.setTitle("Seleccione tu identidad");

        btnLogout.setOnClickListener(v -> mostrarDialogoLogout());

        // About 点击事件
        btnAbout.setOnClickListener(v -> {
            startActivity(new Intent(SelectionActivity.this, AboutActivity.class));
        });
    }

    private void mostrarDialogoLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> realizarLogout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void initializeDefaultUsers() {
        UserInitializer.checkAndInitializeUsers(new UserInitializer.InitializationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                if (success) {
                    Log.d("UserInitializer", message);
                } else {
                    Log.e("UserInitializer", message);
                    Toast.makeText(SelectionActivity.this,
                            "Error initializing users: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void checkExistingSession() {
        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getUserRole();
            String username = sessionManager.getUsername();
            navigateToRoleActivity(role, username);
        }
    }

    private void initViews() {
        btnTransportista = findViewById(R.id.Transportista); // Soy Transportista
        btnSeguridad = findViewById(R.id.Seguridad);     // Soy Encargado Seguridad
        btnControlador = findViewById(R.id.Controlador);   // Soy Controlador Barcos
        btnCapitan = findViewById(R.id.Capitan);       // Soy Capitán
    }

    private void setupClickListeners() {
        // 运输人员按钮
        btnTransportista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedRole = "transportista";
                showAuthDialog();
            }
        });

        // 安全负责人按钮
        btnSeguridad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedRole = "seguridad";
                showAuthDialog();
            }
        });

        // 船舶控制员按钮
        btnControlador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedRole = "controlador";
                showAuthDialog();
            }
        });

        // 船长按钮
        btnCapitan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedRole = "capitan";
                showAuthDialog();
            }
        });
    }

    private void showAuthDialog() {
        final Dialog authDialog = new Dialog(this);
        authDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        authDialog.setContentView(R.layout.dialog_auth);
        authDialog.setCancelable(true);

        // 初始化弹窗视图
        TextView tvDialogTitle = authDialog.findViewById(R.id.tvDialogTitle);
        TextView tvRoleInfo = authDialog.findViewById(R.id.tvRoleInfo);
        TextInputEditText etUsername = authDialog.findViewById(R.id.etUsername);
        TextInputEditText etPassword = authDialog.findViewById(R.id.etPassword);
        Button btnCancel = authDialog.findViewById(R.id.btnCancel);
        Button btnLogin = authDialog.findViewById(R.id.btnLogin);
        TextView tvError = authDialog.findViewById(R.id.tvError);

        // 设置角色信息
        String roleName = AuthManager.getRoleDisplayName(selectedRole);
        tvDialogTitle.setText("Verificación: " + roleName);
        tvRoleInfo.setText("Ingrese sus credenciales para acceder como " + roleName);

        // 取消按钮点击事件
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authDialog.dismiss();
                clearSelection();
            }
        });

        // 登录按钮点击事件
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    showError(authDialog, "Por favor, complete todos los campos");
                    return;
                }

                // 显示加载状态
                btnLogin.setEnabled(false);
                btnLogin.setText("Verificando...");

                // 使用新的 Firebase 验证方式
                AuthManager.verifyCredentials(username, password, new AuthManager.AuthCallback() {
                    @Override
                    public void onResult(boolean success, String role) {
                        // 恢复按钮状态
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Verificar");

                        if (success && role != null) {
                            // 验证成功
                            authDialog.dismiss();
                            handleSuccessfulLogin(role, username, password);
                        } else {
                            // 验证失败
                            showError(authDialog, "Usuario o contraseña incorrectos");
                        }
                    }
                });
            }
        });
        // 设置输入框回车键监听
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            btnLogin.performClick();
            return true;
        });

        authDialog.show();
    }

    private void handleSuccessfulLogin(String role, String username, String password) {
        // Save to session
        sessionManager.saveUserCredentials(username, password, role);

        // Navigate to role activity
        navigateToRoleActivity(role, username);
    }

    private void navigateToRoleActivity(String role, String username) {
        Intent intent = null;

        switch (role) {
            case "transportista":
                intent = new Intent(SelectionActivity.this, TransportistaActivity.class);
                break;
            case "seguridad":
                intent = new Intent(SelectionActivity.this, EncargadoSeguridadActivity.class);
                break;
            case "controlador":
                intent = new Intent(SelectionActivity.this, ControladorActivity.class);
                break;
            case "capitan":
                intent = new Intent(SelectionActivity.this, CapitanActivity.class);
                break;
        }

        if (intent != null) {
            // 传递用户角色信息
            intent.putExtra("USER_ROLE", role);
            intent.putExtra("USERNAME", username);
            intent.putExtra("ROLE_NAME", AuthManager.getRoleDisplayName(role));
            startActivityWithFlags(intent);
        }
    }

    private void showError(Dialog dialog, String errorMessage) {
        TextView tvError = dialog.findViewById(R.id.tvError);
        tvError.setText(errorMessage);
        tvError.setVisibility(View.VISIBLE);
    }

    private void clearSelection() {
        selectedRole = "";
    }

    private void startActivityWithFlags(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void realizarLogout() {
        // 1. 清除本地 Session
        sessionManager.logout();

        // 2. 彻底登出 Firebase + 所有第三方（Google、Facebook 等）
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    // 3. 额外保险：手动断开 Google Sign-In 连接（关键！）
                    GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                            .signOut()
                            .addOnCompleteListener(this, task1 -> {
                                // 4. 最后再彻底清除 Firebase 当前用户
                                FirebaseAuth.getInstance().signOut();

                                // 5. 跳转到 LoginActivity
                                Intent intent = new Intent(SelectionActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                });
    }
}