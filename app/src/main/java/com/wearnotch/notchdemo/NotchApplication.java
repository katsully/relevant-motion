package com.wearnotch.notchdemo;

import android.app.Application;
import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class NotchApplication extends Application {

    private static NotchApplication mInst;

    public static NotchApplication getInst() {
        return mInst;
    }

    @Override
    public void onCreate() {
        mInst = this;
    }
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(Constants.CHAT_SERVER_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}
