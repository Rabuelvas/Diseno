package com.example.tcp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.LocationRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Locale;

import android.widget.Switch;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;


public class MainActivity extends AppCompatActivity {


    private EditText etIP, port, Mensaje;
    private Button Localizar, UDP, Boton;
    private String mess;
    private Switch Switch, Switch2;
    private int REQUEST_ENABLE_BT = 1234;
    private String distancia = null;
    private Boolean connected = false;
    String item;
    String[] items = {"ABC123","DEF456"};

    AutoCompleteTextView autoCompleteTxt;

    ArrayAdapter<String> adapterItems;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    private final Handler loop = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle b = msg.getData();
            String value = b.getString("KEY");

            try {
                Log.i("distancia", Float.toString(Float.parseFloat(value)));
                distancia = value;
            }catch (Exception e){

            }

        }
    };

    private Bluetooth connection = new Bluetooth(loop);
    DatagramSocket udpSocket;
    BluetoothSocket socket;
    Bluetooth.ConnectedThread thread;

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                try {
                    //Initialize  geoCoder
                    Geocoder geocoder = new Geocoder(MainActivity.this,
                            Locale.getDefault());
                    //Initialize address list

                    List<Address> addresses = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1
                    );

                    //Set latitude en texto


                    Location loc = location;
                    double lat = loc.getLatitude();
                    double log = loc.getLongitude();

                    String latitud = String.valueOf(lat);
                    String longitud = String.valueOf(log);

                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String strDate = sdf.format(c.getTime());
                    if(socket!=null){
                        if(socket.isConnected()) {
                            Mensaje.setText("Latitud: " + latitud + "\nLongitud: " + longitud + "\nTimeStamp :" + strDate + "\nDistancia:" + distancia+"\nID :"+item);
                        }
                    }else{
                        Mensaje.setText("Latitud: " + latitud + "\nLongitud: " + longitud + "\nTimeStamp :" + strDate+"\nID :"+item);
                    }



                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static final String TAG = "MainActivity";
    int LOCATION_REQUEST_CODE = 10001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Mensaje = findViewById(R.id.ubicacion);
        Localizar = findViewById(R.id.coordenadas);
        etIP = findViewById(R.id.etIP);
        port = findViewById(R.id.etPuerto);
        UDP = findViewById(R.id.udp);
        Switch = findViewById(R.id.switchE);
        Switch2 = findViewById(R.id.switchIns);
        Boton = findViewById(R.id.bluetooth);



        Boton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                if(!connected) {
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    String MAC = null;
                    if (bluetoothAdapter == null) {
                        // Device doesn't support Bluetooth
                    } else {
                        if (!bluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        }
                        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                        if (pairedDevices.size() > 0) {
                            // There are paired devices. Get the name and address of each paired device.
                            for (BluetoothDevice device : pairedDevices) {
                                String deviceName = device.getName();
                                String deviceHardwareAddress = device.getAddress(); // MAC address
                                Log.i("Device name", deviceName);
                                if (deviceName.equals("Alex's ESP32")) {
                                    MAC = deviceHardwareAddress;
                                    Log.i("MAC", MAC);
                                }

                            }
                        }

                        if (MAC != null) {
                            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(MAC);
                            Log.i("Device", bluetoothDevice.toString());
                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    try {

                                        socket = connection.connect(bluetoothDevice);
                                        Log.i("Connected", "" + socket.isConnected());
                                        thread = connection.new ConnectedThread(socket);
                                        thread.start();
                                        connected = true;

                                    } catch (IOException e) {
                                        e.printStackTrace();

                                    }
                                }
                            }).start();
                            Toast.makeText(getBaseContext(),"Connected",Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                }else{
                    thread.cancel();
                    connected=false;
                    distancia=null;
                    Toast.makeText(getBaseContext(),"Disconnected",Toast.LENGTH_SHORT).show();
                }
            }

        });

        Localizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    getlocation();
                } else {

                    ActivityCompat.requestPermissions(MainActivity.this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION}, 44);
                }
            }
        });

        UDP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int puerto = Integer.parseInt(port.getText().toString());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mess = Mensaje.getText().toString();
                        try {
                            udpSocket = new DatagramSocket(puerto);
                            InetAddress serverAddr = InetAddress.getByName(etIP.getText().toString());
                            byte[] buf = (mess).getBytes();
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, puerto);
                            udpSocket.send(packet);
                            udpSocket.close();
                        } catch (SocketException e) {
                            //Log.e("Udp:", "Socket Error:", e);
                        } catch (IOException e) {
                            //Log.e("Udp Send:", "IO Error:", e);
                        }

                    }
                }
                ).start();

            }


        });

        autoCompleteTxt = findViewById(R.id.auto_complete_txt);
        adapterItems = new ArrayAdapter<String>(this,R.layout.list_item,items);
        autoCompleteTxt.setAdapter(adapterItems);

        autoCompleteTxt.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?>parent,View view, int position, long id ){
                item = parent.getItemAtPosition(position).toString();

            }
        });
    }


    public void start() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();

        SettingsClient client = LocationServices.getSettingsClient(this
        );

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // start location update
                qwerty();

            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(MainActivity.this, 1001);
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        sendIntentException.printStackTrace();
                    }

                }


            }
        });

    }

    public void qwerty() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    public void enable() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                Log.d(TAG, "asklocationPermission: Alert ");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso Garantizado

                start();
            } else {
                // Permiso Negado
                qwerty();
            }
        }
    }

    public void onclick(View view) {

        if (view.getId() == R.id.switchE) {
            if (Switch.isChecked()) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {

                    start();
                    enviar.run();

                    } else {
                    enable();
                }
                } else {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                loop.removeCallbacks(enviar);

            }
        }

        if (view.getId() == R.id.switchIns) {
            if (Switch2.isChecked()) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {

                    start();
                    enviarToIns.run();

                } else {
                    enable();
                }
            } else {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                loop.removeCallbacks(enviarToIns);
            }
        }
    }

    private Runnable enviar = new Runnable() {

        @Override
        public void run() {

            int puerto = Integer.parseInt(port.getText().toString());
            new Thread(new Runnable() {
                @Override
                public void run() {

                    mess = Mensaje.getText().toString();
                    try {
                        udpSocket = new DatagramSocket(puerto);
                        InetAddress serverAddr = InetAddress.getByName(etIP.getText().toString());
                        byte[] buf = (mess).getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, puerto);
                        udpSocket.send(packet);
                        udpSocket.close();
                    } catch (SocketException e) {
                        //Log.e("Udp:", "Socket Error:", e);
                    } catch (IOException e) {
                        //Log.e("Udp Send:", "IO Error:", e);
                    }

                }
            }
            ).start();
            loop.postDelayed(this, 3000);
        }


    };

    private Runnable enviarToIns = new Runnable() {

        @Override
        public void run() {

            new Thread(new Runnable() {
                @Override
                public void run() {

                    mess = Mensaje.getText().toString();
                    try {
                        udpSocket = new DatagramSocket(3020);
                        InetAddress serverInst = InetAddress.getByName("3.213.123.181");
                        InetAddress serverInst2 = InetAddress.getByName("34.239.66.120");
                        InetAddress serverInst3 = InetAddress.getByName("44.198.119.172");
                        InetAddress serverInst4 = InetAddress.getByName("3.225.130.220");

                        byte[] buf = (mess).getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, serverInst, 3020);
                        DatagramPacket packet2 = new DatagramPacket(buf, buf.length, serverInst2, 3020);
                        DatagramPacket packet3 = new DatagramPacket(buf, buf.length, serverInst3, 3020);
                        DatagramPacket packet4 = new DatagramPacket(buf, buf.length, serverInst4, 3020);

                        udpSocket.send(packet);
                        udpSocket.send(packet2);
                        udpSocket.send(packet3);
                        udpSocket.send(packet4);

                        udpSocket.close();
                    } catch (SocketException e) {
                        //Log.e("Udp:", "Socket Error:", e);
                    } catch (IOException e) {
                        //Log.e("Udp Send:", "IO Error:", e);
                    }

                }
            }
            ).start();
            loop.postDelayed(this, 3000);
        }


    };

    private void getlocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {


                    try {
                        //Initialize  geoCoder
                        Geocoder geocoder = new Geocoder(MainActivity.this,
                                Locale.getDefault());
                        //Initialize address list

                        List<Address> addresses = geocoder.getFromLocation(
                                location.getLatitude(), location.getLongitude(), 1
                        );

                        //Set latitude en texto


                        Location loc = location;
                        double lat = loc.getLatitude();
                        double log = loc.getLongitude();

                        String latitud = String.valueOf(lat);
                        String longitud = String.valueOf(log);

                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String strDate = sdf.format(c.getTime());

                        if(distancia!=null){
                                Mensaje.setText("Latitud: " + latitud + "\nLongitud: " + longitud + "\nTimeStamp :" + strDate + "\nDistancia:" + distancia+"\nID :"+item);
                        }else{
                            Mensaje.setText("Latitud: " + latitud + "\nLongitud: " + longitud + "\nTimeStamp :" + strDate+"\nID :"+item);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }


            }
        });
    }

}
