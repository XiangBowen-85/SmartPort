package com.example.smartport;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView tvAboutContent = findViewById(R.id.tvAboutContent);

        String aboutText = "<b>SmartPort</b> es un sistema avanzado de gestión inteligente de puertos y buques.<br><br>" +

                "Nuestra plataforma integra una <b>plataforma de gestión central</b> con dispositivos IoT estratégicamente ubicados en el puerto para recopilar datos críticos en tiempo real.<br><br>" +

                "Los usuarios autorizados —capitanes, tripulación, personal de seguridad y controladores de terminal— pueden recibir instrucciones, reportar estados, supervisar condiciones de seguridad y controlar instalaciones mediante sus respectivas aplicaciones móviles.<br><br>" +

                "<b>Hardware principal del puerto inteligente:</b><br>" +
                "• <b>Sensores de pesaje</b> para medir con precisión el peso de los camiones.<br>" +
                "• <b>Servomotores</b> que permiten abrir/cerrar puertas y barreras de acceso de forma remota.<br>" +
                "• <b>Sistema RFID</b> (lectores + tarjetas) para autenticación y control de acceso seguro a zonas restringidas.<br><br>" +

                "Todos los datos recopilados alimentan al sistema central, que genera una <b>puntuación integral de eficiencia operativa</b> y proporciona soporte inteligente para la toma de decisiones según el rol de cada usuario.<br><br>" +

                "El uso continuo y disciplinado de SmartPort mejora significativamente la <b>seguridad</b> y la <b>eficiencia</b> de las operaciones portuarias, elemento clave en las cadenas logísticas modernas.<br><br>" +

                "El proyecto abarca áreas como automatización industrial, gestión logística y seguridad pública, transformando profundamente el funcionamiento de los puertos y protegiendo vidas y bienes.";

        // 支持 HTML 标签 + 让链接可点击（如果以后想加网页链接）
        tvAboutContent.setText(Html.fromHtml(aboutText, Html.FROM_HTML_MODE_COMPACT));
        tvAboutContent.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}