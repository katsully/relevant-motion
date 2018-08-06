package com.wearnotch.notchdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

// websocket imports
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import java.net.InetSocketAddress;
import java.net.URI;

import com.wearnotch.service.NotchAndroidService;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    // websocket server
    Server server;
    TextView infoip, msg;
    // websocket client
    TextView response;
    EditText editTextAddress, editTextPort;
    Button buttonConnect, buttonClear;

    WebsocketServerS wsServer;

    private MainActivity.SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // tabs
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new MainActivity.SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        // end tabs


        ButterKnife.bind(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, MainFragment.newInstance())
                    .commit();
        }

        Intent controlServiceIntent = new Intent(this, NotchAndroidService.class);
        startService(controlServiceIntent);

        // to develop app UI without notches you can use a 'mock' version of the SDK
        // it returns success for all SDK calls, to use it uncomment this line
        // controlServiceIntent.putExtra("MOCK", true);

        bindService(controlServiceIntent, this, BIND_AUTO_CREATE);


        //websocket server 2
        String ipAddress = "192.168.0.12";
        InetSocketAddress inetSockAddress = new InetSocketAddress(ipAddress, 8887);
        wsServer = new WebsocketServerS(inetSockAddress);
//        WSServer wsServer = new WSServer(inetSockAddress);
        wsServer.start();

//        WSClient wsClient = new WSClient( new URI("ws://localhost:8887") );
//        wsClient.run();


//        // for websocket server
//        infoip = (TextView) findViewById(R.id.infoip);
//        msg = (TextView) findViewById(R.id.msg);
//        server = new Server(this);
//        System.out.println("~~~~~~~~~~~~~~~~~~~~");
//        System.out.println(server.getIpAddress());
//        System.out.println(infoip);
//        infoip.setText(server.getIpAddress() + ":" + server.getPort());
//
//        // for websocket client
//        editTextAddress = (EditText) findViewById(R.id.addressEditText);
//        editTextPort = (EditText) findViewById(R.id.portEditText);
//        buttonConnect = (Button) findViewById(R.id.connectButton);
//        buttonClear = (Button) findViewById(R.id.clearButton);
//        response = (TextView) findViewById(R.id.responseTextView);
//
//
//        buttonConnect.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View arg0) {
//                Client myClient = new Client(editTextAddress.getText()
//                        .toString(), Integer.parseInt(editTextPort
//                        .getText().toString()), response);
//                myClient.execute();
//            }
//        });
//
//        buttonClear.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                response.setText("");
//            }
//        });


    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
//            return PlaceholderFragment.newInstance(position + 1);
            System.out.println(position);
            switch (position) {
                case 0:
                    return new Tab1();
//                    Tab1 tab1 = new Tab1();
//                    return tab1;
                case 1:
                    return new Tab2();
//                    Tab2 tab2 = new Tab2();
//                    return tab2;
                case 2:
                    return new Tab3();
//                    Tab3 tab3 = new Tab3();
//                    return tab3;
                default:
                    return null;
            }

        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }

    public void setIP(View v, WebSocket conn, ClientHandshake handshake, String message)
    {
        System.out.println("setting IP");
        wsServer.broadcast( "from the server message" );
//        broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This method sends a message to all clients connected

//        Toast.makeText(this, "Clicked on Button", Toast.LENGTH_LONG).show();
    }

//    @OnClick(R.id.btn_set_IP)
//    void setIP() {
//        System.out.println("setting IP");
////        userIPDialog.show();
//    }


}

