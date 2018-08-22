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
import android.view.View.*;
import android.widget.Button;
import android.widget.EditText;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;

// websocket imports
import java.io.IOException;
import java.net.InetSocketAddress;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import java.util.Locale;

// notch imports
import com.wearnotch.framework.ActionDevice;
import com.wearnotch.framework.NotchChannel;
import com.wearnotch.framework.NotchNetwork;
import com.wearnotch.notchdemo.util.Util;
import com.wearnotch.service.NotchAndroidService;
import com.wearnotch.service.common.NotchCallback;
import com.wearnotch.service.common.NotchError;
import com.wearnotch.service.common.NotchProgress;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    // websocket server vars
    TextView infoip, msg;
    WebsocketServer wsServer;

    // notch
    private NotchChannel mSelectedChannel;

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
//        mViewPager = (ViewPager) findViewById(R.id.container); // TODO re-add
//        mViewPager.setAdapter(mSectionsPagerAdapter); // TODO re-add

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

//        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout)); // TODO re-add
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


//        Button clickButton = (Button) findViewById(R.id.btn_connect);
//        clickButton.setOnClickListener( new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                System.out.println("this button clicking works");
//
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


//    public void startWebSocket(View v)
//    {
//        System.out.println("starting websocket server...");
//
//        // get the current IP address (to start server with)
//        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int IPbits = wifiInfo.getIpAddress();
//        String ipAddress = String.format(Locale.getDefault(), "%d.%d.%d.%d",
//                (IPbits & 0xff), (IPbits >> 8 & 0xff),
//                (IPbits >> 16 & 0xff), (IPbits >> 24 & 0xff));
//
//
//        Context context = getApplicationContext();
//
//        // websocket server
//        InetSocketAddress inetSockAddress = new InetSocketAddress(ipAddress, 8887);
//        wsServer = new WebsocketServer(inetSockAddress, context, MainActivity.this);
//        wsServer.start();
//    }
//
//    public void stopWebSocket(View v)
//    {
//        System.out.println("stopping websocket server...");
//
//        try {
//            wsServer.stop();
//            System.out.println("websocket server stopped");
//        }
//        catch (IOException e) {
//            System.out.println(e);
//        }
//        catch (InterruptedException e) {
//            System.out.println(e);
//        }
//    }


    // TODO for all buttons
    public void uncheckedinit(View v) {
        System.out.println("unchecking and initing");
    }

    public void click_connect(View v) {
        connectNetwork();
    }

    public void connectNetwork() {
//        inProgress();
        mNotchService.disconnect(new EmptyCallback<Void>(){
            @Override
            public void onSuccess(Void aVoid) {
                final String[] names = new String[] {"UNSPECIFIED","A","B","C","D","E","F","G","H","I",
                        "J","K","L","M","N","O","P","Q","R","S","U","V","W","X","Y","Z",};

                mSelectedChannel = NotchChannel.fromChar(names[1].charAt(0));

                mNotchService.uncheckedInit(mSelectedChannel, new EmptyCallback<NotchNetwork>() {
                    @Override
                    public void onSuccess(NotchNetwork notchNetwork) {
                        super.onSuccess(notchNetwork);
                        updateNetwork();
//                        updateUser(mNotchService.getLicense());
                    }
                });
            }
        });
    }

    private void updateNetwork() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current network:\n");
        if (mNotchService.getNetwork() != null) {
            for (ActionDevice device : mNotchService.getNetwork().getDeviceSet()) {
                sb.append(device.getNetworkId()).append(", ");
            }
        }
        System.out.println(sb.toString());
//        mCurrentNetwork.setText(sb.toString());
    }

    public class EmptyCallback<T> implements NotchCallback<T> {

        @Override
        public void onProgress(NotchProgress notchProgress) {
        }

        @Override
        public void onSuccess(T t) {
            Util.showNotification("Success!");
            clearText();
        }

        @Override
        public void onFailure(NotchError notchError) {
            Util.showNotification(Util.getNotchErrorStr(notchError));
            clearText();
        }

        @Override
        public void onCancelled() {
        }
    }


    private void setCounterText(final TextView text, final String str){
//        setCounterText(text,str,50);
    }

    private void clearText() {
//        setCounterText(mCounterText, "");
    }




//    @BindView(R.id.btn_connect)
//    Button mButtonConnect;
//
//    @OnClick(R.id.btn_connect)
//    void connect() {
////        inProgress();
//        mNotchService.disconnect(new EmptyCallback<Void>(){
//            @Override
//            public void onSuccess(Void aVoid) {
//                mNotchService.uncheckedInit(mSelectedChannel, new EmptyCallback<NotchNetwork>() {
//                    @Override
//                    public void onSuccess(NotchNetwork notchNetwork) {
//                        super.onSuccess(notchNetwork);
//                        updateNetwork();
////                        updateUser(mNotchService.getLicense());
//                    }
//                });
//            }
//        });
//    }





}

