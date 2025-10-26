package com.example.java_tarea_mapa;

import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener{

    EditText xlatitud, xlongitud, xlatitud2, xlongitud2, xdistancia;
    GoogleMap xmap;
    Marker marker1 = null;
    Marker marker2 = null;
    Polyline linea = null;
    boolean siguienteEsSegundo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        xlatitud = findViewById(R.id.latitud);
        xlongitud = findViewById(R.id.longitud);
        xlatitud2 = findViewById(R.id.latitud2);
        xlongitud2 = findViewById(R.id.longitud2);
        xdistancia = findViewById(R.id.distancia);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }
    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        // Alterna entre Ubicación 1 y Ubicación 2 en cada click
        if (!siguienteEsSegundo) {
            // Guardar como Ubicación 1
            xlatitud.setText(String.valueOf(latLng.latitude));
            xlongitud.setText(String.valueOf(latLng.longitude));

            if (marker1 != null) marker1.remove();
            marker1 = xmap.addMarker(new MarkerOptions().position(latLng).title("Ubicación 1"));

            // Si hay una línea/marker2 previamente, la mantenemos pero no calculamos hasta que el usuario ponga Ubicación 2
        } else {
            // Guardar como Ubicación 2
            xlatitud2.setText(String.valueOf(latLng.latitude));
            xlongitud2.setText(String.valueOf(latLng.longitude));

            if (marker2 != null) marker2.remove();
            marker2 = xmap.addMarker(new MarkerOptions().position(latLng).title("Ubicación 2"));
        }

        // Alternar para el siguiente click
        siguienteEsSegundo = !siguienteEsSegundo;

        // Si ambos puntos existen, dibujar polyline y calcular distancia automáticamente
        if (!xlatitud.getText().toString().isEmpty() && !xlongitud.getText().toString().isEmpty()
                && !xlatitud2.getText().toString().isEmpty() && !xlongitud2.getText().toString().isEmpty()) {
            try {
                double lat1 = Double.parseDouble(xlatitud.getText().toString());
                double lon1 = Double.parseDouble(xlongitud.getText().toString());
                double lat2 = Double.parseDouble(xlatitud2.getText().toString());
                double lon2 = Double.parseDouble(xlongitud2.getText().toString());

                LatLng p1 = new LatLng(lat1, lon1);
                LatLng p2 = new LatLng(lat2, lon2);

                // Remover polyline anterior si existe
                if (linea != null) linea.remove();
                linea = xmap.addPolyline(new PolylineOptions().add(p1, p2).width(6));

                // Calcular distancia con Location (metros -> km)
                Location loc1 = new Location("");
                loc1.setLatitude(lat1);
                loc1.setLongitude(lon1);

                Location loc2 = new Location("");
                loc2.setLatitude(lat2);
                loc2.setLongitude(lon2);

                float distanciaMetros = loc1.distanceTo(loc2);
                float distanciaKm = distanciaMetros / 1000f;
                xdistancia.setText(String.format("%.3f km", distanciaKm));

                // Ajustar cámara para ver ambos puntos (centro aproximado)
                double centroLat = (lat1 + lat2) / 2.0;
                double centroLon = (lon1 + lon2) / 2.0;
                xmap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(centroLat, centroLon), 10f));

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Error al parsear coordenadas", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        xmap = googleMap;
        xmap.setOnMapClickListener(this);

        LatLng tarma = new LatLng(-11.41899, -75.68992);
        xmap.addMarker(new MarkerOptions().position(tarma).title("Tarma"));
        xmap.moveCamera(CameraUpdateFactory.newLatLngZoom(tarma, 10));
    }

    public void Cambiar(View v) {
        if (xmap != null) xmap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }

    // Método opcional para borrar puntos y volver a empezar (puedes llamarlo desde un botón si quieres)
    public void Resetear(View v) {
        if (marker1 != null) marker1.remove();
        if (marker2 != null) marker2.remove();
        if (linea != null) linea.remove();
        marker1 = marker2 = null;
        linea = null;
        xlatitud.setText("");
        xlongitud.setText("");
        xlatitud2.setText("");
        xlongitud2.setText("");
        xdistancia.setText("");
        siguienteEsSegundo = false;
    }
}