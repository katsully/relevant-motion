package com.wearnotch.notchdemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;

import com.wearnotch.service.NotchAndroidService;

import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        //String getIP = loadCredentials();

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
    String loadCredentials() {

        String l_ipStr = null, l_usrnameStr = null, l_passwdStr = null;
        Intent l_intent;
        l_intent = getIntent();
        if (l_intent != null) {
            Uri l_uri = l_intent.getData();
            if (l_uri != null) {
                l_ipStr = l_uri.getQueryParameter("IP");
            }
        }

        if (l_ipStr == null) {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            //l_advcodeStr = "19A2-2657-78A8-B648";
            //l_usrnameStr = "My_Family";
            //l_passwdStr = "b3w1h6o2m6z4";
            // l_advcodeStr = sharedPref.getString(getResources().getString(R.string.mprAdvKeyStr), "");
            l_ipStr = sharedPref.getString(getResources().getString(R.string.mprUsrKeyStr), "");

        }

        //l_advCode = (EditText) findViewById(R.id.advCode);
        return l_ipStr;

        /*if (l_advcodeStr.isEmpty()) {
            MyJavaScriptInterface l_js = null;
            l_js = ReadJavaScriptLocalStorage();*/

        }


    void setIPText(String s){
        EditText l_IPEDITTEXT = (EditText) findViewById(R.id.current_IP);
        l_IPEDITTEXT.setText(s);
    }

    void saveCredentials(String l_SetIP)
    {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.mprUsrKeyStr), l_SetIP);
        editor.commit();
    }
}
