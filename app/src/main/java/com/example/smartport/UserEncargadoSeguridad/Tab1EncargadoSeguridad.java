package com.example.smartport.UserEncargadoSeguridad;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartport.R;

public class Tab1EncargadoSeguridad extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 创建一个简单的占位视图
        View view = inflater.inflate(R.layout.tab1_encargadoseguridad, container, false);
        return view;
    }
}
