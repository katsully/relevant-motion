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

import com.wearnotch.db.NotchDataBase;
import com.wearnotch.db.model.Device;
import com.wearnotch.framework.ActionDevice;
import com.wearnotch.framework.Bone;
import com.wearnotch.framework.Measurement;
import com.wearnotch.framework.MeasurementType;
import com.wearnotch.framework.NotchNetwork;
import com.wearnotch.framework.Pair;
import com.wearnotch.framework.Session;
import com.wearnotch.framework.Skeleton;
import com.wearnotch.framework.Workout;
import com.wearnotch.framework.visualiser.VisualiserData;
import com.wearnotch.internal.util.IOUtil;
import com.wearnotch.notchdemo.util.Util;
import com.wearnotch.notchdemo.visualiser.VisualiserActivity;
import com.wearnotch.notchdemo.visualiser.VisualizerSettings;
import com.wearnotch.service.common.Cancellable;
import com.wearnotch.service.common.NotchCallback;
import com.wearnotch.service.common.NotchError;
import com.wearnotch.service.common.NotchProgress;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import io.socket.client.Socket;

public class MainFragment extends BaseFragment {
    private static final String TAG = MainFragment.class.getSimpleName();
    private static final String DEFAULT_USER_LICENSE = "tIv2ChbXTspHUG0QfTxa";

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
    private boolean mRealTime;
    private VisualiserData mRealTimeData;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private SimpleDateFormat mSDF;
    private VisualiserActivity mVisualiserActivity;
    private Workout mWorkout;

    private AnimationDrawable mDockAnimation;


    private enum State {CALIBRATION,STEADY};
    private State mState;
    private Socket mSocket;
    private Boolean isConnected = true;

    @InjectView(R.id.new_title)
    TextView mNewTitle;

    @InjectView(R.id.device_list)
    TextView mDeviceList;

    @InjectView(R.id.device_management_txt)
    TextView mDeviceManagementTxt;

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

    // Buttons
    @InjectView(R.id.btn_set_user)
    Button mButtonSetUser;

    @InjectView(R.id.btn_pair)
    Button mButtonPair;

    @InjectView(R.id.btn_sync_pair)
    Button mButtonSyncPair;

    @InjectView(R.id.btn_remove)
    Button mButtonRemove;

    @InjectView(R.id.btn_shutdown)
    Button mButtonShutDown;

    @InjectView(R.id.btn_erase)
    Button mButtonErase;

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


    @InjectView(R.id.btn_visualize)
    Button mButtonVisualize;

    @InjectView(R.id.counter_text)
    TextView mCounterText;

    @InjectView(R.id.dock_image)
    ImageView mDockImg;

    AlertDialog userDialog;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        bindNotchService();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotchApplication app = (NotchApplication) getActivity().getApplication();
        mApplicationContext = getActivity().getApplicationContext();
        mSocket = app.getSocket();
        mSocket.connect();
        mActivity = getBaseActivity();
        mDB = NotchDataBase.getInst();
        VisualizerSettings.init(mApplicationContext);

