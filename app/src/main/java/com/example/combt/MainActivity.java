package com.example.combt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{

    Button bt_ls,bt_sed;

    BluetoothDevice[] btArray;

    TextView msg_box,status;
    ListView PListView;
    EditText writeMsg;

    SendReceive sendReceive;
    BluetoothAdapter myAdapter;


    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;


    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findById();
        BT_Click();

        myAdapter = BluetoothAdapter.getDefaultAdapter();

        implementListeners();
    }



    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:

                    String tempMsg = null;
                    byte[] readBuff = (byte[]) msg.obj;
                    try
                    {
                        tempMsg = new String(readBuff,"UTF-8");
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        e.printStackTrace();
                    }
                    msg_box.setText(tempMsg);
                    break;
            }
            return false;
        }
    });


    private void implementListeners()
    {
        bt_ls.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Set<BluetoothDevice> bt = myAdapter.getBondedDevices();
                String[] strings = new String[bt.size()];
                btArray = new BluetoothDevice[bt.size()];
                int index = 0 ;

                if (bt.size()>0)
                {
                    for (BluetoothDevice device : bt)
                    {
                        btArray[index] = device;
                        strings[index] = device.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
                    PListView.setAdapter(arrayAdapter);
                }
            }
        });
    }


    private void findById()
    {

        status = (TextView) findViewById(R.id.status);
        msg_box = (TextView) findViewById(R.id.writemsg);
        PListView =(ListView) findViewById(R.id.list);
        writeMsg = (EditText) findViewById(R.id.writemsg);


        bt_ls = (Button) findViewById(R.id.bt_list);
        bt_sed = (Button) findViewById(R.id.bt_send);


    }


    private void BT_Click()
    {
        bt_ls.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                myAdapter.startDiscovery();
            }
        });




        PListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                ClientClass clientClass = new ClientClass(btArray[i]);
                clientClass.start();

                status.setText("Connecting");

            }
        });


        bt_sed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = String.valueOf(writeMsg.getText());
                sendReceive.write (string.getBytes());
            }
        });
    }


    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1)
        {
            device = device1;

            try
            {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        public void run()
        {
            try
            {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }


    private class SendReceive extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket)
        {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try
            {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;
        }
        public  void  run ()
        {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true)
            {
                try
                {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try
            {
                outputStream.write(bytes);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
