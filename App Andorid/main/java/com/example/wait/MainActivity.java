package com.example.wait;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;

import com.example.wait.ui.dashboard.DashBoardActivity;
import com.example.wait.ui.notifications.NotificationsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.example.wait.databinding.ActivityMainBinding;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.IOException;

public class MainActivity extends AppCompatActivity { //implements OnMapReadyCallback

private ActivityMainBinding binding;
private GoogleMap googleMap;
BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

     binding = ActivityMainBinding.inflate(getLayoutInflater());





     setContentView(binding.getRoot());

     Vertx vertx = Vertx.vertx();

     String servidor = "192.168.1.100";

     WebClient client = WebClient.create(vertx);


        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        //mapFragment.getMapAsync(this); // Asigna el mapa al callback

        bottomNavigationView = findViewById(R.id.nav_view);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);





        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                 if (id == R.id.navigation_dashboard) {
                    Intent intent = new Intent(MainActivity.this, DashBoardActivity.class);
                    startActivity(intent);

                    bottomNavigationView.getMenu().findItem(R.id.navigation_home).setChecked(false);
                    bottomNavigationView.getMenu().findItem(R.id.navigation_dashboard).setChecked(false);
                    bottomNavigationView.getMenu().findItem(R.id.navigation_notifications).setChecked(false);
                    return true;
                } else if (id == R.id.navigation_notifications) {
                    Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                    startActivity(intent);

                    bottomNavigationView.getMenu().findItem(R.id.navigation_home).setChecked(false);
                    bottomNavigationView.getMenu().findItem(R.id.navigation_dashboard).setChecked(false);
                    bottomNavigationView.getMenu().findItem(R.id.navigation_notifications).setChecked(false);
                    return true;
                }
                return false;
            }
        });

    }

    //public void onMapReady(GoogleMap map) {
      //  this.googleMap = map;

        //googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    //}

    private void getLocationFromAPI() {
        // URL de tu API
        String apiUrl = "http://localhost:8080/api/sensorsGpsStates/71"; // Cambia esto por la URL de tu API

        // Crear una instancia de OkHttpClient
        OkHttpClient client = new OkHttpClient();

        // Construir la solicitud HTTP
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        // Ejecutar la solicitud de manera asíncrona
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Obtener la respuesta JSON de la API
                    String jsonResponse = response.body().string();

                    try {
                        // Suponiendo que la respuesta sea un JSON con latitud y longitud
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        double latitude = jsonObject.getDouble("valueLat");
                        double longitude = jsonObject.getDouble("valueLong");

                        // Llamamos a la función para agregar el marcador en el mapa
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addMarkerToMap(latitude, longitude);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Función para agregar un marcador en el mapa con la ubicación obtenida.
     */
    private void addMarkerToMap(double latitude, double longitude) {
        if (googleMap != null) {
            // Crear un marcador en la ubicación obtenida
            LatLng location = new LatLng(latitude, longitude);
            googleMap.addMarker(new MarkerOptions().position(location).title("Ubicación Obtenida"));

            // Mover la cámara del mapa para centrarse en el marcador
            googleMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(location, 10));
        }
    }

    public void openMap(View view) {
        Intent intent = new Intent(MainActivity.this, DashBoardActivity.class);
        startActivity(intent);
    }
}



