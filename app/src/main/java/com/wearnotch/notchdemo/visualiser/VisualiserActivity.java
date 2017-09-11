package com.wearnotch.notchdemo.visualiser;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wearnotch.framework.Bone;
import com.wearnotch.framework.Skeleton;
import com.wearnotch.framework.visualiser.VisualiserData;
import com.wearnotch.notchdemo.R;
import com.wearnotch.visualiser.NotchSkeletonRenderer;
import com.wearnotch.visualiser.shader.ColorShader;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class VisualiserActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "Visualiser";

    private static final float SECONDARY_TRANSPARENCY = 0.8f;

    private static final int REQUEST_OPEN = 1;

    public static final String PARAM_INPUT_ZIP = "INPUT_ZIP";
    public static final String PARAM_INPUT_DATA = "INPUT_DATA";
    public static final String PARAM_REALTIME = "REALTIME";

    public static Intent createIntent(Context context, Uri zipUri) {
        Intent i = new Intent(context, VisualiserActivity.class);
        i.putExtra(PARAM_INPUT_ZIP, new Parcelable[] { zipUri });
        return i;
    }

    public static Intent createIntent(Context context, VisualiserData data, boolean realtime) {
        Intent i = new Intent(context, VisualiserActivity.class);
        i.putExtra(PARAM_INPUT_DATA, data);
        i.putExtra(PARAM_REALTIME,realtime);
        return i;
    }

    private Context mApplicationContext;
    private Parcelable[] mZipUri;
    private VisualiserData mData;
    private Skeleton mSkeleton;

    private volatile boolean mPaused;
    private volatile int mFrameIndex;
    private int mFrameCount;

    private volatile float mSpeed = 1f;
    private volatile boolean mSeeking;
    private volatile boolean mShowPath, mPinToCentre, mHighlightBones;
    private volatile List<Bone> mBonesToShow = new ArrayList<>();
    private String[] mBoneNames;
    private volatile boolean[] mCheckedBones;
    private boolean mRealTime, isGroundDrawn;

    private AlertDialog mBoneSelectorDialog;

    @InjectView(R.id.progress_animation)
    protected ProgressBar mProgress;

    @InjectView(R.id.surface_view)
    protected TouchGLView mSurfaceView;

    private ExtendedRenderer mRenderer;

    @InjectView(R.id.seekbar)
    protected SeekBar mSeekBar;

    @InjectView(R.id.button_play_pause)
    protected ImageButton mPlayPause;

    @InjectView(R.id.speed_text)
    protected TextView mSpeedText;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationContext = getApplicationContext();
        setContentView(R.layout.activity_visualiser);
        ButterKnife.inject(this);

        mZipUri = getIntent().getParcelableArrayExtra(PARAM_INPUT_ZIP);
        mData = (VisualiserData) getIntent().getSerializableExtra(PARAM_INPUT_DATA);
        mRealTime = getIntent().getBooleanExtra(PARAM_REALTIME,false);
        mSeekBar.setOnSeekBarChangeListener(this);
        initRenderer();
    }

    @Subscribe
    public void onDataUpdate(VisualiserData data) {
        if (data != null && mRenderer != null) {
            mData = data;
            mRenderer.updateData(0,data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.visualiser, menu);

        VisualizerSettings settings = VisualizerSettings.getInstance();

        if (settings != null) {
            MenuItem highlightBones = menu.findItem(R.id.highlight_bones);
            mHighlightBones = settings.isVisualizerShowFullBody();
            highlightBones.setChecked(mHighlightBones);

            MenuItem pinToCenter = menu.findItem(R.id.pin_to_center);
            mPinToCentre = settings.isVisualizerPinToCentre();
            pinToCenter.setChecked(mPinToCentre);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open:
                openMeasurement();
                return true;
            case R.id.highlight_bones:
                mHighlightBones = !mHighlightBones;
                mRenderer.setHighlightBones(mHighlightBones);
                item.setChecked(mHighlightBones);
                VisualizerSettings.getInstance().putVisualizerShowFullBody(mHighlightBones);
                refreshUI();
                return true;
            case R.id.pin_to_center:
                mPinToCentre = !mPinToCentre;
                mRenderer.setRootMovement(!mPinToCentre);
                item.setChecked(mPinToCentre);
                VisualizerSettings.getInstance().putVisualizerPinToCenter(mPinToCentre);
                refreshUI();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_OPEN:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    setData(data.getData());
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initRenderer() {
        new AsyncTask<Void, Void, ExtendedRenderer>() {

            @Override
            protected ExtendedRenderer doInBackground(Void... params) {

                try {
                    ExtendedRenderer renderer = new ExtendedRenderer(
                            getApplicationContext()) {
                        ColorShader mVisualisationShader;

                        @Override
                        protected void createAdditionalShaders() {
                            super.createAdditionalShaders();
                            mVisualisationShader = new ColorShader(mApplicationContext);
                            addShader(mVisualisationShader);
                        }

                        @Override
                        protected void onSurfaceCreatedGL(GL10 unused, EGLConfig config) {
                            super.onSurfaceCreatedGL(unused, config);
                            GLES20.glClearColor(0.87f, 0.87f, 0.87f, 0.35f);
                        }

                        @Override
                        protected RendererContext createRendererContext(VisualiserData data) {
                            final PlotDemo plotDemo = new PlotDemo(data, isGroundDrawn);
                            if (!isGroundDrawn) isGroundDrawn = true;
                            plotDemo.init();

                            return new RendererContext(data) {

                                @Override
                                public void prepare() {
                                    plotDemo.prepare(mVisualisationShader);
                                    super.prepare();
                                }

                                @Override
                                public void draw() {
                                    super.draw();
                                    plotDemo.setShowPath(mShowPath ? mBonesToShow : null);
                                    plotDemo.draw(getFrameIndex());
                                }

                            };

                        }

                    };
                    renderer.setAutoPlayback(true);

                    if (mZipUri != null) {
                        for (Parcelable uri : mZipUri) {
                            VisualiserData data = VisualiserData.fromStream(new FileInputStream(new File(((Uri) uri).getPath())));
                            if (data != null) {
                                int dataIndex = renderer.addData(data);
                                if (dataIndex > 0) {
                                    renderer.setAlpha(dataIndex, SECONDARY_TRANSPARENCY);
                                }
                            }
                        }
                    } else if (mData != null) {
                        int dataIndex = renderer.addData(mData);
                        if (dataIndex > 0) {
                            renderer.setAlpha(dataIndex, SECONDARY_TRANSPARENCY);
                        }
                    }

                    return renderer;
                } catch (Exception e) {
                    Log.e(TAG, "NotchSkeletonRenderer exception", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ExtendedRenderer renderer) {
                super.onPostExecute(renderer);
                if (renderer != null) {
                    mRenderer = renderer;
                    mSurfaceView.setEGLConfigChooser(new EGLConfigChooser());
                    mSurfaceView.setRenderer(renderer);
                    mProgress.setVisibility(View.GONE);
                    mSurfaceView.setVisibility(View.VISIBLE);
                    mShowPath = true;

                    mRenderer.setHighlightBones(mHighlightBones);
                    mRenderer.setRootMovement(!mPinToCentre);

                    if (mData == null) mData = renderer.getData(0);
                    mFrameCount = mData.getFrameCount();
                    mSeekBar.setMax(mFrameCount);
                    if (mRealTime) {
                        mSeekBar.setMax(0);
                        mSeekBar.setEnabled(false);
                        mPlayPause.setEnabled(false);
                    }
                    mRenderer.setRealTime(mRealTime);
                    mSkeleton = mData.getSkeleton();

                    mBoneNames = new String[mSkeleton.getBoneOrder().size()];
                    mCheckedBones = new boolean[mSkeleton.getBoneOrder().size()];
                    int i = 0;
                    for (Bone b : mSkeleton.getBoneOrder()) {
                        mBoneNames[i] = b.getName();
                        if (b.getName().equals("RightHand")) {
                            mCheckedBones[i] = true;
                            mBonesToShow.add(b);
                        } else {
                            mCheckedBones[i] = false;
                        }
                        i++;
                    }

                    buildBoneSelectorDialog();

                } else {
                    showNotification(R.string.error_invalid_measurement);
                    finish();
                }
            }
        }.execute();
    }

    public void setData(final Uri zipUri) {
        new AsyncTask<Void, Void, VisualiserData>() {

            @Override
            protected VisualiserData doInBackground(Void... params) {
                try {
                    return VisualiserData.fromStream(new FileInputStream(new File(zipUri.getPath())));
                } catch (Exception e) {
                    Log.e(TAG, "NotchSkeletonRenderer exception", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final VisualiserData data) {
                super.onPostExecute(data);
                if (data != null) {
                    mZipUri = Arrays.copyOf(mZipUri, mZipUri.length + 1);
                    mZipUri[mZipUri.length - 1] = zipUri;
                    getIntent().putExtra(PARAM_INPUT_ZIP, mZipUri);

                    mSurfaceView.postOnAnimation(new Runnable() {
                        @Override
                        public void run() {
                            int dataIndex = mRenderer.setData(data);
                            mFrameCount = mRenderer.getData(dataIndex).getFrameCount();
                            mSeekBar.setMax(mFrameCount);
                            mData = data;
                            refreshUI();
                        }
                    });

                } else {
                    showNotification(R.string.error_invalid_measurement);
                }
            }
        }.execute();
    }

    public void refreshUI() {
        mSeekBar.setProgress(mFrameIndex);

        if (mPaused) {
            mPlayPause.setImageResource(R.drawable.ic_play);
        } else {
            mPlayPause.setImageResource(R.drawable.ic_pause);
        }
    }

    private Runnable mRefreshPlayback = new Runnable() {
        @Override
        public void run() {
            if (mRenderer != null) {
                for (int i=0; i<mRenderer.getContextSize();i++) {
                    if (mRenderer.dataIndexExists(i)) mRenderer.setFrameIndex(i,mFrameIndex);
                }

                if (!mSeeking) {
                    mRenderer.setAutoPlayback(!mPaused);
                }
            }
        }
    };

    private Runnable mStartSeeking = new Runnable() {
        @Override
        public void run() {
            if (mRenderer != null && mSeeking) {
                mRenderer.setAutoPlayback(false);
            }
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mRenderer != null && fromUser && mFrameIndex != progress) {
            mFrameIndex = progress;
            mSurfaceView.postOnAnimation(mRefreshPlayback);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mRenderer != null) {
            mSeeking = true;
            mSurfaceView.postOnAnimation(mStartSeeking);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mSeeking = false;
        mSurfaceView.postOnAnimation(mRefreshPlayback);
    }

    private void openMeasurement() {
        Intent chooser = new Intent(Intent.ACTION_GET_CONTENT);
        chooser.setType("application/zip");
        chooser.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(chooser, REQUEST_OPEN);
    }


    @OnClick(R.id.button_play_pause)
    void onPlayPauseClicked() {
        if (mRenderer != null) {
            if (mPaused && mFrameIndex == mFrameCount - 1) {
                mFrameIndex = 0;
            }
            mPaused = !mPaused;
            refreshUI();
            mSurfaceView.postOnAnimation(mRefreshPlayback);
        }
    }

    @OnClick(R.id.button_start)
    void onStartClicked() {
        if (mRenderer != null && !mRealTime) {
            mRenderer.setAutoPlayback(false);
            mRenderer.setAllFrameIndex(0);
            mSurfaceView.postOnAnimation(mRefreshPlayback);
            mRenderer.setAutoPlayback(true);

        }
        refreshUI();
    }

    @OnClick(R.id.button_end)
    void onEndClicked() {
        if (mRenderer != null && !mRealTime) {
            mPaused = true;
            mRenderer.setAutoPlayback(false);
            mRenderer.setAllFrameIndex(mFrameCount);
            mSurfaceView.postOnAnimation(mRefreshPlayback);
        }
        refreshUI();
    }

    @OnClick(R.id.button_speed)
    void onSpeedClicked() {
        if (mRenderer != null && !mRealTime) {
            mSpeed = mSpeed * 2f;
            if (mSpeed > 1f) {
                mSpeed = 0.25f;
            }
            if (mSpeed >= 1f) {
                mSpeedText.setText((int)mSpeed + "x");
            }
            else {
                mSpeedText.setText("1/" + (int)(1f/mSpeed) + "x");
            }
            mRenderer.setAllPlaybackSpeed(mSpeed);
        }
        refreshUI();
    }

    @OnClick(R.id.button_front_view)
    void onFrontViewClicked() {
        if (mRenderer != null) {
            mRenderer.setCameraBeta((float) Math.PI/2.0f);
            mRenderer.setCameraAlpha((float) Math.PI / 2.0f);
        }
        refreshUI();
    }

    @OnClick(R.id.button_top_view)
    void onTopViewClicked() {
        if (mRenderer != null) {
            mRenderer.setCameraBeta(0.0f);
            mRenderer.setCameraAlpha((float) Math.PI/2.0f);
        }
        refreshUI();
    }

    @OnClick(R.id.button_side_view)
    void onSideViewClicked() {
        if (mRenderer != null) {
            mRenderer.setCameraBeta((float) Math.PI/2.0f);
            mRenderer.setCameraAlpha((float) Math.PI);
        }
        refreshUI();
    }

    @OnClick(R.id.button_show_path)
    void onShowPathClicked() {
        mBoneSelectorDialog.show();
    }

    public void showNotification(final String msg) {
        try {
            Toast.makeText(mApplicationContext, msg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Toast exception", e);
        }
    }

    public void showNotification(final int stringId) {
        try {
            Toast.makeText(mApplicationContext, stringId, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Toast exception", e);
        }
    }


    private static class EGLConfigChooser implements GLSurfaceView.EGLConfigChooser {

        private static final int[] PRIMARY_ATTRS = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL11.EGL_SAMPLE_BUFFERS, 1,
                EGL11.EGL_SAMPLES, 4,
                EGL11.EGL_NONE
        };

        private static final int[] SECONDARY_ATTRS = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL11.EGL_NONE
        };

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] confAttribs = PRIMARY_ATTRS;
            int[] numConfig = new int[1];

            egl.eglChooseConfig(display, confAttribs, null, 0, numConfig);
            if (numConfig[0] <= 0) {
                confAttribs = SECONDARY_ATTRS;
                egl.eglChooseConfig(display, confAttribs, null, 0, numConfig);
                if (numConfig[0] <= 0)
                    throw new IllegalArgumentException("No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfig[0]];
            egl.eglChooseConfig(display, confAttribs, configs, numConfig[0], numConfig);

            return configs[0];
        }
    }

    class ExtendedRenderer extends NotchSkeletonRenderer {
        private Context mContext;
        private final float cameraBetaMax = (float) Math.PI - 0.01f;
        private final float cameraBetaMin = 0.01f;
        private final float cameraDistanceMin = 0.5f;

        public ExtendedRenderer(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        protected void onFrameIndexChanged(int dataIndex, int frameIndex) {
            super.onFrameIndexChanged(dataIndex, frameIndex);
            mFrameIndex = frameIndex;
            mSeekBar.setProgress(mFrameIndex);
        }

        @Override
        protected void onPlaybackFinished(int dataIndex, int frameIndex) {
            super.onPlaybackFinished(dataIndex, frameIndex);
            mPaused = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshUI();
                }
            });
            mSurfaceView.postOnAnimation(mRefreshPlayback);
        }


        @Override
        public void setCameraBeta(float cameraBeta) {
            if (cameraBeta > cameraBetaMax) {
                cameraBeta = cameraBetaMax;
            } else if (cameraBeta < cameraBetaMin) {
                cameraBeta = cameraBetaMin;
            }

            super.setCameraBeta(cameraBeta);
        }

        @Override
        public void setCameraDistance(float cameraDistance) {
            if (cameraDistance < cameraDistanceMin) {
                cameraDistance = cameraDistanceMin;
            }

            super.setCameraDistance(cameraDistance);
        }
    }

    private void buildBoneSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select bones!");

        builder.setMultiChoiceItems(mBoneNames, mCheckedBones, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                Bone b = mSkeleton.getBone(mBoneNames[which]);
                mCheckedBones[which] = isChecked;
                if (isChecked) {
                    if (!mBonesToShow.contains(b)) mBonesToShow.add(b);
                } else {
                    if (mBonesToShow.contains(b)) mBonesToShow.remove(b);
                }
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setNegativeButton("Clear all", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mBonesToShow.removeAll(mBonesToShow);
                for (int i=0; i<mCheckedBones.length; i++) {
                    Bone b = mSkeleton.getBone(mBoneNames[i]);
                    mBonesToShow.remove(b);
                    mCheckedBones[i]= false;
                    ((AlertDialog) dialog).getListView().setItemChecked(i, false);
                }
            }
        });

        mBoneSelectorDialog = builder.create();
    }
}
