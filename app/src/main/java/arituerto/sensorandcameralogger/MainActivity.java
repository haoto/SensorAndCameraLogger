package arituerto.sensorandcameralogger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // TODO: Save configuration for next runs.
    // TODO: Add GPS logging
    // TODO: Write logging session description (Time, sensors, sensor delay, camera properties,...)
    // TODO: Add EXIT button or option

    private static final String TAG = "MAIN";

    static final int SENSORS_SETTINGS_REQUEST = 1;
    static final int CAMERA_SETTINGS_REQUEST = 2;
    static final int[] SENSOR_TYPES = new int[]{
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR};

    // SENSORS
    private SensorManager mSensorManager;
    private Map<String, Sensor> mSensorMap;
    private ArrayList<String> mNameSensorList;
    private boolean[] mSelectedSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap;
    int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;

    // CAMERA
    private String mCameraId;
    private Size mImageSize;
    private Size[] mJpegSizeList;
    int mFocusMode;
    int[] mFocusModeList;

    private Surface mPreviewSurface;
    private Surface mReaderSurface;
    private List<Surface> mSurfaceList = new ArrayList<Surface>();

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CaptureRequest.Builder mSaveRequestBuilder;
    private CaptureRequest mSaveRequest;
    private ImageReader mImgReader;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    // LOGGING
    boolean loggingActive = false;
    boolean cameraActive = false;
    File loggingDir;
    File imageDir;
    String dataSetName;

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startCameraHandlerThread();
        getCameraConfiguration();
        setupSurfaces();

        //VISUAL
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);

        // SENSORS
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensorList = new ArrayList<Sensor>();
        for (int iSensorType = 0; iSensorType < SENSOR_TYPES.length; iSensorType++) {
            List<Sensor> auxList = mSensorManager.getSensorList(SENSOR_TYPES[iSensorType]);
            for (Sensor iSensor : auxList) {
                sensorList.add(iSensor);
            }
        }
        mSensorLoggerMap = new HashMap<Sensor, Logger>();
        mNameSensorList = new ArrayList<String>();
        mSelectedSensorList = new boolean[sensorList.size()];
        mSensorMap = new HashMap<String, Sensor>();
        for (int i = 0; i < sensorList.size(); i++) {
            Sensor iSensor = sensorList.get(i);
            String sensorString = iSensor.getStringType().split("\\.")[2].toUpperCase() +
                    "\n" +
                    iSensor.getName();
            mSensorMap.put(sensorString, iSensor);
            mNameSensorList.add(sensorString);
            mSelectedSensorList[i] = true;
        }

        // CAMERA

        final Button sensorSettingsButton = (Button) findViewById(R.id.buttonSensorSettings);
        sensorSettingsButton.setOnClickListener(sensorSettingsClick);
        final Button cameraSettingsButton = (Button) findViewById(R.id.buttonCameraSettings);
        cameraSettingsButton.setOnClickListener(cameraSettingsClick);
        final Button startButton = (Button) findViewById(R.id.buttonStartLogging);
        startButton.setOnClickListener(startClick);
        final Button stopButton = (Button) findViewById(R.id.buttonStopLogging);
        stopButton.setOnClickListener(stopClick);
    }

    @Override
    protected void onDestroy() {

        Log.i(TAG, "onDestroy");
        super.onDestroy();

        closeCamera();
        stopCameraHandlerThread();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SENSORS_SETTINGS_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Sensor Settings Received");
                Bundle bundle = data.getExtras();
                mSelectedSensorList = bundle.getBooleanArray("selectedSensors");
                sensorDelay = bundle.getInt("sensorDelay");
            }
        }

    }

    // SENSORS FUNCTIONS
    private void startSensorListeners() {
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
            if (mSelectedSensorList[iSensor]) {
                mSensorManager.registerListener(this,
                        mSensorMap.get(mNameSensorList.get(iSensor)),
                        sensorDelay);
            }
        }
        Log.i(TAG, "Sensor Listeners ON");
    }

    private void stopSensorListeners() {
        mSensorManager.unregisterListener(this);
        Log.i(TAG, "Sensor Listeners OFF");
    }

    private void startSensorsLogging() {

        startSensorListeners();

        EditText textEntry = (EditText) findViewById(R.id.inputDataSetName);
        dataSetName = textEntry.getText().toString();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());

        // CREATE LOGGING DIRECTORY
        loggingDir = new File(Environment.getExternalStorageDirectory().getPath() +
                "/" + currentDateAndTime +
                "_" + Build.MANUFACTURER +
                "_" + Build.MODEL +
                "_" + dataSetName);
        try {
            loggingDir.mkdirs();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

//        // CREATE IMAGES DIRECTORY
//        imageDir = new File(loggingDir.getPath() + "/images");
//        if (cameraActive) {
//            try {
//                imageDir.mkdirs();
//            } catch (SecurityException e) {
//                e.printStackTrace();
//            }
//        }

        // CREATE SENSOR LOGGERS
        String loggerFileName;
        for (int iSensor = 0; iSensor < mSelectedSensorList.length; iSensor++) {
            if (mSelectedSensorList[iSensor]) {

                Sensor sensor = mSensorMap.get(mNameSensorList.get(iSensor));

                String sensorTypeString = sensor.getStringType();
                String[] parts = sensorTypeString.split("\\.");
                loggerFileName = loggingDir.getPath() + "/sensor_" + parts[parts.length - 1].toUpperCase() + "_log.csv";

                // First line: Data description
                String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ns], EVENT_" + sensorTypeString + "_VALUES";
                try {
                    Logger logger = new Logger(loggerFileName);
                    mSensorLoggerMap.put(sensor, logger);
                    try {
                        logger.log(csvFormat);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void stopSensorsLogging() {
        for (Map.Entry<Sensor, Logger> iSensorLogger : mSensorLoggerMap.entrySet()) {
            try {
                iSensorLogger.getValue().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        mSensorLoggerMap.clear();
        stopSensorListeners();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        if (loggingActive) {
            Sensor key = event.sensor;
            Logger sensorLogger = mSensorLoggerMap.get(key);
            String eventData = SystemClock.elapsedRealtimeNanos() + "," + event.timestamp;
            for (float i : event.values) {
                eventData += "," + i;
            }
            try {
                sensorLogger.log(eventData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // CAMERA FUNCTIONS

    private void startCameraHandlerThread() {
        mHandlerThread = new HandlerThread("Camera Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopCameraHandlerThread() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandlerThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getCameraConfiguration() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics cc = cameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap streamMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mJpegSizeList = streamMap.getOutputSizes(ImageFormat.JPEG);
            setCameraImageSize(mJpegSizeList[7]); // TODO: Something better please!
            mFocusModeList = cc.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            setCameraAutoFocus(mFocusModeList[0]);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setCameraImageSize(Size imgSize) {
        mImageSize = imgSize;
    }

    private void setCameraAutoFocus(int focusMode) {
        mFocusMode = focusMode;
    }

    private void setupSurfaces() {

        TextureView textureView = (TextureView) findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mPreviewSurface = new Surface(surface);
                if (null != surface) {
                    setupCamera();
                }
                Log.i("TextureView", "Surface Available");
                Log.i("TextureView", "Surface size" + width + "x" + height);
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });

        mImgReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
        mImgReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);
        mReaderSurface = mImgReader.getSurface();
    }

    private void setupCamera() {

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        //Find the right camera device
        try {
            mCameraId = mCameraManager.getCameraIdList()[0];
            if (mCameraId != null) {
                try {
                    //Try to open the camera
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                        return;
                    }
                    mCameraManager.openCamera(mCameraId, mCameraStateCallback, mHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i("setupCamera", "Unable to access camera!");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera(){

        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (null != mImgReader) {
            mImgReader.close();
            mImgReader = null;
        }

    }

    private void setupCaptureSession() throws CameraAccessException {
        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewRequestBuilder.addTarget(mPreviewSurface);
        mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                        mCaptureSession = cameraCaptureSession;
                        try {
                            mPreviewRequest = mPreviewRequestBuilder.build();
                            //Schedule the repeating request
                            mCaptureSession.setRepeatingRequest(mPreviewRequest, mSessionCaptureCallback, mHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {}
                }, null);
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try{
                image = reader.acquireNextImage();
                Log.i("ImageReader", "Image read " + image.getWidth() + "x" + image.getHeight());
                Log.i("ImageReader", "Capture completed at " + image.getTimestamp());
            }catch(IllegalStateException e){
                e.printStackTrace();
                Log.e("ImageReader", "No more buffers available, skipping frame");
            }
            image.close();
        }
    };

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
			Log.d("CameraDevice", "Camera device opened!");
			mCameraDevice = camera;
            try {
                setupCaptureSession();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
			Log.d("CameraDevice", "Camera device disconnected!");
			mCameraDevice = null;
        }
        @Override
        public void onError(CameraDevice camera, int error) {
			Log.d("CameraDevice", "Camera device error: "+error);
        }
    };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback(){
		@Override
		public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
		}
	};

    // BUTTONS FUNCTIONS
    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!loggingActive) {
                Log.i(TAG, "Start Logging");
                startSensorsLogging();
                loggingActive = true;
                mProgressBar.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "System is already Logging");
            }
        }
    };

    private View.OnClickListener stopClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (loggingActive) {
                Log.i(TAG, "Stop Logging");
                stopSensorsLogging();
                loggingActive = false;
                mProgressBar.setVisibility(View.GONE);
            } else {
                Log.i(TAG, "System is not Logging");
            }
        }
    };

    private View.OnClickListener sensorSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Sensor Settings");
            Intent intent = new Intent(MainActivity.this, SensorSettingsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("allSensors", mNameSensorList);
            bundle.putBooleanArray("selectedSensors", mSelectedSensorList);
            intent.putExtras(bundle);
            startActivityForResult(intent, SENSORS_SETTINGS_REQUEST);
        }
    };

    private View.OnClickListener cameraSettingsClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Camera Settings");
            Intent intent = new Intent(MainActivity.this, SensorSettingsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("allSensors", mNameSensorList);
            bundle.putBooleanArray("selectedSensors", mSelectedSensorList);
            intent.putExtras(bundle);
            startActivityForResult(intent, SENSORS_SETTINGS_REQUEST);
        }
    };
}
