package com.wearnotch.notchdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

// component imports
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

// websocket imports
import java.io.IOException;
import java.net.InetSocketAddress;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import java.util.Locale;

// notch imports
import com.wearnotch.service.NotchAndroidService;
import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    // websocket server vars
    TextView infoip, msg;
    WebsocketServer wsServer;

//    Server server;
//    // websocket client
//    TextView response;
//    EditText editTextAddress, editTextPort;
//    Button buttonConnect, buttonClear;


    private MainActivity.SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TABS
        mSectionsPagerAdapter = new MainActivity.SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));


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
            switch (position) {
                case 0:
                    return new Tab1();
                case 1:
                    return new Tab2();
                case 2:
                    return new Tab3();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3; // Show 3 total pages.
        }
    }


    public void setIP(View v)
    {
        System.out.println("clicked");
        wsServer.broadcast( "from the server message" );
    }

//    @OnClick(R.id.btn_set_IP)
//    void setIP() {
//        System.out.println("setting IP");
////        userIPDialog.show();
//    }


    public void startWebSocket(View v)
    {
        System.out.println("starting websocket server...");

        // get the current IP address (to start server with)
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int IPbits = wifiInfo.getIpAddress();
        String ipAddress = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (IPbits & 0xff), (IPbits >> 8 & 0xff),
                (IPbits >> 16 & 0xff), (IPbits >> 24 & 0xff));


        Context context = getApplicationContext();

        // websocket server
        InetSocketAddress inetSockAddress = new InetSocketAddress(ipAddress, 8887);
        wsServer = new WebsocketServer(inetSockAddress, context, MainActivity.this);
        wsServer.start();
    }

    public void stopWebSocket(View v)
    {
        System.out.println("stopping websocket server...");

        try {
            wsServer.stop();
            System.out.println("websocket server stopped");
        }
        catch (IOException e) {
            System.out.println(e);
        }
        catch (InterruptedException e) {
            System.out.println(e);
        }
    }


    // TODO for all buttons
    public void uncheckedinit(View v) {
        System.out.println("unchecking and initing");
    }


}

