package com.example.java_tarea_mapa;

import android.animation.ValueAnimator;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap xmap;
    private TextView textViewTiempo, txtTransporteNombre, txtTransporteDetalle;
    private LinearLayout layoutTransporteInfo;
    private CardView btnCaminar, btnBicicleta, btnAuto;

    private Marker marker1 = null;
    private Marker marker2 = null;
    private Polyline polyline = null;
    private Marker animatingMarker = null;
    private boolean siguienteEsSegundo = false;

    private List<LatLng> rutaActual = null;
    private int transporteSeleccionado = 0; // 0: ninguno, 1: caminar, 2: bicicleta, 3: auto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarVistas();
        configurarMapa();
    }

    private void inicializarVistas() {
        textViewTiempo = findViewById(R.id.textViewTiempo);
        layoutTransporteInfo = findViewById(R.id.layoutTransporteInfo);
        txtTransporteNombre = findViewById(R.id.txtTransporteNombre);
        txtTransporteDetalle = findViewById(R.id.txtTransporteDetalle);

        btnCaminar = findViewById(R.id.btnCaminar);
        btnBicicleta = findViewById(R.id.btnBicicleta);
        btnAuto = findViewById(R.id.btnAuto);

        // Configurar listeners para los botones de transporte
        btnCaminar.setOnClickListener(this::onSeleccionarTransporte);
        btnBicicleta.setOnClickListener(this::onSeleccionarTransporte);
        btnAuto.setOnClickListener(this::onSeleccionarTransporte);
    }

    private void configurarMapa() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    public void onSeleccionarTransporte(View view) {
        if (marker1 == null || marker2 == null) {
            Toast.makeText(this, "Primero selecciona ambos puntos en el mapa", Toast.LENGTH_SHORT).show();
            return;
        }

        // Resetear selección anterior
        resetearSeleccionTransporte();

        // Configurar nueva selección
        if (view.getId() == R.id.btnCaminar) {
            transporteSeleccionado = 1;
            btnCaminar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.selected_color));
            mostrarInfoTransporte("🚶 Caminando", "Velocidad: 5 km/h");
        } else if (view.getId() == R.id.btnBicicleta) {
            transporteSeleccionado = 2;
            btnBicicleta.setCardBackgroundColor(ContextCompat.getColor(this, R.color.selected_color));
            mostrarInfoTransporte("🚲 Bicicleta", "Velocidad: 15 km/h");
        } else if (view.getId() == R.id.btnAuto) {
            transporteSeleccionado = 3;
            btnAuto.setCardBackgroundColor(ContextCompat.getColor(this, R.color.selected_color));
            mostrarInfoTransporte("🚗 Automóvil", "Velocidad: 40 km/h");
        }

        // Recalcular ruta con el nuevo transporte
        calcularRutaMejorada();
    }

    private void resetearSeleccionTransporte() {
        int defaultColor = ContextCompat.getColor(this, R.color.transport_default);
        btnCaminar.setCardBackgroundColor(defaultColor);
        btnBicicleta.setCardBackgroundColor(defaultColor);
        btnAuto.setCardBackgroundColor(defaultColor);
        transporteSeleccionado = 0;
    }

    private void mostrarInfoTransporte(String nombre, String detalle) {
        txtTransporteNombre.setText(nombre);
        txtTransporteDetalle.setText(detalle);
        layoutTransporteInfo.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        xmap = googleMap;
        xmap.setOnMapClickListener(this);

        LatLng tarma = new LatLng(-11.41899, -75.68992);
        xmap.moveCamera(CameraUpdateFactory.newLatLngZoom(tarma, 12));
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (!siguienteEsSegundo) {
            if (marker1 != null) marker1.remove();
            marker1 = xmap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Punto de partida")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            );
            Toast.makeText(this, "📍 Punto de partida seleccionado", Toast.LENGTH_SHORT).show();
        } else {
            if (marker2 != null) marker2.remove();
            marker2 = xmap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Destino")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            );
            Toast.makeText(this, "🎯 Destino seleccionado", Toast.LENGTH_SHORT).show();

            calcularRutaMejorada();
        }

        siguienteEsSegundo = !siguienteEsSegundo;
    }

    private void calcularRutaMejorada() {
        if (marker1 == null || marker2 == null) {
            textViewTiempo.setText("Selecciona ambos puntos en el mapa");
            return;
        }

        LatLng punto1 = marker1.getPosition();
        LatLng punto2 = marker2.getPosition();

        // Determinar el tipo de ruta según la distancia y alineación
        rutaActual = generarRutaInteligente(punto1, punto2);

        // Calcular distancia y tiempo
        double distancia = calcularDistanciaReal(rutaActual);
        double tiempoMinutos = calcularTiempo(distancia);

        textViewTiempo.setText(String.format("📏 Distancia: %.1f km\n⏱️ Tiempo: %.1f min", distancia, tiempoMinutos));

        // Dibujar y animar
        dibujarRutaReal();
        animarPorRutaReal(tiempoMinutos);

        Toast.makeText(this, "🗺️ Ruta inteligente generada", Toast.LENGTH_SHORT).show();
    }

    private List<LatLng> generarRutaInteligente(LatLng inicio, LatLng fin) {
        List<LatLng> ruta = new ArrayList<>();

        double distancia = calcularDistanciaSimple(inicio, fin);
        double desviacionMaxima = calcularDesviacionPermitida(distancia);

        // Agregar punto inicial
        ruta.add(inicio);

        // Si los puntos están muy cerca o bien alineados, usar línea recta
        if (distancia < 0.5 || estanBienAlineados(inicio, fin)) {
            // Ruta directa - pocos puntos intermedios
            int puntosIntermedios = Math.max(3, (int)(distancia * 10));

            for (int i = 1; i < puntosIntermedios; i++) {
                double fraction = (double) i / puntosIntermedios;
                double lat = inicio.latitude + (fin.latitude - inicio.latitude) * fraction;
                double lng = inicio.longitude + (fin.longitude - inicio.longitude) * fraction;
                ruta.add(new LatLng(lat, lng));
            }
        } else {
            // Ruta con curvas suaves simulando calles
            int puntosIntermedios = Math.max(5, (int)(distancia * 15));

            for (int i = 1; i < puntosIntermedios; i++) {
                double fraction = (double) i / puntosIntermedios;

                // Punto base en línea recta
                double latBase = inicio.latitude + (fin.latitude - inicio.latitude) * fraction;
                double lngBase = inicio.longitude + (fin.longitude - inicio.longitude) * fraction;

                // Aplicar curvas suaves solo si es necesario
                double lat = latBase;
                double lng = lngBase;

                // Solo agregar curvas en ciertos segmentos para simular calles
                if (i % 4 == 0 && i < puntosIntermedios - 2) {
                    // Curva suave hacia la derecha
                    lat += desviacionMaxima * Math.sin(fraction * Math.PI);
                    lng += desviacionMaxima * Math.cos(fraction * Math.PI);
                } else if (i % 3 == 0 && i < puntosIntermedios - 2) {
                    // Curva suave hacia la izquierda
                    lat -= desviacionMaxima * 0.7 * Math.cos(fraction * Math.PI);
                    lng -= desviacionMaxima * 0.7 * Math.sin(fraction * Math.PI);
                }

                ruta.add(new LatLng(lat, lng));
            }
        }

        // Agregar punto final
        ruta.add(fin);

        return ruta;
    }

    private boolean estanBienAlineados(LatLng punto1, LatLng punto2) {
        double diffLat = Math.abs(punto1.latitude - punto2.latitude);
        double diffLng = Math.abs(punto1.longitude - punto2.longitude);
        return (diffLat / diffLng > 3.0) || (diffLng / diffLat > 3.0);
    }

    private double calcularDesviacionPermitida(double distancia) {
        if (distancia < 0.5) return 0.0001;
        if (distancia < 2.0) return 0.0003;
        return 0.0005;
    }

    private double calcularDistanciaReal(List<LatLng> ruta) {
        if (ruta == null || ruta.size() < 2) return 0;

        double distanciaTotal = 0;
        for (int i = 0; i < ruta.size() - 1; i++) {
            distanciaTotal += calcularDistanciaSimple(ruta.get(i), ruta.get(i + 1));
        }
        return distanciaTotal;
    }

    private double calcularDistanciaSimple(LatLng punto1, LatLng punto2) {
        Location location1 = new Location("");
        location1.setLatitude(punto1.latitude);
        location1.setLongitude(punto1.longitude);

        Location location2 = new Location("");
        location2.setLatitude(punto2.latitude);
        location2.setLongitude(punto2.longitude);

        return location1.distanceTo(location2) / 1000.0;
    }

    private double calcularTiempo(double distancia) {
        double velocidad;

        switch (transporteSeleccionado) {
            case 1: // Caminar
                velocidad = 5.0;
                break;
            case 2: // Bicicleta
                velocidad = 15.0;
                break;
            case 3: // Auto
                velocidad = 40.0;
                break;
            default:
                velocidad = 5.0;
        }

        return (distancia / velocidad) * 60;
    }

    private void dibujarRutaReal() {
        if (polyline != null) polyline.remove();

        if (rutaActual != null && !rutaActual.isEmpty()) {
            polyline = xmap.addPolyline(new PolylineOptions()
                    .addAll(rutaActual)
                    .width(10f)
                    .color(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            );

            // Ajustar cámara para mostrar toda la ruta
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : rutaActual) {
                builder.include(point);
            }
            try {
                xmap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
            } catch (Exception e) {
                xmap.moveCamera(CameraUpdateFactory.newLatLngZoom(rutaActual.get(0), 12));
            }
        }
    }

    private void animarPorRutaReal(double tiempoMinutos) {
        if (animatingMarker != null) animatingMarker.remove();
        if (rutaActual == null || rutaActual.isEmpty()) return;

        // Crear marcador animado
        animatingMarker = xmap.addMarker(new MarkerOptions()
                .position(rutaActual.get(0))
                .title("En movimiento")
                .icon(BitmapDescriptorFactory.defaultMarker(obtenerColorTransporte()))
        );

        // Calcular duración de animación
        long duracion = Math.max(3000, Math.min((long)(tiempoMinutos * 600), 20000));

        // Animar a lo largo de la ruta
        ValueAnimator animator = ValueAnimator.ofInt(0, rutaActual.size() - 1);
        animator.setDuration(duracion);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int index = (int) animation.getAnimatedValue();
                if (index < rutaActual.size() && animatingMarker != null) {
                    animatingMarker.setPosition(rutaActual.get(index));
                }
            }
        });

        animator.start();
    }

    private float obtenerColorTransporte() {
        switch (transporteSeleccionado) {
            case 1: return BitmapDescriptorFactory.HUE_BLUE;
            case 2: return BitmapDescriptorFactory.HUE_GREEN;
            case 3: return BitmapDescriptorFactory.HUE_RED;
            default: return BitmapDescriptorFactory.HUE_BLUE;
        }
    }

    public void CambiarVista(View view) {
        if (xmap != null) {
            int currentMapType = xmap.getMapType();
            if (currentMapType == GoogleMap.MAP_TYPE_NORMAL) {
                xmap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                Toast.makeText(this, "Vista: Satélite", Toast.LENGTH_SHORT).show();
            } else if (currentMapType == GoogleMap.MAP_TYPE_SATELLITE) {
                xmap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                Toast.makeText(this, "Vista: Híbrida", Toast.LENGTH_SHORT).show();
            } else if (currentMapType == GoogleMap.MAP_TYPE_HYBRID) {
                xmap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                Toast.makeText(this, "Vista: Terreno", Toast.LENGTH_SHORT).show();
            } else {
                xmap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                Toast.makeText(this, "Vista: Normal", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void Reset(View view) {
        if (marker1 != null) marker1.remove();
        if (marker2 != null) marker2.remove();
        if (polyline != null) polyline.remove();
        if (animatingMarker != null) animatingMarker.remove();

        marker1 = null;
        marker2 = null;
        polyline = null;
        animatingMarker = null;
        rutaActual = null;
        siguienteEsSegundo = false;

        textViewTiempo.setText("Tiempo estimado: ");
        layoutTransporteInfo.setVisibility(View.GONE);
        resetearSeleccionTransporte();

        Toast.makeText(this, "🔄 Mapa reiniciado", Toast.LENGTH_SHORT).show();
    }
}