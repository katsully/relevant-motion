package com.wearnotch.notchdemo;

import com.wearnotch.notchdemo.util.Util;

import java.net.InetSocketAddress;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;
import android.app.Activity;


public class WebsocketServer extends WebSocketServer
{
    // vars
    private static final String TAG = "WebSocket";
    InetSocketAddress WSIPAddress;
    Activity mActivity;
    Context MainContext;

    // create
    public WebsocketServer(InetSocketAddress IPAddress, Context context, Activity activity) {
        super(IPAddress);
        WSIPAddress = IPAddress;
        MainContext = context;
        mActivity = activity;
        // TODO Auto-generated constructor stub
    }

    // methods
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // TODO Auto-generated method stub
        broadcast( conn + " has left the room!" );
        System.out.println( conn + " has left the room!" );

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // TODO Auto-generated method stub
        ex.printStackTrace();
        if( conn != null ) {
            // some errors like port binding failed may not be assignable to a specific websocket
            System.out.println("error starting websocket, see logs: " + conn);
            showNotification("error starting websocket, see logs: " + conn);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // TODO Auto-generated method stub
        broadcast( message );
        System.out.println( conn + ": " + message );
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // TODO Auto-generated method stub
        conn.send("Welcome to the server!"); // This method sends a message to the new client
        broadcast( "new connection: " + handshake.getResourceDescriptor() ); // This method sends a message to all clients connected
        System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
    }

    @Override
    public void onStart() {
        System.out.println("Server started: " + WSIPAddress);
        showNotification("server started: " + WSIPAddress);
    }


    // helper
    public void showNotification(final String msg) {
        try {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainContext, msg , Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Toast exception", e);
        }
    }
}