package com.example.wait.ui.notifications;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wait.MainActivity;
import com.example.wait.R;
import com.example.wait.databinding.ActivityMainBinding;
import com.example.wait.databinding.FragmentDashboardBinding;
import com.example.wait.databinding.FragmentNotificationsBinding;
import com.example.wait.ui.dashboard.DashBoardActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NotificationsActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;


    private FragmentNotificationsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = FragmentNotificationsBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
         // Creamos el layout XML luego

        // Inicializamos la barra de navegación
        bottomNavigationView = findViewById(R.id.nav_viewN);
        bottomNavigationView.setSelectedItemId(R.id.navigation_notifications);

        // EditTexts



        // Botón para enviar texto (Opcional)


        // Navegación entre actividades
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.navigation_home) {
                    Intent intent = new Intent(NotificationsActivity.this, MainActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.navigation_dashboard) {
                    Intent intent = new Intent(NotificationsActivity.this, DashBoardActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
    }


}
