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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import android.graphics.Color;

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
import com.wearnotch.notchmaths.fvec3;
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

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

// OSC imports
import java.net.*;
import java.util.*;
import com.illposed.osc.*;

// websocket imports
import java.io.IOException;
import java.net.InetSocketAddress;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import java.util.Locale;

public class MainFragment extends BaseFragment {
    private static final String TAG = MainFragment.class.getSimpleName();
    private static final String DEFAULT_USER_LICENSE = "ZvqYLovXeNGREMadVnRE";

    private static final String NOTCH_DIR = "notch_tutorial";
    private static final long CALIBRATION_TIME = 7000L;
    private static final long CALIBRATION_SEQ_TIME = 7500L;
    private static final long STEADY_TIME_DELAY = 5000L;
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
    private boolean mRealTime, mRemote, mChangingChannel;
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

    volatile boolean running = true;

    // websocket server vars
    TextView infoip, msg;
    WebsocketServer wsServer;

    boolean streamSequence1 = false;
    boolean streamSequence2 = false;
    boolean calibSequence1 = false;
    boolean calibSequence2 = false;
    boolean steadySequence1 = false;
    boolean steadySequence2 = false;
    boolean streamActionStart = true;




    @BindView(R.id.new_title)
    TextView mNewTitle;

    @BindView(R.id.current_IP)
    TextView mCurrentIP;

    @BindView(R.id.device_list)
    TextView mDeviceList;

    @BindView(R.id.device_management_txt)
    TextView mDeviceManagementTxt;

    @BindView(R.id.selected_channel_txt)
    TextView mSelectedChannelTxt;

    @BindView(R.id.calibration_txt)
    TextView mCalibrationTxt;

    @BindView(R.id.steady_txt)
    TextView mSteadyTxt;

    @BindView(R.id.capture_txt)
    TextView mCaptureTxt;

    @BindView(R.id.current_network)
    TextView mCurrentNetwork;

    @BindView(R.id.chk_realtime)
    CheckBox mRealTimeBox;

    @BindView(R.id.chk_remote)
    CheckBox mRemoteBox;

    // Buttons
    @BindView(R.id.btn_set_user)
    Button mButtonSetUser;

    @BindView(R.id.btn_set_IP)
    Button mButtonSetIP;

    @BindView(R.id.btn_pair)
    Button mButtonPair;

    @BindView(R.id.btn_sync_pair)
    Button mButtonSyncPair;

    @BindView(R.id.btn_remove)
    Button mButtonRemove;

    @BindView(R.id.btn_connect)
    Button mButtonConnect;

    @BindView(R.id.btn_disconnect)
    Button mButtonDisconnect;

    @BindView(R.id.btn_shutdown)
    Button mButtonShutDown;

    @BindView(R.id.btn_erase)
    Button mButtonErase;

    @BindView(R.id.btn_change_channel)
    Button mButtonChangeChannel;

    @BindView(R.id.btn_unchecked_init)
    Button mButtonUncheckedInit;

    @BindView(R.id.btn_configure_calib)
    Button mButtonConfigureCalib;

    @BindView(R.id.btn_calibrate)
    Button mButtonCalibrate;

    @BindView(R.id.btn_get_calibration)
    Button mButtonGetCalibData;

    @BindView(R.id.btn_init_3_steady)
    Button mButtonInitSteady;

    @BindView(R.id.btn_configure_steady)
    Button mButtonConfigureSteady;

    @BindView(R.id.btn_steady)
    Button mButtonSteady;

    @BindView(R.id.btn_get_steady)
    Button mButtonGetSteadyData;

    @BindView(R.id.btn_init_3_capture)
    Button mButtonInitCapture;

    @BindView(R.id.btn_configure_capture)
    Button mButtonConfigure;

    @BindView(R.id.btn_capture)
    Button mButtonCapture;

    @BindView(R.id.btn_download)
    Button mButtonDownload;

    @BindView(R.id.btn_post_download)
    Button mButtonPostDownload;

    @BindView(R.id.btn_visualize)
    Button mButtonVisualize;

    @BindView(R.id.btn_show_example)
    Button mButtonShowExample;

    @BindView(R.id.btn_stop_osc)
    Button mButtonStopOSC;

    @BindView(R.id.btn_start_osc)
    Button mButtonStartOSC;