        if(!hasPermissions(mActivity, PERMISSIONS)){
            requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION);
        }
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
        mButtonErase.setTypeface(tfLight);
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
        mButtonVisualize.setTypeface(tfLight);

        mCounterText.setTypeface(tfLight);
        mRealTimeBox.setTypeface(tfLight);

        // Animation
        mDockImg.setBackgroundResource(R.drawable.sensor_anim);
        mDockAnimation = (AnimationDrawable) mDockImg.getBackground();
        mDockImg.setVisibility(View.INVISIBLE);

        // Other
        mSDF = new SimpleDateFormat("yyyyMMdd_HHmmss");

        buildUserDialog();
        mHandler.postDelayed(mSetDefaultUser, 1000L);

        return root;
    }

    Runnable mSetDefaultUser = new Runnable() {
        @Override
        public void run() {
            setActionBarTitle(R.string.app_name);
            if (mNotchService != null && mUser == null) {
                mUser = DEFAULT_USER_LICENSE;
                updateUser(mUser);
                mNotchService.setLicense(mUser);
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


    @OnClick(R.id.btn_unchecked_init)
    void uncheckedinit() {
        inProgress();
        mNotchService.uncheckedInit(new EmptyCallback<NotchNetwork>() {
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
            Workout workout = Workout.from("Demo_config", skeleton, IOUtil.readAll(new InputStreamReader(mApplicationContext.getResources().openRawResource(R.raw.config_2_real_time))));
            if (mRealTime) {
                workout = Workout.from("Demo_config", skeleton, IOUtil.readAll(new InputStreamReader(mApplicationContext.getResources().openRawResource(R.raw.config_2_real_time))));
                workout = workout.withMeasurementType(MeasurementType.STEADY_SKIP);
            }
            mWorkout = workout;
            inProgress();
            mNotchService.init(workout, new EmptyCallback<NotchNetwork>() {
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
                Pair colors = info.getColor();
                sb.append(info.getBone().getName()). append(": ");
                sb.append(colors.first.toString());
                sb.append(colors.first.equals(colors.second) ? "" : ", " + colors.second.toString());
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
           // mSocket.emit("newmessage", "startcapture");
        }
        else {
            inProgress();
            mNotchService.configureTimedCapture(TIMED_CAPTURE_LENGTH, false, new EmptyCallback<Void>());
        }
    }

//    Cancellable c;
//
//    @OnClick(R.id.btn_capture)
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
//            mNotchService.timedCapture(new EmptyCallback<Measurement>() {
//                @Override
//                public void onSuccess(Measurement measurement) {
//                    mCurrentMeasurement = measurement;
//                    Util.showNotification("Capture finished");
//                    clearText();
//                }
//            });
//        }
//    }
Cancellable c;
    long mUpdateStartTime;
    long mRefreshTime = 20;

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
                        //updateRealTime();
                        mUpdateStartTime = System.currentTimeMillis();
                        mHandler.removeCallbacks(mLogRealTimeData);
                        mHandler.post(mLogRealTimeData);
                        //mSocket.emit("newmessage", mLogRealTimeData);
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
                    mHandler.removeCallbacks(mLogRealTimeData);
                }

                @Override
                public void onCancelled() {
                    Util.showNotification("Real-time measurement stopped!");
                    clearText();
                    mHandler.removeCallbacks(mLogRealTimeData);

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
            // Slect the current frame from the last update
            int currentFrame = startingFrame + (int)(millisSinceUpdate / millisPerFrame);

            // Show the last frame until a new update comes
            if (currentFrame > mRealTimeData.getFrameCount() - 1) currentFrame = mRealTimeData.getFrameCount() - 1;

            // Logging data for measured bones
            Log.d("REALTIME", "Current frame:" + currentFrame);
            for (Bone b : mNotchService.getNetwork().getDevices().keySet()) {
                //fvec3 a=mRealTimeData.getPos(b,currentFrame);
//                Log.d("REALTIME", b.getName() + " "
//                        // Orientation (quaternion)
//                        + mRealTimeData.getQ(b,currentFrame) + " "
//                        // Position of the bone (end of vector)
//                        + mRealTimeData.getPos(b,currentFrame));
                mSocket.emit("newmessage","0"+b.getName() + " "
                        // Orientation (quaternion)
                       + mRealTimeData.getQ(b,currentFrame) + " "
                        // Position of the bone (end of vector)
                        + mRealTimeData.getPos(b,currentFrame));
            }

            mHandler.postDelayed(mLogRealTimeData,mRefreshTime);
        }
    };

    @OnClick(R.id.btn_download)
    void download() {
        if (mRealTime) {
            if (c != null) {
                c.cancel();
                c = null;
            }
        }
        else {
            File outputDir = Environment.getExternalStoragePublicDirectory(NOTCH_DIR);
            if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
                Util.showNotification("Failed to create output directory!");
            }
            String currentDateAndTime = mSDF.format(new Date());
            mCurrentOutput = new File(outputDir, "meas_" + currentDateAndTime + ".zip");
            inProgress();
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
            });
        }
    }

    @OnClick(R.id.btn_visualize)
    void vis() {
        visualise();
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
                    sb.append(device.getNotchDevice().getDeviceMac()).append(")\n");
                }
                mDeviceList.setText(sb.toString());
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
                    mNotchService.steady(new EmptyCallback<Session>());
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

}
