package com.example.tcp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class MainActivity extends AppCompatActivity {


    private EditText etIP, port, Mensaje;
    private Button Enviar, Localizar, UDP;
    private String mess;
    FusedLocationProviderClient fusedLocationProviderClient;
    Socket s;
    PrintWriter pw;
    DatagramSocket udpSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Mensaje = findViewById(R.id.ubicacion);
        Enviar = findViewById(R.id.send);
        Localizar = findViewById(R.id.bt_location);
        etIP = findViewById(R.id.etIP);
        port = findViewById(R.id.etPuerto);
        UDP = findViewById(R.id.udp);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
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

        Enviar.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View view) {

                mess = Mensaje.getText().toString();
                int puerto = Integer.parseInt(port.getText().toString());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            s = new Socket(etIP.getText().toString(), puerto);
                            pw = new PrintWriter(s.getOutputStream());
                            pw.write(mess);
                            pw.flush();
                            s.close();


                        } catch (IOException e) {
                            e.printStackTrace();

                        }


                    }
                }
                ).start();

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

    }

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

                        String dir = String.valueOf(addresses.get(0).getAddressLine(0));

                        Mensaje.setText("Latitud: " + latitud + "\nLongitud: " + longitud+"\nTimeStamp :"+strDate);


                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }


            }
        });
    }


}