package com.example.trailblaze;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.trailblaze.adapters.PlacesListAdapter;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.TravelMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final String PHONE_NUMBER = "669521322"; // Número de teléfono con el código de país
    private MapView mapView;
    private GoogleMap mMap;
    private Handler handler;
    private Runnable runnable;
    private Marker placeMarker;
    private boolean animateCameraToLocation;

    private GeoApiContext geoApiContext;
    private boolean isBuildingRoute = false;
    private boolean popupOpen = false;
    private LatLng miUbicacion;
    private float miDireccion;
    private ImageButton mMyLocationButton;
    private TextView distanceDurationText;
    private LinearLayout buttonLayout;
    String folderName = "trailblaze_rutas"; // Nombre de la carpeta
    private static final int PERMISSION_REQUEST_SEND_SMS = 1;

    private LatLng lastSearch;
    private PlacesClient placesClient;
    private androidx.appcompat.widget.SearchView searchView;
    private ListView placesList;
    private Marker currentLocationMarker;
    private List<Place.Field> placeFields = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
    );

    private DirectionsRoute currentRoute; // Variable global para almacenar la ruta en construcción

    private static final String CHANNEL_ID = "my_channel";
    private static final String CHANNEL_NAME = "My Channel";
    private static final String CHANNEL_DESCRIPTION = "Channel Description";
    private static int notificationId = 0;
    private Dialog popupDialog;

    private void showPopup(String text) {
        popupOpen = true;
        // Inflar el diseño del pop-up
        View popupView = getLayoutInflater().inflate(R.layout.popup_layout, null);

        // Configurar el contenido del pop-up
        TextView textViewContent = popupView.findViewById(R.id.textViewContent);
        textViewContent.setText(text);

        // Crear el diálogo y configurar su apariencia
        popupDialog = new Dialog(this);
        popupDialog.setContentView(popupView);
        popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Configurar el botón de cierre
        Button buttonClose = popupView.findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                closePopup();
                popupOpen = false;
            }
        });

        // Mostrar el pop-up
        popupDialog.show();
    }

    private void closePopup() {
        if (popupDialog != null && popupDialog.isShowing()) {
            popupDialog.dismiss();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = getSystemService(
                    NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String text) {
        // Crear el intent para abrir la actividad cuando se toque la notificación
        Intent intent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Crear la notificación con categoría CATEGORY_CALL
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Trailblaze")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Mostrar la notificación
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, builder.build());
        notificationId++; // incrementar el valor para la siguiente notificación
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Inicializar el TextView distanceDurationText
        distanceDurationText = findViewById(R.id.distance_duration_text);
        distanceDurationText.setVisibility(View.GONE);
        // Obtiene una referencia al botón de "Mi ubicación"
        mMyLocationButton = findViewById(R.id.my_location_button);
        //Obtiene el combo de boton iniciar ruta y añadir parada
        buttonLayout = findViewById(R.id.button_layout);

        TextView menuPhoneNumber = findViewById(R.id.menu_phone_number);
        menuPhoneNumber.setText(PHONE_NUMBER);

        menuPhoneNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setTitle("Editar número de teléfono");

                // Crear un EditText en el cuadro de diálogo para que el usuario ingrese el nuevo número de teléfono
                final EditText editText = new EditText(MapsActivity.this);
                editText.setText(menuPhoneNumber.getText().toString());
                builder.setView(editText);

                builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Obtener el número de teléfono ingresado por el usuario y actualizar el TextView menuPhoneNumber
                        String newPhoneNumber = editText.getText().toString();
                        menuPhoneNumber.setText(newPhoneNumber);
                    }
                });

                builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.create().show();
            }
        });


        File folder = new File(getFilesDir(), folderName);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                showNotification("Carpeta creada correctamente: " + folder.getAbsolutePath());
            } else {
                showNotification("Error al crear la carpeta: " + folder.getAbsolutePath());
            }
        }

        // Verificar si se tiene el permiso SEND_SMS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Si no se tiene el permiso, solicitarlo en tiempo de ejecución
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    PERMISSION_REQUEST_SEND_SMS);
        }

        //Cargar opciones menu lateral
        cargarNombresArchivos();

        //Crear canal de notificaciones
        createNotificationChannel();

        // Obtén la referencia al DrawerLayout y al botón del menú
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ImageView menuButton = findViewById(R.id.menu_button);

        // Establece el OnClickListener en el botón del menú
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    // Si el menú está abierto, ciérralo
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Si el menú está cerrado, ábrelo
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });


        // Configura un click listener para el botón de "Mi ubicación"
        mMyLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateCameraToLocation = true;
                getMyInitLocation();
            }
        });

        // Agregar código para manejar el botón y la barra de búsqueda
        Button hideSearchButton = findViewById(R.id.hide_search_button);
        hideSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListView placesList = findViewById(R.id.placesList);
                placesList.setVisibility(View.GONE);

                SearchView searchView = findViewById(R.id.searchView);
                searchView.setQuery("", true);
            }
        });


        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    hideSearchButton.setVisibility(View.VISIBLE);
                } else {
                    hideSearchButton.setVisibility(View.GONE);
                }
            }
        });

        // Inicializar la API de Google Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);

        mapView = findViewById(R.id.mapView);
        searchView = findViewById(R.id.searchView);
        placesList = findViewById(R.id.placesList);

        // Inicializar el mapa
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Configurar la barra de búsqueda de lugares
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                geoApiContext = new GeoApiContext.Builder()
                        .apiKey(getString(R.string.google_maps_key))
                        .build();

                searchForPlaces(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2) {
                    geoApiContext = new GeoApiContext.Builder()
                            .apiKey(getString(R.string.google_maps_key))
                            .build();
                    searchForPlaces(newText);
                } else {
                    // Limpiar la lista de sugerencias
                    PlacesListAdapter adapter = new PlacesListAdapter(getApplicationContext(), new ArrayList<>());
                    placesList.setAdapter(adapter);
                }
                return true;
            }
        });

        // Mostrar la ventana de autocompletado de lugares cuando se hace clic en la barra de búsqueda
        searchView.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY, fields)
                    .build(getApplicationContext());
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });


    }

    private void addPlaceMarker(Place place) {
        // Eliminar el marcador anterior, si existe
        if (placeMarker != null) {
            placeMarker.remove();
        }

        // Agregar un marcador en la ubicación del lugar
        LatLng latLng = place.getLatLng();
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(place.getName())
                .snippet(place.getAddress());
        placeMarker = mMap.addMarker(markerOptions);

        // Animar la cámara a la ubicación del lugar
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(15)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    private String convertirRouteAString(DirectionsRoute route) {
        Gson gson = new Gson();
        return gson.toJson(route);
    }

    private DirectionsRoute convertirStringARoute(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, DirectionsRoute.class);
    }

    private void guardarCurrentRoute(String filename) throws IOException {
        String content = convertirRouteAString(currentRoute); // Convertir el objeto Route a una cadena de texto
        // Obtener la ruta de la carpeta "trailblaze_rutas" en el almacenamiento interno
        File folder = new File(getFilesDir(), "trailblaze_rutas");
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Log.d(TAG, "Carpeta creada correctamente: " + folder.getAbsolutePath());
            } else {
                Log.e(TAG, "Error al crear la carpeta: " + folder.getAbsolutePath());
                return;
            }
        }
        // Crear el archivo en la carpeta "trailblaze_rutas"
        File file = new File(folder, filename);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
            writer.close();
            // Mostrar un mensaje de éxito
            String text = "Ruta " + filename + " guardada correctamente";
            showNotification(text);

            // Borrar el archivo "tc"
            File fileTc = new File(folder, "tc");
            if (fileTc.exists()) {
                if (fileTc.delete()) {
                    Log.d(TAG, "Archivo 'tc' borrado correctamente");
                } else {
                    Log.e(TAG, "Error al borrar el archivo 'tc'");
                }
            }

            // Borrar el archivo "casa moy"
            File fileCasaMoy = new File(folder, "casa moy");
            if (fileCasaMoy.exists()) {
                if (fileCasaMoy.delete()) {
                    Log.d(TAG, "Archivo 'casa moy' borrado correctamente");
                } else {
                    Log.e(TAG, "Error al borrar el archivo 'casa moy'");
                }
            }

        } catch (IOException e) {
            // Mostrar un mensaje de éxito
            String text = "Error " + filename + " guardada incorrectamente";
            showNotification(text);
            e.printStackTrace();
        }


    }

    private String obtenerContenidoArchivo(String nombreArchivo) {
        // Ruta completa del archivo
        String filePath = getApplicationContext().getFilesDir() + "/trailblaze_rutas/" + nombreArchivo;

        StringBuilder contenido = new StringBuilder();

        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

            String linea;
            while ((linea = bufferedReader.readLine()) != null) {
                contenido.append(linea);
            }

            bufferedReader.close();
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contenido.toString();
    }

    private void cargarNombresArchivos() {
        File directory = new File(getApplicationContext().getFilesDir() + File.separator + folderName);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    agregarOpcionMenu(fileName);
                }
            }
        }
    }

    private void agregarOpcionMenu(String opcion) {
        LinearLayout menuOptionsContainer = findViewById(R.id.menu_options_container);

        TextView textView = new TextView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 16, 0, 0); // Agrega un margen superior de 16dp
        textView.setLayoutParams(layoutParams);
        textView.setText(opcion);
        textView.setPadding(16, 16, 16, 16); // Agrega un relleno de 16dp en los cuatro lados
        textView.setOnClickListener(view -> {
            // Acciones a realizar cuando se seleccione la opción del menú
            Toast.makeText(this, "Seleccionaste la ruta: " + opcion, Toast.LENGTH_SHORT).show();
            String content = obtenerContenidoArchivo(opcion);
            currentRoute = convertirStringARoute(content);
            mMap.clear();
            addRouteToMap(currentRoute);
            // Mostrar los botones "Iniciar Ruta" y "Añadir Parada"
            buttonLayout.setVisibility(View.VISIBLE);

            // Cerrar el menú lateral
            DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawer(GravityCompat.START);

        });

        menuOptionsContainer.addView(textView);
    }

    private void eliminarOpcionesMenu() {
        LinearLayout menuOptionsContainer = findViewById(R.id.menu_options_container);
        menuOptionsContainer.removeAllViews();
    }

    private void searchForPlaces(String query) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Crear una solicitud de búsqueda de lugares
                FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .build();

                // Enviar la solicitud de búsqueda de lugares
                placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
                    // Mostrar la lista de lugares encontrados
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                    PlacesListAdapter adapter = new PlacesListAdapter(getApplicationContext(), predictions);
                    placesList.setAdapter(adapter);

                    // Agregar un listener para cuando se seleccione una sugerencia
                    placesList.setOnItemClickListener((parent, view, position, id) -> {
                        // Ocultar el teclado del dispositivo
                        hideKeyboard();

                        ImageButton saveRouteButton = findViewById(R.id.save_route);
                        SearchView searchView = findViewById(R.id.searchView);
                        ListView placesList = findViewById(R.id.placesList);
                        searchView.setVisibility(View.GONE);
                        placesList.setVisibility(View.GONE);
                        saveRouteButton.setVisibility(View.VISIBLE);


                        AutocompletePrediction prediction = adapter.getItem(position);
                        String placeId = prediction.getPlaceId();

                        // Crear una solicitud para buscar el lugar por su ID
                        FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields)
                                .build();

                        // Enviar la solicitud para buscar el lugar por su ID
                        placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener((fetchPlaceResponse) -> {
                            Place place = fetchPlaceResponse.getPlace();

                            // Crear una solicitud de direcciones
                            DirectionsApiRequest directionsRequest = DirectionsApi.newRequest(geoApiContext);
                            LatLng origin;
                            if (isBuildingRoute) {
                                origin = lastSearch;
                            } else {
                                // Obtener la ubicación del usuario
                                origin = miUbicacion;
                            }

                            // Obtener la ubicación del lugar seleccionado
                            LatLng destination = place.getLatLng();
                            lastSearch = destination;

                            // Configurar las ubicaciones de origen y destino en la solicitud de direcciones
                            directionsRequest.origin(
                                    new com.google.maps.model.LatLng(
                                            origin.latitude,
                                            origin.longitude));
                            directionsRequest.destination(
                                    new com.google.maps.model.LatLng(
                                            destination.latitude,
                                            destination.longitude));
                            directionsRequest.mode(TravelMode.WALKING); // Modo de transporte a pie

                            // Enviar la solicitud de direcciones de forma síncrona
                            try {
                                DirectionsResult result = directionsRequest.await();

                                // Obtener la ruta y agregarla al mapa
                                if (result.routes.length > 0) {
                                    currentRoute = result.routes[0];
                                    addRouteToMap(currentRoute);

                                    // Obtener información de tiempo y distancia a pie
                                    String duration = currentRoute.legs[0].duration.humanReadable;
                                    String distance = currentRoute.legs[0].distance.humanReadable;

                                    // Crear la cadena con el formato deseado
                                    String durationDistanceText = "Duración: " + duration
                                            + "\nDistancia: " + distance;

                                    // Mostrar la cadena en el TextView
                                    distanceDurationText.setText(durationDistanceText);
                                    distanceDurationText.setVisibility(View.VISIBLE);

                                    // Mostrar los botones "Iniciar Ruta" y "Añadir Parada"
                                    buttonLayout.setVisibility(View.VISIBLE);
                                } else {
                                    Log.e(TAG, "No routes found.");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Directions request failed: " + e.getMessage());
                            }
                        }).addOnFailureListener((exception) -> {
                            Log.e(TAG, "Place not found: " + exception.getMessage());
                        });


                    });
                }).addOnFailureListener((exception) -> {
                    Log.e(TAG, "Autocomplete prediction failed: " + exception.getMessage());
                });
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocusView = getCurrentFocus();
        if (currentFocusView != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocusView.getWindowToken(), 0);
        }
    }

    private void addRouteToMap(DirectionsRoute route) {
        // Agregar marcador destino
        mMap.addMarker(new MarkerOptions().position(
                new LatLng(route.legs[0].endLocation.lat,
                        route.legs[0].endLocation.lng)).title("Destino"));

        // Configurar el polilínea de la ruta
        List<LatLng> points = new ArrayList<>();
        for (DirectionsStep step : route.legs[0].steps) {
            points.addAll(PolyUtil.decode(step.polyline.getEncodedPath()));
        }
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .width(10)
                .color(Color.BLUE);

        // Agregar la polilínea al mapa
        mMap.addPolyline(polylineOptions);
        // Ajustar la cámara para mostrar toda la ruta
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int padding = 100; // Espacio en píxeles alrededor de la ruta
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cameraUpdate);
    }

    public void addStop(View view) {
        isBuildingRoute = true;
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setQuery("", false);
        searchView.setVisibility(View.VISIBLE);
        placesList.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Manejar el resultado de la ventana de autocompletado de lugares
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                searchView.setQuery(place.getName(), false);

                LatLng placeLatLng = place.getLatLng();
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(placeLatLng)
                        .zoom(15)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e(TAG, "Error al autocompletar lugares: " + status.getStatusMessage());
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso SEND_SMS otorgado, enviar el SMS

            } else {
                // Permiso SEND_SMS denegado, mostrar un mensaje o realizar otra acción
                // en caso de que el usuario no otorgue el permiso
            }
        }
    }


    private void sendSMS(String text) {
        String smsText = text;

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(PHONE_NUMBER, null,
                smsText, null, null);
    }

    /**
     * Maneja el evento de click del botón de "Mi ubicación".
     */
    public void getMyInitLocation() {
        if (mMap != null) {
            if (isLocationEnabled()) {
                showPopup("Activa ubicación para continuar");
            }
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


                return;
            }


            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(0);
            locationRequest.setFastestInterval(0);
            locationRequest.setExpirationDuration(2000);

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }

                    for (Location location : locationResult.getLocations()) {
                        miUbicacion = new LatLng(location.getLatitude(), location.getLongitude());
                        miDireccion = location.getBearing();
                        if (currentLocationMarker != null) {
                            currentLocationMarker.remove();
                        }

                        printArrowInLocation(location, 50);

                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            // Crear el Handler y el Runnable para actualizar la ubicación cada medio segundo
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling ActivityCompat#requestPermissions
                        // here to request the missing permissions

                        // Mostrar mensaje de solicitud de activación de ubicación
                        Toast.makeText(getApplicationContext(), "La ubicación no está activada", Toast.LENGTH_SHORT).show();

                        return;
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    handler.postDelayed(this, 1000); // Ejecutar el Runnable cada segundo
                }
            };

            handler.postDelayed(runnable, 1000); // Iniciar el Runnable después de un segundo
        }
    }

    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return !gpsEnabled && !networkEnabled;
    }

    public void printArrowInLocation(Location location, float desiredSize) {
        // Obtener la imagen original del recurso drawable
        BitmapDrawable originalDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.ic_arrow);
        Bitmap originalBitmap = originalDrawable.getBitmap();

        // Escalar la imagen del marcador al tamaño deseado
        float scale = desiredSize / originalBitmap.getWidth();
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap scaledBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);

        // Obtener la dirección actual del dispositivo (en grados)
        float currentHeading = location.getBearing();

        // Rotar la imagen del marcador según la dirección actual
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(currentHeading);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), rotationMatrix, true);

        // Crear el BitmapDescriptor a partir del Bitmap resultante
        BitmapDescriptor markerIcon = BitmapDescriptorFactory.fromBitmap(rotatedBitmap);

        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(miUbicacion)
                .title("Mi ubicación")
                .icon(markerIcon));

        if (animateCameraToLocation) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(miUbicacion)
                    .zoom(15)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


            animateCameraToLocation = false;
        }
    }

    public void startRoute(View view) {
        // Obtener referencias a los elementos
        Button startRouteButton = findViewById(R.id.start_route_button);
        Button addStopButton = findViewById(R.id.add_stop_button);
        SearchView searchView = findViewById(R.id.searchView);
        ListView placesList = findViewById(R.id.placesList);
        // Ocultar los botones y la barra de búsqueda
        startRouteButton.setVisibility(View.GONE);
        addStopButton.setVisibility(View.GONE);
        searchView.setVisibility(View.GONE);
        placesList.setVisibility(View.GONE);


        // Crear un Handler para ejecutar la comprobación de ubicación cada segundo
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Comprobar la ubicación del usuario en relación con la ruta
                checkUserLocationOnRoute();
                handler.postDelayed(this, 1000); // Ejecutar el Runnable cada segundo
            }
        };

        handler.postDelayed(runnable, 1000); // Iniciar el Runnable después de 1 segundo
    }


    private void checkUserLocationOnRoute() {
        if (miUbicacion == null) {
            return;
        }

        // Crear una lista de LatLng a partir de la polilínea de la ruta
        List<LatLng> routePoints = new ArrayList<>();
        for (DirectionsStep step : currentRoute.legs[0].steps) {
            routePoints.addAll(PolyUtil.decode(step.polyline.getEncodedPath()));
        }

        // Verificar si la ubicación del usuario está en la ruta
        boolean isOnRoute = PolyUtil.isLocationOnPath(miUbicacion,
                routePoints,
                false,
                10); // Tolerancia de 10 metros

        if (!isOnRoute && !popupOpen) {
            // El usuario está en la ruta
            String text = "Estas alejado 10 metros de la ruta";
            showPopup(text);
            showNotification(text);

            sendSMS("Se ha detectado una anomalía en la ruta");
            // Actualizar la ubicación inicial de currentRoute a miUbicacion
            currentRoute.legs[0].startLocation = new com.google.maps.model.LatLng(
                    miUbicacion.latitude,
                    miUbicacion.longitude);

            addRouteToMap(currentRoute);
        } else {
            cameraInRouteMode();
        }
    }

    public void cameraInRouteMode() {
        // Obtener la ubicación actual del usuario
        LatLng userLocation = new LatLng(miUbicacion.latitude, miUbicacion.longitude);

        // Crear un objeto CameraPosition.Builder
        CameraPosition.Builder builder = new CameraPosition.Builder()
                .target(userLocation) // Ubicación del usuario
                .zoom(mMap.getCameraPosition().zoom) // Mantener el nivel de zoom actual
                .tilt(60); // Ángulo de inclinación en primera persona

        // Obtener la dirección actual del teléfono
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // Establecer la orientación de la cámara según la dirección del teléfono
        builder.bearing(miDireccion);

        // Crear un objeto CameraPosition
        CameraPosition cameraPosition = builder.build();

        // Animar la cámara hacia la nueva posición y ángulo de inclinación
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public void saveRoutePopUp(View view) {
        // Inflar el diseño del pop-up
        View popupView = getLayoutInflater().inflate(R.layout.layout_save_route, null);

        // Crear el diálogo y configurar su apariencia
        popupDialog = new Dialog(this);
        popupDialog.setContentView(popupView);
        popupDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Configurar el botón de cierre
        Button buttonClose = popupView.findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePopup();
            }
        });

        // Configurar el botón de guardado
        Button buttonSave = popupView.findViewById(R.id.buttonSaveRoute);
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextRouteName = popupView.findViewById(R.id.editTextRouteName);
                String routeName = editTextRouteName.getText().toString();
                try {
                    guardarCurrentRoute(routeName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                closePopup();

                //Cargar opciones menu lateral
                eliminarOpcionesMenu();
                cargarNombresArchivos();

            }
        });

        // Mostrar el pop-up
        popupDialog.show();
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        animateCameraToLocation = true;
        getMyInitLocation();


    }



    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
