package com.example.smartport.UserCapitan;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smartport.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class CapitanActivity extends AppCompatActivity {
    // Nombres de las pestañas
    private String[] nombres = new String[]{"Home", "Perfil"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capitan);

        // 获取传递的用户角色信息
        String userRole = getIntent().getStringExtra("USER_ROLE");
        String roleName = getIntent().getStringExtra("ROLE_NAME");

        // 可以根据角色显示不同的欢迎信息等
        if (roleName != null) {
            Toast.makeText(this, "Bienvenido, " + roleName, Toast.LENGTH_SHORT).show();
        }

        // Pestañas
        ViewPager2 viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new MiPagerAdapter(this));
        TabLayout tabs = findViewById(R.id.tabs);
        new TabLayoutMediator(tabs, viewPager,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        tab.setText(nombres[position]);
                    }
                }
        ).attach();
    }

    public class MiPagerAdapter extends FragmentStateAdapter {
        public MiPagerAdapter(FragmentActivity activity) {
            super(activity);
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        @Override
        @NonNull
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new Tab1Capitan(); // 暂不实现
                case 1:
                    return new Tab2Capitan();
            }
            return new Tab1Capitan();
        }
    }
}