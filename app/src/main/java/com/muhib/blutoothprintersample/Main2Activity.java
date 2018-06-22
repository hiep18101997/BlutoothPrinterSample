package com.muhib.blutoothprintersample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class Main2Activity extends AppCompatActivity {

    Button btnConnect, btnDisconnect, btnPrint;
    EditText txtText;
    TextView lblPrinterName;

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;

    OutputStream outputStream;
    InputStream inputStream;
    Thread thread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnPrint = (Button) findViewById(R.id.btnPrint);

        txtText = (EditText) findViewById(R.id.txtText);
        lblPrinterName = (TextView) findViewById(R.id.lblPrinterName);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    findBluetoothDevice();
                    openBluetoothPrinter();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    disconnectBT();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    printData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void findBluetoothDevice() {
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                lblPrinterName.setText("No bluetooth adapter found");
            }
            if (bluetoothAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, 0);
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice pairedDev : pairedDevices) {
                    if (pairedDev.getName().equals("BTP_F09F1A")) {
                        bluetoothDevice = pairedDev;
                        lblPrinterName.setText("Bluetooth Printer Attached: " + pairedDev.getName());
                        break;
                    }
                }
            }
            lblPrinterName.setText("Bluetooth Printer Attached");
        } catch (Exception e) {

        }
    }

    void openBluetoothPrinter() throws IOException {
        try {

            UUID uuidString = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            bluetoothSocket = bluetoothDevice
                    .createRfcommSocketToServiceRecord(uuidString);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            beginListenData();
        } catch (Exception e) {

        }
    }

    void beginListenData() {
        try {
            final Handler handler = new Handler();
            final byte delimiter = 10;
            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int byteAvailable = inputStream.available();
                            if (byteAvailable > 0) {
                                byte[] packetByte = new byte[byteAvailable];
                                inputStream.read(packetByte);

                                for (int i = 0; i < byteAvailable; i++) {
                                    byte b = packetByte[i];
                                    if (b == delimiter) {
                                        byte[] encodedByte = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedByte, 0,
                                                encodedByte.length
                                        );
                                        final String data = new String(encodedByte, "US-ASCII");
                                        readBufferPosition = 0;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {

                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            stopWorker = true;
                        }
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void printData() throws IOException {
        try {
            String msg = txtText.getText().toString();
            msg += "\n";
            outputStream.write(msg.getBytes());
            lblPrinterName.setText("Printing ....");
        } catch (Exception e) {

        }
    }

    void disconnectBT() throws IOException {
        try {
            stopWorker = true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            lblPrinterName.setText("Printer Disconnecte");
        } catch (Exception e) {

        }
    }
}
