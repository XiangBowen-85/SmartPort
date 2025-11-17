package com.example.smartport;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.FacebookSdk;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;


public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 初始化视图
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        // 设置登录按钮点击事件
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarLogin();
            }
        });

        // 检查用户是否已经登录（但不自动跳转）
        verificarUsuarioActual();
    }

    private void verificarUsuarioActual() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            usuario.reload().addOnSuccessListener(aVoid -> {
                if (usuario.isEmailVerified()) {
                    irASelectionActivity();
                }
            }).addOnFailureListener(e -> {
                // reload 失败，留在登录页
            });
        }
    }

    private void iniciarLogin() {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            // 检查邮箱是否已验证
            if (usuario.isEmailVerified()) {
                // 邮箱已验证，可以进入主界面
                irASelectionActivity();
            } else {
                // 邮箱未验证，发送验证邮件
                usuario.sendEmailVerification()
                        .addOnCompleteListener(task -> {
                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this,
                                        "Se ha enviado un email de verificación. Por favor verifica tu cuenta.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Error al enviar email de verificación",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } else {
            // 用户未登录，显示自定义的Firebase UI登录界面
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                    new AuthUI.IdpConfig.FacebookBuilder().build(),
                    new AuthUI.IdpConfig.TwitterBuilder().build());

            // 创建自定义的登录意图（只保留这一个）
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    //.setIsSmartLockEnabled(false)

                    // 🔥 添加自定义配置 🔥
                    .setTheme(R.style.MyLoginTheme)  // 自定义主题
                    .setLogo(R.drawable.logo)   // 设置logo
                    .setTosAndPrivacyPolicyUrls(     // 条款和隐私政策链接
                            "https://example.com/terms.html",
                            "https://example.com/privacy.html")
                    .build();

            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }

    private void irASelectionActivity() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        Toast.makeText(this, "Bienvenido: " + usuario.getDisplayName(),
                Toast.LENGTH_LONG).show();

        Intent i = new Intent(this, SelectionActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // 登录成功，检查邮箱验证
                FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
                if (usuario != null && usuario.isEmailVerified()) {
                    irASelectionActivity();
                } else if (usuario != null) {
                    // 用户登录成功但邮箱未验证
                    usuario.sendEmailVerification();
                    Toast.makeText(this,
                            "Por favor verifica tu email antes de continuar",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                // 登录失败
                String mensajeError = "Error en el login";
                if (response != null && response.getError() != null) {
                    switch (response.getError().getErrorCode()) {
                        case ErrorCodes.NO_NETWORK:
                            mensajeError = "Sin conexión a Internet";
                            break;
                        case ErrorCodes.PROVIDER_ERROR:
                            mensajeError = "Error en proveedor";
                            break;
                        case ErrorCodes.DEVELOPER_ERROR:
                            mensajeError = "Error desarrollador";
                            break;
                        default:
                            mensajeError = "Error de autentificación";
                    }
                } else {
                    mensajeError = "Login cancelado";
                }
                Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show();
            }
        }
    }
}
