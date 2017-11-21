package com.wearnotch.notchdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.illposed.osc.utility.OSCPacketDispatcher;
import com.wearnotch.db.model.Device;
import com.wearnotch.db.NotchDataBase;
import com.wearnotch.framework.ActionDevice;
import com.wearnotch.framework.Bone;
import com.wearnotch.framework.ColorPair;
import com.wearnotch.framework.Measurement;
import com.wearnotch.framework.MeasurementType;
import com.wearnotch.framework.NotchChannel;
import com.wearnotch.framework.NotchNetwork;
import com.wearnotch.framework.Skeleton;
import com.wearnotch.framework.Workout;
import com.wearnotch.internal.util.IOUtil;
import com.wearnotch.notchdemo.util.Util;
import com.wearnotch.notchdemo.visualiser.VisualiserActivity;
import com.wearnotch.notchdemo.visualiser.VisualizerSettings;
import com.wearnotch.service.common.Cancellable;
import com.wearnotch.service.common.NotchCallback;
import com.wearnotch.service.common.NotchError;
import com.wearnotch.service.common.NotchProgress;
import com.wearnotch.framework.visualiser.VisualiserData;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

// OSC imports
import java.net.*;
import java.util.*;
import com.illposed.osc.*;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class MainFragment extends BaseFragment {
    private static final String TAG = MainFragment.class.getSimpleName();

    // reading from local.properties
//    private static final String DEFAULT_USER_LICENSE = System.getProperty("license");
    private static final String DEFAULT_USER_LICENSE = "ZvqYLovXeNGREMadVnRE";


    private static final String NOTCH_DIR = "notch_tutorial";
    private static final long CALIBRATION_TIME = 7000L;
    private static final long TIMED_CAPTURE_LENGTH = 2000L;

    private static final int REQUEST_ALL_PERMISSION = 1;
    private static String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION };


    public static MainFragment newInstance() {
        return new MainFragment();
    }

    private Activity mActivity;
    private NotchDataBase mDB;
    private Measurement mCurrentMeasurement;
    private File mCurrentOutput;
    private String mUser;
    private boolean mRealTime, mRemote, mChangingChannel, mOSC;
    private VisualiserData mRealTimeData;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private SimpleDateFormat mSDF;
    private VisualiserActivity mVisualiserActivity;
    private Workout mWorkout;
    private NotchChannel mSelectedChannel;

    private AnimationDrawable mDockAnimation;

    private File mOutputDir = Environment.getExternalStoragePublicDirectory(NOTCH_DIR);



    private enum State {CALIBRATION,STEADY,CAPTURE}
    private State mState;

    @InjectView(R.id.new_title)
    TextView mNewTitle;

    @InjectView(R.id.device_list)
    TextView mDeviceList;

    @InjectView(R.id.device_management_txt)
    TextView mDeviceManagementTxt;

    @InjectView(R.id.selected_channel_txt)
    TextView mSelectedChannelTxt;

    @InjectView(R.id.calibration_txt)
    TextView mCalibrationTxt;

    @InjectView(R.id.steady_txt)
    TextView mSteadyTxt;

    @InjectView(R.id.capture_txt)
    TextView mCaptureTxt;

    @InjectView(R.id.current_network)
    TextView mCurrentNetwork;

    @InjectView(R.id.chk_realtime)
    CheckBox mRealTimeBox;

    @InjectView(R.id.chk_remote)
    CheckBox mRemoteBox;

    @InjectView(R.id.chk_OSC)
    CheckBox mOSCBox;

    // Buttons
    @InjectView(R.id.btn_set_user)
    Button mButtonSetUser;

    @InjectView(R.id.btn_pair)
    Button mButtonPair;

    @InjectView(R.id.btn_sync_pair)
    Button mButtonSyncPair;

    @InjectView(R.id.btn_remove)
    Button mButtonRemove;

    @InjectView(R.id.btn_connect)
    Button mButtonConnect;

    @InjectView(R.id.btn_disconnect)
    Button mButtonDisconnect;

    @InjectView(R.id.btn_shutdown)
    Button mButtonShutDown;

    @InjectView(R.id.btn_erase)
    Button mButtonErase;

    @InjectView(R.id.btn_change_channel)
    Button mButtonChangeChannel;

    @InjectView(R.id.btn_unchecked_init)
    Button mButtonUncheckedInit;

    @InjectView(R.id.btn_configure_calib)
    Button mButtonConfigureCalib;

    @InjectView(R.id.btn_calibrate)
    Button mButtonCalibrate;

    @InjectView(R.id.btn_get_calibration)
    Button mButtonGetCalibData;

    @InjectView(R.id.btn_init_3_steady)
    Button mButtonInitSteady;

    @InjectView(R.id.btn_configure_steady)
    Button mButtonConfigureSteady;

    @InjectView(R.id.btn_steady)
    Button mButtonSteady;

    @InjectView(R.id.btn_get_steady)
    Button mButtonGetSteadyData;

    @InjectView(R.id.btn_init_3_capture)
    Button mButtonInitCapture;

    @InjectView(R.id.btn_configure_capture)
    Button mButtonConfigure;

    @InjectView(R.id.btn_capture)
    Button mButtonCapture;

    @InjectView(R.id.btn_download)
    Button mButtonDownload;

    @InjectView(R.id.btn_post_download)
    Button mButtonPostDownload;

    @InjectView(R.id.btn_visualize)
    Button mButtonVisualize;

    @InjectView(R.id.btn_show_example)
    Button mButtonShowExample;

    @InjectView(R.id.counter_text)
    TextView mCounterText;

    @InjectView(R.id.dock_image)
    ImageView mDockImg;

    AlertDialog userDialog, fileDialog, channelDialog;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        bindNotchService();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationContext = getActivity().getApplicationContext();
        mActivity = getBaseActivity();
        mDB = NotchDataBase.getInst();
        VisualizerSettings.init(mApplicationContext);

        if(!hasPermissions(mActivity, PERMISSIONS)){
            requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION);
        }

        // Start the thread that sends messages
        oscThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.inject(this, root);

        // Set typefaces
        Typeface tfLight = Typeface.createFromAsset(mApplicationContext.getAssets(), "fonts/Lato-Light.ttf");
        Typeface tfBold = Typeface.createFromAsset(mApplicationContext.getAssets(), "fonts/Lato-Bold.ttf");

        mNewTitle.setTypeface(tfBold);
        mDeviceManagementTxt.setTypeface(tfBold);
        mSelectedChannelTxt.setTypeface(tfLight);
        mCalibrationTxt.setTypeface(tfBold);
        mSteadyTxt.setTypeface(tfBold);
        mCaptureTxt.setTypeface(tfBold);

        mCurrentNetwork.setTypeface(tfLight);
        mDeviceList.setTypeface(tfLight);

        // Set button typeface
        mButtonSetUser.setTypeface(tfLight);
        mButtonPair.setTypeface(tfLight);
        mButtonSyncPair.setTypeface(tfLight);
        mButtonRemove.setTypeface(tfLight);
        mButtonShutDown.setTypeface(tfLight);
        mButtonConnect.setTypeface(tfLight);
        mButtonDisconnect.setTypeface(tfLight);
        mButtonErase.setTypeface(tfLight);
        mButtonChangeChannel.setTypeface(tfLight);
        mButtonUncheckedInit.setTypeface(tfLight);
        mButtonConfigureCalib.setTypeface(tfLight);
        mButtonCalibrate.setTypeface(tfLight);
        mButtonGetCalibData.setTypeface(tfLight);
        mButtonInitSteady.setTypeface(tfLight);
        mButtonConfigureSteady.setTypeface(tfLight);
        mButtonSteady.setTypeface(tfLight);
        mButtonGetSteadyData.setTypeface(tfLight);
        mButtonInitCapture.setTypeface(tfLight);
        mButtonConfigure.setTypeface(tfLight);
        mButtonCapture.setTypeface(tfLight);
        mButtonDownload.setTypeface(tfLight);
        mButtonPostDownload.setTypeface(tfLight);
        mButtonVisualize.setTypeface(tfLight);
        mButtonShowExample.setTypeface(tfLight);

        mCounterText.setTypeface(tfLight);
        mRealTimeBox.setTypeface(tfLight);
        mOSCBox.setTypeface(tfLight);
        mRemoteBox.setTypeface(tfLight);


        // Animation
        mDockImg.setBackgroundResource(R.drawable.sensor_anim);
        mDockAnimation = (AnimationDrawable) mDockImg.getBackground();
        mDockImg.setVisibility(View.INVISIBLE);

        // Other
        mSDF = new SimpleDateFormat("yyyyMMdd_HHmmss");

        buildUserDialog();
        buildChannelDialog();
        try {
            checkAndCopyExample();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mHandler.postDelayed(mSetDefaultUser, 1000L);

        return root;
    }

    Runnable mSetDefaultUser = new Runnable() {
        @Override
        public void run() {
            setActionBarTitle(R.string.app_name);
            if (mNotchService != null && mUser == null) {
                System.out.println(DEFAULT_USER_LICENSE);
                mUser = DEFAULT_USER_LICENSE;
                if (DEFAULT_USER_LICENSE.length() > 0) {
                    updateUser(mUser);
                    mNotchService.setLicense(mUser);
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        setActionBarTitle(R.string.app_name);
        if (mNotchService != null && mUser == null) {
            mUser = DEFAULT_USER_LICENSE;
            updateUser(mUser);
            mNotchService.setLicense(mUser);
        }
    }

    @OnCheckedChanged(R.id.chk_realtime)
    void checkRealTime() {
        mRealTime = mRealTimeBox.isChecked();
        mButtonCapture.setText(mRealTime ? "START REAL-TIME" : "CAPTURE 2 SEC");
        mButtonConfigure.setText(mRealTime ? "CONFIGURE REAL-TIME" : "CONFIGURE 2 SEC CAPTURE");
        mButtonDownload.setText(mRealTime ? "STOP REAL-TIME" : "DOWNLOAD");
        mButtonInitSteady.setText(mRealTime ? "INIT 2 NOTCHES" : "INIT 3 NOTCHES");
        mButtonInitCapture.setText(mRealTime ? "INIT 2 NOTCHES" : "INIT 3 NOTCHES");
        mRemoteBox.setEnabled(!mRealTime);
    }

    @OnCheckedChanged(R.id.chk_remote)
    void checkRemote() {
        mRemote = mRemoteBox.isChecked();
        mRealTimeBox.setEnabled(!mRemote);
    }

    // ReMo OSC Button
    @OnCheckedChanged(R.id.chk_OSC)
    void OSCtoggle() {
        mOSC = mOSCBox.isChecked();
        mOSCBox.setChecked(mOSC);
//        System.out.println(mOSC);
    }

    @OnClick(R.id.btn_set_user)
    void setUser() {
        userDialog.show();
    }

    @OnClick(R.id.btn_pair)
    void pair() {
        inProgress();
        mNotchService.pair(new EmptyCallback<Device>() {
            @Override
            public void onSuccess(Device device) {
                updateUser(mNotchService.getLicense());
                shutdown();
            }
        });
    }

    @OnClick(R.id.btn_sync_pair)
    void syncPair() {
        inProgress();
        mNotchService.syncPairedDevices(new EmptyCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateUser(mNotchService.getLicense());
                super.onSuccess(aVoid);
            }
        });
    }

    @OnClick(R.id.btn_remove)
    void remove() {
        inProgress();
        mNotchService.deletePairedDevices(null, new EmptyCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateUser(mNotchService.getLicense());
                mNotchService.disconnect(new EmptyCallback<Void>() {});
            }
        });

    }

    @OnClick(R.id.btn_connect)
    void connect() {
        inProgress();
        mNotchService.disconnect(new EmptyCallback<Void>(){
            @Override
            public void onSuccess(Void aVoid) {
                mNotchService.uncheckedInit(mSelectedChannel, new EmptyCallback<NotchNetwork>() {
                    @Override
                    public void onSuccess(NotchNetwork notchNetwork) {
                        super.onSuccess(notchNetwork);
                        updateNetwork();
                        updateUser(mNotchService.getLicense());
                    }
                });
            }
        });
    }

    @OnClick(R.id.btn_disconnect)
    void disconnect() {
        mNotchService.disconnect(new EmptyCallback<Void>(){
            @Override
            public void onSuccess(Void aVoid) {
                super.onSuccess(aVoid);
                updateNetwork();
            }
        });
    }

    @OnClick(R.id.btn_shutdown)
    void shutdown() {
        inProgress();
        mNotchService.shutDown(new EmptyCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateNetwork();
                super.onSuccess(aVoid);
            }
        });

    }

    @OnClick(R.id.btn_erase)
    void erase() {
        inProgress();
        mNotchService.erase(new EmptyCallback<Void>());
    }

    @OnClick(R.id.selected_channel_txt)
    void selectChannel() {
        mChangingChannel = false;
        channelDialog.show();
    }


    @OnClick(R.id.btn_change_channel)
    void changeChannel() {
        mChangingChannel = true;
        mSelectedChannel = null;
        channelDialog.show();
    }


    @OnClick(R.id.btn_unchecked_init)
    void uncheckedinit() {
        inProgress();
        mNotchService.uncheckedInit(mSelectedChannel, new EmptyCallback<NotchNetwork>() {
            @Override
            public void onSuccess(NotchNetwork notchNetwork) {
                updateNetwork();
                super.onSuccess(notchNetwork);
            }
        });
    }

    @OnClick(R.id.btn_configure_calib)
    void configureCalib() {
        inProgress();
        mNotchService.configureCalibration(true, new EmptyCallback<Void>());
    }

    @OnClick(R.id.btn_calibrate)
    void calibrate() {
        mState = State.CALIBRATION;
        mCountDown.start();
    }

    @OnClick(R.id.btn_get_calibration)
    void getCalibData() {
        inProgress();
        mNotchService.getCalibrationData(new EmptyCallback<Boolean>());
    }

    @OnClick(R.id.btn_init_3_steady)
    void initSteady() {
        Skeleton skeleton;
        try {
            skeleton = Skeleton.from(new InputStreamReader(mApplicationContext.getResources().openRawResource(R.raw.skeleton_male), "UTF-8"));
            Workout workout = Workout.from("Demo_config", skeleton, IOUtil.readAll(new InputStreamReader(mApplicationContext.getResources().openRawResource(R.raw.config_3_right_arm))));
            if (mRealTime) {
                workout = Workout.from("Demo_config", skeleton, IOUtil.readAll(new InputStreamReader(mApplicationContext.getResources().openRawResource(R.raw.config_2_real_time))));
//                workout = Workout.from("Demo_config", skeleton, IOUtil.readAll(new InputStreamReader(mApplicationContext.getResources().openRawResource(R.raw.config_12_real_time))));

                workout = workout.withMeasurementType(MeasurementType.STEADY_SKIP);
            }
            mWorkout = workout;
            inProgress();
            mNotchService.init(mSelectedChannel, workout, new EmptyCallback<NotchNetwork>() {
                @Override
                public void onSuccess(NotchNetwork notchNetwork) {
                    updateNetwork();
                    super.onSuccess(notchNetwork);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error while loading skeleton file!", e);
            Util.showNotification(R.string.error_skeleton);
        }

    }


    @OnClick(R.id.btn_configure_steady)
    void configureSteady() {
        // Display bone-notch configuration
        StringBuilder sb = new StringBuilder();
        sb.append("Measured bones:\n\n");
        if (mWorkout != null) {
            for (Workout.BoneInfo info : mWorkout.getBones().values()) {
                ColorPair colors = info.getColor();
                sb.append(info.getBone().getName()). append(": ");
                sb.append(colors.getPrimary().toString());
                sb.append(colors.getPrimary().equals(colors.getSecondary()) ? "" : ", " + colors.getSecondary().toString());
                sb.append("\n");
            }
        }
        setCounterText(mCounterText,sb.toString(),30);
        mNotchService.configureSteady(MeasurementType.STEADY_SIMPLE, true, new EmptyCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Do nothing, leave text visible
            }
        });
    }

    @OnClick(R.id.btn_steady)
    void steady() {
        mState = State.STEADY;
        mCountDown.start();
    }

    @OnClick(R.id.btn_get_steady)
    void getSteadyData() {
        inProgress();
        mNotchService.getSteadyData(new EmptyCallback<Void>());
    }

    @OnClick(R.id.btn_init_3_capture)
    void initCapture() {
        initSteady();
    }


    @OnClick(R.id.btn_configure_capture)
    void configureCapture() {
        if (mRealTime) {
            inProgress();
            mNotchService.configureCapture(false, new EmptyCallback<Void>());
        }
        else {
            inProgress();
            mNotchService.configureTimedCapture(TIMED_CAPTURE_LENGTH, false, new EmptyCallback<Void>());
        }
    }

    Cancellable c;

    @OnClick(R.id.btn_capture)
    void capture() {
        mVisualiserActivity = null;
        if (mRealTime) {
            inProgress();
            c = mNotchService.capture(new NotchCallback<Void>() {
                @Override
                public void onProgress(NotchProgress progress) {
                    if (progress.getState() == NotchProgress.State.REALTIME_UPDATE) {
                        mRealTimeData = (VisualiserData) progress.getObject();
                        updateRealTime();
                        mUpdateStartTime = System.currentTimeMillis();
                        mHandler.removeCallbacks(mLogRealTimeData);
                        mHandler.post(mLogRealTimeData);
                    }
                }

                @Override
                public void onSuccess(Void nothing) {
                    clearText();
                    Log.d("onSuccessRealtime", "realtime called");
                }

                @Override
                public void onFailure(NotchError notchError) {
                    Util.showNotification(Util.getNotchErrorStr(notchError));
                    clearText();
                    mHandler.removeCallbacks(mLogRealTimeData);
                    Log.d("onFailure", "realtime failed");
                }

                @Override
                public void onCancelled() {
                    Util.showNotification("Real-time measurement stopped!");
                    clearText();
                    mHandler.removeCallbacks(mLogRealTimeData);
                    Log.d("onCancelled", "realtime cancelled");
                }
            });
        }
        else {
            mNotchService.timedCapture(new EmptyCallback<Measurement>() {
                @Override
                public void onSuccess(Measurement measurement) {
                    mCurrentMeasurement = measurement;
                    Util.showNotification("Capture finished");
                    clearText();
                }
            });
        }
    }

    // realtime vars
    long mUpdateStartTime;
    long mRefreshTime = 20;
    // Bone Object format: [0]=name, [1]=posX, [2]=posY, [3]=posZ, [4]=oriX, [5]=oriY, [6]=oriZ, [7]=oriW
    Object[] rightUpperArm  = new Object[8];
    Object[] bone02  = new Object[8];
//    ArrayList<Object> bone02  = new ArrayList<Object>();


    OSCBundle bone02bundle = new OSCBundle();


    Runnable mLogRealTimeData = new Runnable() {
        @Override
        public void run() {
            // Index of first new frame in the last update
            int startingFrame = mRealTimeData.getStartingFrame();
            // Elapsed time since the last update
            long millisSinceUpdate = System.currentTimeMillis() - mUpdateStartTime;
            // Recording frequency
            float frequency = mRealTimeData.getFrequency();
            // Milliseconds per frame
            float millisPerFrame = 1000.f / frequency;
            // Select the current frame from the last update
            int currentFrame = startingFrame + (int)(millisSinceUpdate / millisPerFrame);
            // Show the last frame until a new update comes
            if (currentFrame > mRealTimeData.getFrameCount() - 1) {
                currentFrame = mRealTimeData.getFrameCount() - 1;
            }
            // Logging data for measured bones
//            Log.d("REALTIME", "Current frame:" + currentFrame);



            for (Bone b : mNotchService.getNetwork().getDevices().keySet()) {

//                System.out.println(b);
                String boneName = b.getName();


                if ( boneName.equals("Hip") ) {
                    bone02[0] = b.getName();                                  // bone name
                    bone02[1] = mRealTimeData.getPos(b,currentFrame).get(0);  // x
                    bone02[2] = mRealTimeData.getPos(b,currentFrame).get(1);  // y
                    bone02[3] = mRealTimeData.getPos(b,currentFrame).get(2);  // z
                    bone02[4] = mRealTimeData.getQ(b,currentFrame).get(0);    // x
                    bone02[5] = mRealTimeData.getQ(b,currentFrame).get(1);    // y
                    bone02[6] = mRealTimeData.getQ(b,currentFrame).get(2);    // z
                    bone02[7] = mRealTimeData.getQ(b,currentFrame).get(3);    // w
                    //  bone02[7] = mRealTimeData.getQ(b,currentFrame).get(3);    // w
//                    bone02.add(mRealTimeData.getQ(b,currentFrame).get(3));    // w


                    bone02bundle.addPacket(new OSCMessage("/notch/"+ bone02[0] +"/pos/x", Arrays.asList(bone02[1]) ));
                    bone02bundle.addPacket(new OSCMessage("/notch/"+ bone02[0] +"/pos/y", Arrays.asList(bone02[2]) ));
                    bone02bundle.addPacket(new OSCMessage("/notch/"+ bone02[0] +"/pos/z", Arrays.asList(bone02[3]) ));
                    bone02bundle.addPacket(new OSCMessage("/notch/"+ bone02[0] +"/ori/x", Arrays.asList(bone02[4]) ));
                    bone02bundle.addPacket(new OSCMessage("/notch/"+ bone02[0] +"/ori/y", Arrays.asList(bone02[5]) ));
                    bone02bundle.addPacket(new OSCMessage("/notch/"+ bone02[0] +"/ori/z", Arrays.asList(bone02[6]) ));
                    bone02bundle.addPacket(new OSCMessage("/notch/"+ bone02[0] +"/ori/w", Arrays.asList(bone02[7]) ));

                }



                if ( boneName.equals("ChestBottom") ) {
                    rightUpperArm[0] = b.getName();                                  // bone name
//                    rightUpperArm[1] = mRealTimeData.getPos(b,currentFrame).get(0);  // x
//                    rightUpperArm[2] = mRealTimeData.getPos(b,currentFrame).get(1);  // y
//                    rightUpperArm[3] = mRealTimeData.getPos(b,currentFrame).get(2);  // z
                    rightUpperArm[4] = mRealTimeData.getQ(b,currentFrame).get(0);    // w
                    rightUpperArm[5] = mRealTimeData.getQ(b,currentFrame).get(1);    // x
                    rightUpperArm[6] = mRealTimeData.getQ(b,currentFrame).get(2);    // y
                    rightUpperArm[7] = mRealTimeData.getQ(b,currentFrame).get(3);    // z

//                    OSCMessage

//                    System.out.println(rightUpperArm[4]);
                }

//                if ( boneName.equals("RightForeArm") ) {
//                    rightForeArm[0] = b.getName();                                  // bone name
//                    rightForeArm[1] = mRealTimeData.getPos(b,currentFrame).get(0);  // x
//                    rightForeArm[2] = mRealTimeData.getPos(b,currentFrame).get(1);  // y
//                    rightForeArm[3] = mRealTimeData.getPos(b,currentFrame).get(2);  // z
//                    rightForeArm[4] = mRealTimeData.getQ(b,currentFrame).get(0);    // w
//                    rightForeArm[5] = mRealTimeData.getQ(b,currentFrame).get(1);    // x
//                    rightForeArm[6] = mRealTimeData.getQ(b,currentFrame).get(2);    // y
//                    rightForeArm[7] = mRealTimeData.getQ(b,currentFrame).get(3);    // z
//                }



                /*
                // tried to be clever with it, but it didn't work
                someBone[0] = b.getName();                                  // bone name
                someBone[1] = mRealTimeData.getPos(b,currentFrame).get(0);  // x
                someBone[2] = mRealTimeData.getPos(b,currentFrame).get(1);  // y
                someBone[3] = mRealTimeData.getPos(b,currentFrame).get(2);  // z
                someBone[4] = mRealTimeData.getQ(b,currentFrame).get(0);    // w
                someBone[5] = mRealTimeData.getQ(b,currentFrame).get(1);    // x
                someBone[6] = mRealTimeData.getQ(b,currentFrame).get(2);    // y
                someBone[7] = mRealTimeData.getQ(b,currentFrame).get(3);    // z
//                bonePosX = new OSCMessage("/notch/"+ someBone[0] +"/pos/x", Arrays.asList(someBone[1]));
//                OSCMessage bonePosX = new OSCMessage("/notch/"+ someBone[0] +"/pos/x", Arrays.asList(someBone[1]));
                */


            }

            mHandler.postDelayed(mLogRealTimeData, mRefreshTime);
        }
    };

     /*  --- unused Yao code: ----
        //fvec3 a=mRealTimeData.getPos(b,currentFrame);

     */




    // Original Notch Realtime Code w/out OSC
    /*
    Cancellable c;

    @OnClick(R.id.btn_capture)
    void cptr() {
        mState = State.CAPTURE;
        mCountDown.start();
    }

    void capture() {
        mVisualiserActivity = null;
        if (mRealTime) {
            inProgress();
            c = mNotchService.capture(new NotchCallback<Void>() {
                @Override
                public void onProgress(NotchProgress progress) {
                    if (progress.getState() == NotchProgress.State.REALTIME_UPDATE) {
                        mRealTimeData = (VisualiserData) progress.getObject();
                        updateRealTime();
                    }
                }

                @Override
                public void onSuccess(Void nothing) {
                    clearText();
                }

                @Override
                public void onFailure(NotchError notchError) {
                    Util.showNotification(Util.getNotchErrorStr(notchError));
                    clearText();
                }

                @Override
                public void onCancelled() {
                    Util.showNotification("Real-time measurement stopped!");
                    clearText();
                }
            });
        }
        else {
            if (mRemote) {
                mNotchService.remoteCapture(new EmptyCallback<Measurement>() {
                    @Override
                    public void onSuccess(Measurement measurement) {
                        mCurrentMeasurement = measurement;
                        getNewOutput();
                        File newOutput = new File(mCurrentOutput.getParentFile(), mCurrentOutput.getName().replace(".zip",".notchx"));
                        saveForPostDownload(mCurrentMeasurement, newOutput);
                        clearText();
                    }
                });
            } else {
                mNotchService.timedCapture(new EmptyCallback<Measurement>() {
                    @Override
                    public void onSuccess(Measurement measurement) {
                        mCurrentMeasurement = measurement;
                        Util.showNotification("Capture finished");
                        clearText();
                    }
                });
            }
        }
    }
    */



    @OnClick(R.id.btn_download)
    void download() {
        if (mRealTime) {
            if (c != null) {
                c.cancel();
                c = null;
            }
        }
        else {
            getNewOutput();
            inProgress();
            if (mCurrentMeasurement== null) {
                Util.showNotification("No measurement to download!");
                return;
            }
            mNotchService.download(mCurrentOutput, mCurrentMeasurement, new EmptyCallback<Measurement>() {
                @Override
                public void onSuccess(Measurement measurement) {
                    Map<String, List<Bone>> status = measurement.getStatus();
                    if (status.keySet().size() > 0) {
                        Util.showNotification(Util.getMeasurementStatusString(status));
                    }
                    Util.showNotification("Download finished!");
                    clearText();
                }

                @Override
                public void onFailure(NotchError notchError) {
                    super.onFailure(notchError);
                    File newOutput = new File(mCurrentOutput.getParentFile(), mCurrentOutput.getName().replace(".zip",".notchx"));
                    saveForPostDownload(mCurrentMeasurement, newOutput);
                }
            });
        }
    }

    void getNewOutput() {
        if (!mOutputDir.isDirectory() && !mOutputDir.mkdirs()) {
            Util.showNotification("Failed to create output directory!");
        }
        String currentDateAndTime = mSDF.format(new Date());
        mCurrentOutput = new File(mOutputDir, "meas_" + currentDateAndTime + ".zip");
    }

    @OnClick(R.id.btn_post_download)
    void showNotDownloadedFiles() {
        File[] files = mOutputDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".notchx");
            }
        });

        if (files.length > 0) {
            buildFileDialog(files);
            fileDialog.show();
        } else {
            Util.showNotification("There is no measurement saved for post-download!");
        }
    }

    void saveForPostDownload(Measurement meas, File out) {
        try {
            FileOutputStream fos = new FileOutputStream(out);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(meas);
            os.close();
            fos.close();
            Util.showNotification("Measurement is saved for post-download to: " + out.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void postDownload(File file) {
        if (!file.getName().endsWith(".notchx")) {
            Util.showNotification("Invalid file!");
            return;
        }

        final File fileToDownload = file;
        if (mNotchService.getNetwork() == null) {
            mNotchService.uncheckedInit(mSelectedChannel, new EmptyCallback<NotchNetwork>() {
                @Override
                public void onSuccess(NotchNetwork notchNetwork) {
                    super.onSuccess(notchNetwork);
                    postDownload(fileToDownload);
                }
            });
            inProgress();
        } else {
            if (!mOutputDir.isDirectory() && !mOutputDir.mkdirs()) {
                Util.showNotification("Failed to create output directory!");
            }

            ObjectInputStream in;
            // Open measurement
            try {
                InputStream stream = new FileInputStream(fileToDownload);
                in = new ObjectInputStream(stream);
                mCurrentMeasurement = (Measurement) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                Util.showNotification("Not supported measurement format!");
                return;
            } catch (IllegalArgumentException e) {
                Util.showNotification("Not supported measurement format!");
                return;
            }

            mCurrentOutput = new File(fileToDownload.getParentFile(), fileToDownload.getName().replace(".notchx",".zip"));
            inProgress();
            mNotchService.download(mCurrentOutput, mCurrentMeasurement, new EmptyCallback<Measurement>() {
                @Override
                public void onSuccess(Measurement measurement) {
                    Map<String, List<Bone>> status = measurement.getStatus();
                    if (status.keySet().size() > 0) {
                        Util.showNotification(Util.getMeasurementStatusString(status));
                    }
                    fileToDownload.delete();
                    Util.showNotification("Download finished!");
                    clearText();
                }
            });

        }
    }

    @OnClick(R.id.btn_visualize)
    void vis() {
        visualise();
    }

    @OnClick(R.id.btn_show_example)
    void showExample() {
        File outputDir = Environment.getExternalStoragePublicDirectory(NOTCH_DIR);
        File exampleFile = new File(outputDir, "hello_notches.notch");
        mVisualiserActivity = new com.wearnotch.notchdemo.visualiser.VisualiserActivity();
        Intent i = VisualiserActivity.createIntent(getActivity(), Uri.fromFile(exampleFile));
        startActivity(i);
    }

    private void buildUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMessage("Enter user license!");

        // Set up the input
        final EditText input = new EditText(this.mApplicationContext);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(getResources().getColor(R.color.black));
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String user;
                user = input.getText().toString();

                mNotchService.setLicense(user);
                updateUser(mNotchService.getLicense());
                mUser = user;
            }
        });

        userDialog = builder.create();
    }

    private void buildFileDialog(final File[] files) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle("Choose a measurement to download!");
        String[] names = new String[files.length];
        int initial = 0;
        mCurrentOutput = files[initial];

        for (int i=0;i<files.length;i++) {
            names[i] = files[i].getName();
        }

        builder.setSingleChoiceItems(names, initial, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCurrentOutput = files[which];
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                postDownload(mCurrentOutput);
            }
        });

        fileDialog = builder.create();

    }

    private void buildChannelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle("Choose a channel!");
        final String[] names = new String[] {"UNSPECIFIED","A","B","C","D","E","F","G","H","I",
                "J","K","L","M","N","O","P","Q","R","S","U","V","W","X","Y","Z",};

        int initial = 0;
        mSelectedChannel = null;

        builder.setSingleChoiceItems(names, initial, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mSelectedChannel = null;
                } else {
                    mSelectedChannel = NotchChannel.fromChar(names[which].charAt(0));
                }
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mChangingChannel) {
                    inProgress();
                    if (mSelectedChannel != null) {
                        mNotchService.changeChannel(mSelectedChannel, new EmptyCallback<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                super.onSuccess(aVoid);
                                mSelectedChannel = null;
                                updateUser(mNotchService.getLicense());
                            }

                            @Override
                            public void onFailure(NotchError notchError) {
                                super.onFailure(notchError);
                                mSelectedChannel = null;
                            }
                        });
                    } else {
                        Util.showNotification("Select a valid channel (A-Z)!");
                        clearText();
                    }

                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mSelectedChannel == null) {
                                mSelectedChannelTxt.setText("SELECTED CHANNEL: UNSPECIFIED");
                            } else {
                                mSelectedChannelTxt.setText("SELECTED CHANNEL: " + mSelectedChannel.toChar());
                            }
                        }
                    });
                }
            }
        });

        channelDialog = builder.create();

    }

    private void checkAndCopyExample() throws IOException {
        File outputDir = Environment.getExternalStoragePublicDirectory(NOTCH_DIR);
        File exampleFile = new File(outputDir, "hello_notches.notch");

        if (!exampleFile.exists()) {

            if (!outputDir.isDirectory()) {
                boolean isDir = outputDir.mkdirs();
                if (!isDir) {
                    throw new IOException("Couldn't create output directory");
                }
            }

            InputStream in = getResources().openRawResource(R.raw.hello_3notches);
            FileOutputStream out = new FileOutputStream(exampleFile);
            Util.copyFile(in, out);
        }
    }


    private void updateUser(final String user) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNewTitle.setText("CURRENT LICENSE:\n" + user);
                if (mDB == null) mDB = NotchDataBase.getInst();
                StringBuilder sb = new StringBuilder();
                sb.append("Device list:\n");
                for (Device device : mDB.findAllDevices(user)) {
                    sb.append("Notch ").append(device.getNotchDevice().getNetworkId()).append(" (");
                    sb.append(device.getNotchDevice().getDeviceMac()).append(") ");
                    sb.append("FW: " + device.getSwVersion() + ", ");
                    sb.append("Ch: " + device.getChannel().toChar() + "\n");
                }
                mDeviceList.setText(sb.toString());
                if (mSelectedChannel == null) {
                    mSelectedChannelTxt.setText("SELECTED CHANNEL: UNSPECIFIED");
                } else {
                    mSelectedChannelTxt.setText("SELECTED CHANNEL: " + mSelectedChannel.toChar());
                }
            }
        });
    }

    private void updateNetwork() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("Current network:\n");
                if (mNotchService.getNetwork() != null) {
                    for (ActionDevice device : mNotchService.getNetwork().getDeviceSet()) {
                        sb.append(device.getNetworkId()).append(", ");
                    }
                }
                mCurrentNetwork.setText(sb.toString());
            }
        });
    }

    private void visualise() {
        if (mCurrentMeasurement != null) {
            mVisualiserActivity = new com.wearnotch.notchdemo.visualiser.VisualiserActivity();
            Intent i = VisualiserActivity.createIntent(getActivity(), Uri.fromFile(mCurrentOutput));
            startActivity(i);
        }
        else {
            Util.showNotification("No measurement to visualise!");
        }
    }

    private void updateRealTime() {
        if (mVisualiserActivity == null) {
            mVisualiserActivity = new VisualiserActivity();
            Intent i = VisualiserActivity.createIntent(getActivity(), mRealTimeData, mRealTime);
            startActivity(i);
        }
        else {
            EventBus.getDefault().post(mRealTimeData);
        }
    }

    private CountDownTimer mCountDown = new CountDownTimer(3250, 500) {
        public void onTick(long millisUntilFinished) {
            //update the UI with the new count
            setCounterText(mCounterText,millisUntilFinished);
        }

        public void onFinish() {
            //start the activity
            switch (mState) {
                case CALIBRATION:
                    mNotchService.calibration(new EmptyCallback<Measurement>());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mDockImg.setVisibility(View.VISIBLE);
                            mDockAnimation.setVisible(false, true);
                            mDockAnimation.start();
                        }
                    });

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mDockImg.setVisibility(View.GONE);
                            mDockAnimation.stop();
                        }
                    },CALIBRATION_TIME);
                    break;

                case STEADY:
                    mNotchService.steady(new EmptyCallback<Measurement>());
                    break;

                case CAPTURE:
                    capture();
                    break;
            }

            setCounterText(mCounterText,"");
        }
    };

    private void setCounterText(final TextView text, final long millisec){
        if(isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 120);
                    text.setText("" + Math.round((float) millisec / 1000.0f));
                }
            });
        }
    }

    private void setCounterText(final TextView text, final String str, final float size){
        if(isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
                    text.setText(str);
                }
            });
        }
    }

    private void setCounterText(final TextView text, final String str){
        setCounterText(text,str,50);
    }

    private void clearText() {
        setCounterText(mCounterText, "");
    }

    private void inProgress() {
        setCounterText(mCounterText,"In progress...");

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

    // Permission check
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ALL_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Util.showNotification("Permissions granted!");
                } else {
                    requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION);
                    Util.showNotification("Permissions must be granted");
                }
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }



    /* ---- OSC SECTION ----
     * These two variables hold the IP address and port number.
     * You should change them to the appropriate address and port.
     */
    private String myIP = "172.16.44.155"; // the IP of the computer sending OSC to...
    private int myPort = 8000;
    public OSCPortOut oscPortOut;  // This is used to send messages
    private int OSCdelay = 40; // interval for sending OSC data

    // This thread will contain all the code that pertains to OSC
    private Thread oscThread = new Thread() {
        @Override
        public void run() {
            /* The first part of the run() method initializes the OSCPortOut for sending messages.
             *
             * For more advanced apps, where you want to change the address during runtime, you will want
             * to have this section in a different thread, but since we won't be changing addresses here,
             * we only have to initialize the address once.
             */

            try {
                // Connect to some IP address and port
                oscPortOut = new OSCPortOut(InetAddress.getByName(myIP), myPort);
            } catch(UnknownHostException e) {
                // Error handling when your IP isn't found
                return;
            } catch(Exception e) {
                // Error handling for any other errors
                return;
            }

            // TODO make this toggle-able w OSC checkbox
            while (true) {
                if (oscPortOut != null) {

                    // left hand positions [X, Y, Z]
//                    OSCMessage rightUpperArmPosX = new OSCMessage("/notch/"+ rightUpperArm[0] +"/pos/x", Arrays.asList(rightUpperArm[1]));
//                    OSCMessage rightUpperArmPosY = new OSCMessage("/notch/"+ rightUpperArm[0] +"/pos/y", Arrays.asList(rightUpperArm[2]));
//                    OSCMessage rightUpperArmPosZ = new OSCMessage("/notch/"+ rightUpperArm[0] +"/pos/z", Arrays.asList(rightUpperArm[3]));
//                     left hand orientations [W, X, Y, Z] (quaternion)


                    // temp comment out
                    OSCMessage rightUpperArmOriX = new OSCMessage("/notch/"+ rightUpperArm[0] +"/ori/x", Arrays.asList(rightUpperArm[4]));
                    OSCMessage rightUpperArmOriY = new OSCMessage("/notch/"+ rightUpperArm[0] +"/ori/y", Arrays.asList(rightUpperArm[5]));
                    OSCMessage rightUpperArmOriZ = new OSCMessage("/notch/"+ rightUpperArm[0] +"/ori/z", Arrays.asList(rightUpperArm[6]));
                    OSCMessage rightUpperArmOriW = new OSCMessage("/notch/"+ rightUpperArm[0] +"/ori/w", Arrays.asList(rightUpperArm[7]));



                    /*
                    // right hand positions [X, Y, Z]
                    OSCMessage rightForeArmPosX = new OSCMessage("/notch/"+ rightForeArm[0] +"/pos/x", Arrays.asList(rightForeArm[1]));
                    OSCMessage rightForeArmPosY = new OSCMessage("/notch/"+ rightForeArm[0] +"/pos/y", Arrays.asList(rightForeArm[2]));
                    OSCMessage rightForeArmPosZ = new OSCMessage("/notch/"+ rightForeArm[0] +"/pos/z", Arrays.asList(rightForeArm[3]));
                    // right hand orientations [W, X, Y, Z] (quaternion)
                    OSCMessage rightForeArmOriW = new OSCMessage("/notch/"+ rightForeArm[0] +"/ori/w", Arrays.asList(rightForeArm[4]));
                    OSCMessage rightForeArmOriX = new OSCMessage("/notch/"+ rightForeArm[0] +"/ori/x", Arrays.asList(rightForeArm[5]));
                    OSCMessage rightForeArmOriY = new OSCMessage("/notch/"+ rightForeArm[0] +"/ori/y", Arrays.asList(rightForeArm[6]));
                    OSCMessage rightForeArmOriZ = new OSCMessage("/notch/"+ rightForeArm[0] +"/ori/z", Arrays.asList(rightForeArm[7]));
                    */

                    try {
                        // Send the messages


                        OSCBundle bundle = new OSCBundle();
                        bundle.addPacket(rightUpperArmOriX);
                        bundle.addPacket(rightUpperArmOriY);
                        bundle.addPacket(rightUpperArmOriZ);
                        bundle.addPacket(rightUpperArmOriW);
//                        OSCMessage test = new OSCMessage("/notch", Arrays.asList(rightUpperArm[7]));
//                        bundle.addPacket(test);
                        oscPortOut.send(bundle); // WORKS !



                        oscPortOut.send(bone02bundle); // WORKS !



                        sleep(OSCdelay); // pause so it's not sending LOADS of OSC

                    } catch (Exception e) {
                        // Error handling for some error
                    }


                    // old example code unused
                     /*
                    ArrayList<Object> moreThingsToSend = new ArrayList<Object>();
                    moreThingsToSend.add("Hello World2");
                    moreThingsToSend.add(123456);
                    moreThingsToSend.add(12.345);
                    moreThingsToSend.add(12.345);

                    OSCMessage message2 = new OSCMessage("/test", moreThingsToSend);
                    //OSCMessage message2 = new OSCMessage(myIP, moreThingsToSend.toArray()); // downloadable version of API
                    */
                }
            }
        }
    };


}