    @BindView(R.id.btn_stream_action)
    Button mButtonStreamAction;

    @BindView(R.id.counter_text)
    TextView mCounterText;

    @BindView(R.id.dock_image)
    ImageView mDockImg;

    AlertDialog userDialog, fileDialog, channelDialog, userIPDialog;


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
//        oscThread.start();
        new OSCThread().start();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, root);

        // Set typefaces
//        Typeface tfLight = Typeface.createFromAsset(mApplicationContext.getAssets(), "fonts/Lato-Light.ttf");
//        Typeface tfBold = Typeface.createFromAsset(mApplicationContext.getAssets(), "fonts/Lato-Bold.ttf");

//        mNewTitle.setTypeface(tfBold);
//        mCurrentIP.setTypeface(tfBold);
//        mDeviceManagementTxt.setTypeface(tfBold);
//        mSelectedChannelTxt.setTypeface(tfLight);
//        mCalibrationTxt.setTypeface(tfBold);
//        mSteadyTxt.setTypeface(tfBold);
//        mCaptureTxt.setTypeface(tfBold);
//
//        mCurrentNetwork.setTypeface(tfLight);
//        mDeviceList.setTypeface(tfLight);
//
//        // Set button typeface
//        mButtonSetUser.setTypeface(tfLight);
//        mButtonPair.setTypeface(tfLight);
//        mButtonSyncPair.setTypeface(tfLight);
//        mButtonRemove.setTypeface(tfLight);
//        mButtonShutDown.setTypeface(tfLight);
////        mButtonConnect.setTypeface(tfLight);
//        mButtonDisconnect.setTypeface(tfLight);
//        mButtonErase.setTypeface(tfLight);
//        mButtonChangeChannel.setTypeface(tfLight);
//        mButtonUncheckedInit.setTypeface(tfLight);
//        mButtonConfigureCalib.setTypeface(tfLight);
//        mButtonCalibrate.setTypeface(tfLight);
//        mButtonGetCalibData.setTypeface(tfLight);
//        mButtonInitSteady.setTypeface(tfLight);
//        mButtonConfigureSteady.setTypeface(tfLight);
//        mButtonSteady.setTypeface(tfLight);
//        mButtonGetSteadyData.setTypeface(tfLight);
//        mButtonInitCapture.setTypeface(tfLight);
//        mButtonConfigure.setTypeface(tfLight);
//        mButtonCapture.setTypeface(tfLight);
//        mButtonDownload.setTypeface(tfLight);
//        mButtonPostDownload.setTypeface(tfLight);
//        mButtonVisualize.setTypeface(tfLight);
//        mButtonShowExample.setTypeface(tfLight);
//
//        // OSC
//        mButtonStopOSC.setTypeface(tfLight);
//        mButtonStartOSC.setTypeface(tfLight);
//        mButtonSetIP.setTypeface(tfLight);
//
//        mCounterText.setTypeface(tfLight);
//        mRealTimeBox.setTypeface(tfLight);
//        mRemoteBox.setTypeface(tfLight);


        // Animation
        mDockImg.setBackgroundResource(R.drawable.sensor_anim);
        mDockAnimation = (AnimationDrawable) mDockImg.getBackground();
        mDockImg.setVisibility(View.INVISIBLE);

        // Other
        mSDF = new SimpleDateFormat("yyyyMMdd_HHmmss");

        buildUserDialog();
        buildUserIPDialog();
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
            setActionBarTitle(R.string.app_name_long);
            if (mNotchService != null && mUser == null) {
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
        setActionBarTitle(R.string.app_name_long);
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

    @OnClick(R.id.btn_set_user)
    void setUser() {
        userDialog.show();
    }

    @OnClick(R.id.btn_set_IP)
    void setIP() {
        System.out.println("setting IP");
        userIPDialog.show();
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
        System.out.println("unchecking and initing");
        inProgress();
        mNotchService.uncheckedInit(mSelectedChannel, new EmptyCallback<NotchNetwork>() {
            @Override
            public void onSuccess(NotchNetwork notchNetwork) {
                updateNetwork();
                super.onSuccess(notchNetwork);

                if (calibSequence1) {
                    configureCalib();
                }
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
                workout = Workout.from("Demo_config", skeleton, IOUtil.readAll(new InputStreamReader(mApplicationContext.getResources().openRawResource(R.raw.config_5_full_body))));
                workout = workout.withMeasurementType(MeasurementType.STEADY_SKIP);
            }
            mWorkout = workout;
            inProgress();
            mNotchService.init(mSelectedChannel, workout, new EmptyCallback<NotchNetwork>() {
                @Override
                public void onSuccess(NotchNetwork notchNetwork) {
                    updateNetwork();
                    super.onSuccess(notchNetwork);

                    if (streamSequence1) {
                        configureCapture();
                    }
                    if (steadySequence1) {
                        configureSteady();
                    }
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

                        mSkeleton = mRealTimeData.getSkeleton();

                        mRealTime = true; // needed here now if not visualizing
                        updateRealTime(); // for visualizing
                        mUpdateStartTime = System.currentTimeMillis();
                        mHandler.removeCallbacks(mLogRealTimeData);
                        mHandler.post(mLogRealTimeData); // start it
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



    // Relevant Motion - REALTIME SECTION
    long mUpdateStartTime;
    long mRefreshTime = 20;
    int scale = 100;

    // Objects
    Object[] chestObj           = new Object[7];
    Object[] rightUpperArmObj   = new Object[7];
    Object[] rightForeArmObj    = new Object[7];
    Object[] leftUpperArmObj    = new Object[7];
    Object[] leftForeArmObj     = new Object[7];
//    Object[] objects = new Object[]{ chestObj, rightUpperArmObj, rightForeArmObj, leftUpperArmObj, leftForeArmObj };

    // make some fvec3
    fvec3 chestAngle            = new fvec3();
    fvec3 rightShoulderAngle    = new fvec3();
    fvec3 rightElbowAngle       = new fvec3();
    fvec3 leftShoulderAngle     = new fvec3();
    fvec3 leftElbowAngle        = new fvec3();

    Skeleton mSkeleton;
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

            // TODO put this in a conditional statement or try/catch
            wsServer.broadcast( "chest angle: [" + chestAngle.get(0) + "]"); // This method sends a message to all clients connected

//            System.out.println(chestAngle.get(0));

            // get some bones
            Bone root           = mSkeleton.getRoot(); // a helpful method
            Bone hip            = mSkeleton.getBone("Hip");
            Bone chestBottom    = mSkeleton.getBone("ChestBottom");
            Bone rightCollar    = mSkeleton.getBone("RightCollar");
            Bone rightUpperArm  = mSkeleton.getBone("RightUpperArm");
            Bone rightForeArm   = mSkeleton.getBone("RightForeArm");
            Bone leftCollar     = mSkeleton.getBone("LeftCollar");
            Bone leftUpperArm   = mSkeleton.getBone("LeftUpperArm");
            Bone leftForeArm    = mSkeleton.getBone("LeftForeArm");
            Bone RightLowerLeg  = mSkeleton.getBone("RightLowerLeg");
            Bone LeftLowerLeg   = mSkeleton.getBone("LeftLowerLeg");


            // Usage: calculateRelativeAngle(Bone child, Bone parent, int frameIndex, fvec3 output)
            // Hack:  frameIndex = 0 gives current realtime frames
            mRealTimeData.calculateRelativeAngle(hip, chestBottom, 0, chestAngle);
            chestObj[0] = chestBottom.getName();
            chestObj[1] = scale * mRealTimeData.getPos( chestBottom, currentFrame).get(0);  // x
            chestObj[2] = scale * mRealTimeData.getPos( chestBottom, currentFrame).get(1);  // y
            chestObj[3] = scale * mRealTimeData.getPos( chestBottom, currentFrame).get(2);  // z
            chestObj[4] = chestAngle.get(0);  // x
            chestObj[5] = chestAngle.get(1);  // y
            chestObj[6] = chestAngle.get(2);  // z

            mRealTimeData.calculateRelativeAngle(rightCollar, rightUpperArm, 0, rightShoulderAngle);
            rightUpperArmObj[0] = rightUpperArm.getName();
            rightUpperArmObj[1] = scale * mRealTimeData.getPos( rightUpperArm, currentFrame).get(0);  // x
            rightUpperArmObj[2] = scale * mRealTimeData.getPos( rightUpperArm, currentFrame).get(1);  // y
            rightUpperArmObj[3] = scale * mRealTimeData.getPos( rightUpperArm, currentFrame).get(2);  // z
            rightUpperArmObj[4] = rightShoulderAngle.get(0);  // x
            rightUpperArmObj[5] = rightShoulderAngle.get(1);  // y
            rightUpperArmObj[6] = rightShoulderAngle.get(2);  // z

            mRealTimeData.calculateRelativeAngle(rightUpperArm, rightForeArm, 0, rightElbowAngle);
            rightForeArmObj[0] = rightForeArm.getName();
            rightForeArmObj[1] = scale * mRealTimeData.getPos( rightForeArm, currentFrame).get(0);  // x
            rightForeArmObj[2] = scale * mRealTimeData.getPos( rightForeArm, currentFrame).get(1);  // y
            rightForeArmObj[3] = scale * mRealTimeData.getPos( rightForeArm, currentFrame).get(2);  // z
            rightForeArmObj[4] = rightElbowAngle.get(0);  // x
            rightForeArmObj[5] = rightElbowAngle.get(1);  // y
            rightForeArmObj[6] = rightElbowAngle.get(2);  // z

            mRealTimeData.calculateRelativeAngle(leftCollar, leftUpperArm, 0, leftShoulderAngle);
            leftUpperArmObj[0] = leftUpperArm.getName();
            leftUpperArmObj[1] = scale * mRealTimeData.getPos( leftUpperArm, currentFrame).get(0);  // x
            leftUpperArmObj[2] = scale * mRealTimeData.getPos( leftUpperArm, currentFrame).get(1);  // y
            leftUpperArmObj[3] = scale * mRealTimeData.getPos( leftUpperArm, currentFrame).get(2);  // z
            leftUpperArmObj[4] = leftShoulderAngle.get(0);  // x
            leftUpperArmObj[5] = leftShoulderAngle.get(1);  // y
            leftUpperArmObj[6] = leftShoulderAngle.get(2);  // z

            mRealTimeData.calculateRelativeAngle(leftUpperArm, leftForeArm, 0, leftElbowAngle);
            leftForeArmObj[0] = leftForeArm.getName();
            leftForeArmObj[1] = scale * mRealTimeData.getPos( leftForeArm, currentFrame).get(0);  // x
            leftForeArmObj[2] = scale * mRealTimeData.getPos( leftForeArm, currentFrame).get(1);  // y
            leftForeArmObj[3] = scale * mRealTimeData.getPos( leftForeArm, currentFrame).get(2);  // z
            leftForeArmObj[4] = leftElbowAngle.get(0);  // x
            leftForeArmObj[5] = leftElbowAngle.get(1);  // y
            leftForeArmObj[6] = leftElbowAngle.get(2);  // z

            mHandler.postDelayed(mLogRealTimeData, mRefreshTime);
        }
    };

//    void capture() {
//        mVisualiserActivity = null;
//        if (mRealTime) {
//            inProgress();
//            c = mNotchService.capture(new NotchCallback<Void>() {
//                @Override
//                public void onProgress(NotchProgress progress) {
//                    if (progress.getState() == NotchProgress.State.REALTIME_UPDATE) {
//                        mRealTimeData = (VisualiserData) progress.getObject();
//                        updateRealTime();
//                    }
//                }
//
//                @Override
//                public void onSuccess(Void nothing) {
//                    clearText();
//                }
//
//                @Override
//                public void onFailure(NotchError notchError) {
//                    Util.showNotification(Util.getNotchErrorStr(notchError));
//                    clearText();
//                }
//
//                @Override
//                public void onCancelled() {
//                    Util.showNotification("Real-time measurement stopped!");
//                    clearText();
//                }
//            });
//        }
//        else {
//            if (mRemote) {
//                mNotchService.remoteCapture(new EmptyCallback<Measurement>() {
//                    @Override
//                    public void onSuccess(Measurement measurement) {
//                        mCurrentMeasurement = measurement;
//                        getNewOutput();
//                        File newOutput = new File(mCurrentOutput.getParentFile(), mCurrentOutput.getName().replace(".zip",".notchx"));
//                        saveForPostDownload(mCurrentMeasurement, newOutput);
//                        clearText();
//                    }
//                });
//            } else {
//                mNotchService.timedCapture(new EmptyCallback<Measurement>() {
//                    @Override
//                    public void onSuccess(Measurement measurement) {
//                        mCurrentMeasurement = measurement;
//                        Util.showNotification("Capture finished");
//                        clearText();
//                    }
//                });
//            }
//        }
//    }

    @OnClick(R.id.btn_download)
    void download() {
        if (mRealTime) {
            if (c != null) {
                c.cancel();
                c = null;
            }
            resetSequences();
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

    @OnClick(R.id.btn_stop_osc)
    void stopOSC() {
        running = false;
    }

    @OnClick(R.id.btn_start_osc)
    void startOSC() {
        running = true;

        new OSCThread().start();
    }

//    /Users/Spencer/.gradle/caches/modules-2/files-2.1/com.notch/framework/1.1.54/3deb683507b15c4aeee10e6f7c00d5e744c68a67/framework-1.1.54.jar!/com/wearnotch/framework/Workout.class
    // *** SEQUENCES ***
    @OnClick(R.id.btn_init_config_calib)
    void InitAndConfigCalibSequence() {
        resetSequences();
        calibSequence1 = true;
        uncheckedinit();
    }

    @OnClick(R.id.btn_start_get_calibrate)
    void StartAndGetCalibSequence() {
        resetSequences();
        calibSequence2 = true;
        calibrate();
    }

    @OnClick(R.id.btn_init_config_steady)
    void InitAndConfigSteadySequence() {
        resetSequences();
        steadySequence1 = true;
        initSteady();
    }

    @OnClick(R.id.btn_start_get_steady)
    void StartAndGetSteadySequence() {
        resetSequences();
        steadySequence2 = true;
        steady();
    }

    @OnClick(R.id.btn_init_config_stream)
    void InitAndConfigstreamSequence1() {
        resetSequences();
        streamSequence1 = true;
        initSteady();
    }

    @OnClick(R.id.btn_stream_action)
    void StartAndGetstreamSequence1() {
        resetSequences();
        System.out.println("streamActionStart: " + streamActionStart);

        if (streamActionStart) {
            streamSequence2 = true;
            cptr();

            streamActionStart = false;
            mButtonStreamAction.setBackgroundColor(Color.parseColor("#FF0000"));
            mButtonStreamAction.setText("Stop Stream");
        }
        else if (!streamActionStart) {
            download();

            streamActionStart = true;
            mButtonStreamAction.setBackgroundColor(Color.parseColor("#2CB978"));
            mButtonStreamAction.setText("Start Stream");
        }
    }



    @OnClick(R.id.btn_start_ws)
    void startWSButton() {
        startWebSocket();
    }

    @OnClick(R.id.btn_stop_ws)
    void stopWSButton() {
        stopWebSocket();
    }

    public void resetSequences() {
        streamSequence1 = false;
        calibSequence1 = false;
        calibSequence2 = false;

        steadySequence1 = false;
    }

    public void startWebSocket()
    {
        System.out.println("starting websocket server...");

        // get the current IP address (to start server with)
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(mApplicationContext.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int IPbits = wifiInfo.getIpAddress();
        String ipAddress = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (IPbits & 0xff), (IPbits >> 8 & 0xff),
                (IPbits >> 16 & 0xff), (IPbits >> 24 & 0xff));


        Context context = getActivity().getApplicationContext();

        // websocket server
        InetSocketAddress inetSockAddress = new InetSocketAddress(ipAddress, 8887);
        wsServer = new WebsocketServer(inetSockAddress, context, mActivity);
        wsServer.start();
    }

    public void stopWebSocket()
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

    private void buildUserIPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMessage("Enter IP");

        // Set up the input
        final EditText input = new EditText(this.mApplicationContext);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(getResources().getColor(R.color.black));
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String IP;
                IP = input.getText().toString();

                myIP = IP;
                updateUserIP(myIP);

//                mNotchService.setLicense(user);
//                updateUser(mNotchService.getLicense());
//                mUser = user;
            }
        });

        userIPDialog = builder.create();
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

    private void updateUserIP(final String IP) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCurrentIP.setText("IP:\n" + IP);
//                if (mDB == null) mDB = NotchDataBase.getInst();
//                StringBuilder sb = new StringBuilder();
//                sb.append("Device list:\n");
//                for (Device device : mDB.findAllDevices(user)) {
//                    sb.append("Notch ").append(device.getNotchDevice().getNetworkId()).append(" (");
//                    sb.append(device.getNotchDevice().getDeviceMac()).append(") ");
//                    sb.append("FW: " + device.getSwVersion() + ", ");
//                    sb.append("Ch: " + device.getChannel().toChar() + "\n");
//                }
//                mDeviceList.setText(sb.toString());
//                if (mSelectedChannel == null) {
//                    mSelectedChannelTxt.setText("SELECTED CHANNEL: UNSPECIFIED");
//                } else {
//                    mSelectedChannelTxt.setText("SELECTED CHANNEL: " + mSelectedChannel.toChar());
//                }
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

//                            if (calibSequence2) {
//                                getCalibData();
//                            }
                        }
                    },CALIBRATION_TIME);

                    // a little more of a delay for the next sequence function
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            if (calibSequence2) {
                                getCalibData();
                            }
                        }
                    },CALIBRATION_SEQ_TIME);

                    break;

                case STEADY:
                    mNotchService.steady(new EmptyCallback<Measurement>());

                    // for the sequence
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (steadySequence2) {
                                getSteadyData();
                            }
                        }
                    }, STEADY_TIME_DELAY);

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
    private String myIP  = "127.0.0.1"; // the IP of the computer sending OSC to...
    private int myPort = 8000;
    public OSCPortOut oscPortOut;  // This is used to send messages
    private int OSCdelay = 40; // interval for sending OSC data


    private class OSCThread extends Thread {

        @Override
        public void run() {
            /* The first part of the run() method initializes the OSCPortOut for sending messages.
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

            // TODO: make OSC streaming toggle-able
            while (running) {
                if (oscPortOut != null) {
                    // constructs osc messages w arrays from mRealtime log function

                    OSCMessage bone01Message = new OSCMessage("/notch/"+ chestObj[0] +"/all",           Arrays.asList(chestObj[1]+","+chestObj[2]+","+chestObj[3]+","+chestObj[4]+","+chestObj[5]+","+chestObj[6] ));
                    OSCMessage bone02Message = new OSCMessage("/notch/"+ rightUpperArmObj[0] +"/all",   Arrays.asList(rightUpperArmObj[1]+","+rightUpperArmObj[2]+","+rightUpperArmObj[3]+","+rightUpperArmObj[4]+","+rightUpperArmObj[5]+","+rightUpperArmObj[6] ));
                    OSCMessage bone03Message = new OSCMessage("/notch/"+ rightForeArmObj[0] +"/all",    Arrays.asList(rightForeArmObj[1]+","+rightForeArmObj[2]+","+rightForeArmObj[3]+","+rightForeArmObj[4]+","+rightForeArmObj[5]+","+rightForeArmObj[6] ));
                    OSCMessage bone04Message = new OSCMessage("/notch/"+ leftUpperArmObj[0] +"/all",    Arrays.asList(leftUpperArmObj[1]+","+leftUpperArmObj[2]+","+leftUpperArmObj[3]+","+leftUpperArmObj[4]+","+leftUpperArmObj[5]+","+leftUpperArmObj[6] ));
                    OSCMessage bone05Message = new OSCMessage("/notch/"+ leftForeArmObj[0] +"/all",     Arrays.asList(leftForeArmObj[1]+","+leftForeArmObj[2]+","+leftForeArmObj[3]+","+leftForeArmObj[4]+","+leftForeArmObj[5]+","+leftForeArmObj[6] ));

                    try {
                        // make osc bundles and add osc messages to them

                        OSCBundle bone01AllBundle = new OSCBundle();
                        bone01AllBundle.addPacket(bone01Message);

                        OSCBundle bone02AllBundle = new OSCBundle();
                        bone02AllBundle.addPacket(bone02Message);

                        OSCBundle bone03AllBundle = new OSCBundle();
                        bone03AllBundle.addPacket(bone03Message);

                        OSCBundle bone04AllBundle = new OSCBundle();
                        bone04AllBundle.addPacket(bone04Message);

                        OSCBundle bone05AllBundle = new OSCBundle();
                        bone05AllBundle.addPacket(bone05Message);

                        oscPortOut.send(bone01AllBundle);
                        oscPortOut.send(bone02AllBundle);
                        oscPortOut.send(bone03AllBundle);
                        oscPortOut.send(bone04AllBundle);
                        oscPortOut.send(bone05AllBundle);


                        // pause so it's not sending LOADS of OSC
                        sleep(OSCdelay);

                    } catch (Exception e) {
                        // Error handling for some error
                    }
                }
            }
        }
    }


    //private OSCThread oscThread = new OSCThread();
//    (new OSCThread()).start();


    // This thread will contain all the code that pertains to OSC
//    private Thread oscThread = new Thread() {
//        @Override
//        public void run() {
//            /* The first part of the run() method initializes the OSCPortOut for sending messages.
//             * For more advanced apps, where you want to change the address during runtime, you will want
//             * to have this section in a different thread, but since we won't be changing addresses here,
//             * we only have to initialize the address once.
//             */
//
//            try {
//                // Connect to some IP address and port
//                oscPortOut = new OSCPortOut(InetAddress.getByName(myIP), myPort);
//            } catch(UnknownHostException e) {
//                // Error handling when your IP isn't found
//                return;
//            } catch(Exception e) {
//                // Error handling for any other errors
//                return;
//            }
//
//            // TODO: make OSC streaming toggle-able
//            while (running) {
//                if (oscPortOut != null) {
//                    // constructs osc messages w arrays from mRealtime log function
//
//                    OSCMessage bone01Message = new OSCMessage("/notch/"+ chestObj[0] +"/all",           Arrays.asList(chestObj[1]+","+chestObj[2]+","+chestObj[3]+","+chestObj[4]+","+chestObj[5]+","+chestObj[6] ));
//                    OSCMessage bone02Message = new OSCMessage("/notch/"+ rightUpperArmObj[0] +"/all",   Arrays.asList(rightUpperArmObj[1]+","+rightUpperArmObj[2]+","+rightUpperArmObj[3]+","+rightUpperArmObj[4]+","+rightUpperArmObj[5]+","+rightUpperArmObj[6] ));
//                    OSCMessage bone03Message = new OSCMessage("/notch/"+ rightForeArmObj[0] +"/all",    Arrays.asList(rightForeArmObj[1]+","+rightForeArmObj[2]+","+rightForeArmObj[3]+","+rightForeArmObj[4]+","+rightForeArmObj[5]+","+rightForeArmObj[6] ));
//                    OSCMessage bone04Message = new OSCMessage("/notch/"+ leftUpperArmObj[0] +"/all",    Arrays.asList(leftUpperArmObj[1]+","+leftUpperArmObj[2]+","+leftUpperArmObj[3]+","+leftUpperArmObj[4]+","+leftUpperArmObj[5]+","+leftUpperArmObj[6] ));
//                    OSCMessage bone05Message = new OSCMessage("/notch/"+ leftForeArmObj[0] +"/all",     Arrays.asList(leftForeArmObj[1]+","+leftForeArmObj[2]+","+leftForeArmObj[3]+","+leftForeArmObj[4]+","+leftForeArmObj[5]+","+leftForeArmObj[6] ));
//
//                    try {
//                        // make osc bundles and add osc messages to them
//
//                        OSCBundle bone01AllBundle = new OSCBundle();
//                        bone01AllBundle.addPacket(bone01Message);
//
//                        OSCBundle bone02AllBundle = new OSCBundle();
//                        bone02AllBundle.addPacket(bone02Message);
//
//                        OSCBundle bone03AllBundle = new OSCBundle();
//                        bone03AllBundle.addPacket(bone03Message);
//
//                        OSCBundle bone04AllBundle = new OSCBundle();
//                        bone04AllBundle.addPacket(bone04Message);
//
//                        OSCBundle bone05AllBundle = new OSCBundle();
//                        bone05AllBundle.addPacket(bone05Message);
//
//                        oscPortOut.send(bone01AllBundle);
//                        oscPortOut.send(bone02AllBundle);
//                        oscPortOut.send(bone03AllBundle);
//                        oscPortOut.send(bone04AllBundle);
//                        oscPortOut.send(bone05AllBundle);
//
//
//                        // pause so it's not sending LOADS of OSC
//                        sleep(OSCdelay);
//
//                    } catch (Exception e) {
//                        // Error handling for some error
//                    }
//                }
//            }
//        }



    };




