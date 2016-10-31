package arituerto.sensorandcameralogger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity:: ";

    // SENSORS
    private SensorManager mSensorManager;
    private List<Sensor> mSensorList;
    private List<Sensor> mSelectedSensorList;
    private Map<Sensor, Logger> mSensorLoggerMap;

    // CAMERA
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;

    // Logging data
    private boolean mLoggingActive;
    private File loggingDir;
    private File imageDir;
    private String dataSetName;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, " onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        mSelectedSensorList = new ArrayList<Sensor>(mSensorList);
        mSensorLoggerMap = new HashMap<Sensor, Logger>();

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        startSensorListeners(mSelectedSensorList);

        mCameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);

        final Button startButton = (Button) findViewById(R.id.buttonStartLogging);
        startButton.setOnClickListener(startClick);
        final Button stopButton = (Button) findViewById(R.id.buttonStopLogging);
        stopButton.setOnClickListener(stopClick);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSensorListeners();

    }

    private void startSensorListeners(List<Sensor> sensorList) {
        for (Sensor iSensor : sensorList) {
            mSensorManager.registerListener(this,iSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void stopSensorListeners() {
        mSensorManager.unregisterListener(this);
    }

    private View.OnClickListener startClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (!mLoggingActive) {

                Log.i(TAG, "Start Logging");

                EditText textEntry = (EditText) findViewById(R.id.inputDataSetName);
                dataSetName = textEntry.getText().toString();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
                String currentDateAndTime = sdf.format(new Date());

                // Create directory
                loggingDir = new File(Environment.getExternalStorageDirectory().getPath() +
                        "/" + currentDateAndTime +
                        "_" + Build.MANUFACTURER +
                        "_" + Build.MODEL +
                        "_" + dataSetName);
                try {
                    loggingDir.mkdirs();
                } catch (Exception e) {
                    Log.i(TAG, e.getMessage());
                }

                imageDir = new File(loggingDir.getPath() + "/images");
                try {
                    imageDir.mkdirs();
                } catch (Exception e) {
                    Log.i(TAG, e.getMessage());
                }

                String loggerFileName;
                for (Sensor iSensor : mSensorList) {

                    String sensorTypeString = iSensor.getStringType();
                    String[] parts = sensorTypeString.split("\\.");
                    loggerFileName = loggingDir.getPath() + "/sensor_" + parts[parts.length - 1].toUpperCase() + "_log.csv";

                    // First line: Data description
                    String csvFormat = "// SYSTEM_TIME [ns], EVENT_TIMESTAMP [ns], EVENT_" + sensorTypeString + "_VALUES";
                    try {
                        Logger logger = new Logger(loggerFileName);
                        mSensorLoggerMap.put(iSensor, logger);
                        try {
                            logger.log(csvFormat);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                mLoggingActive = true;
                progressBar.setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "System is already Logging");
            }
        }
    };

    private View.OnClickListener stopClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mLoggingActive) {
                Log.i(TAG, "Stop Logging");
                for (Map.Entry<Sensor, Logger> iSensorLogger : mSensorLoggerMap.entrySet()) {
                    try {
                        iSensorLogger.getValue().close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                mSensorLoggerMap.clear();
                mLoggingActive = false;
                progressBar.setVisibility(View.GONE);
            } else {
                Log.i(TAG, "System is not Logging");
            }
        }
    };

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        if (mLoggingActive) {
            Sensor key = event.sensor;
            Logger sensorLogger = mSensorLoggerMap.get(key);
            String eventData = SystemClock.elapsedRealtimeNanos() + "," + event.timestamp;
            for (float i : event.values){
                eventData += "," + i;
            }
            try {
                sensorLogger.log(eventData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
