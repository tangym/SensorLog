package tangym.sensorlog;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.Manifest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private final String TAG = "SensorLog";
    private final String PATH = "sdcard/SensorLog/";
    private SensorManager mSensorManager;
    private BufferedWriter output;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private ToggleButton mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        // Check for sensor and sdcard permission
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.BODY_SENSORS
            }, 0);
        }

        // Create folder if not exists
        File dir = new File(PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mButton = (ToggleButton) findViewById(R.id.button);
        mButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });
    }

    protected  void startRecording() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(new Date());
        File file = new File(PATH, String.format("s%s.csv", timestamp));
        try {
            output = new BufferedWriter(new FileWriter(file.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mSensorManager != null) {
            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);


            try {
                output.append("## Note: lines start with two hash tags should be ignored when parsing sensor data.\n");
                output.append("## (Type, Name, Int Type, Resolution, Power)\n");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            for (Sensor sensor : sensors) {
                int delay = SensorManager.SENSOR_DELAY_UI;
                if (sensor.getType() == 21) {
                    delay = SensorManager.SENSOR_DELAY_FASTEST;
                }
                mSensorManager.registerListener(this, sensor, delay);
                String message = String.format("## Sensor found: (%s, %s, %d, %f, %f)",
                        sensor.getStringType(), sensor.getName(),
                        sensor.getType(), sensor.getResolution(), sensor.getPower());
                try {
                    output.append(message + "\n");
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                Log.i(TAG, message);
            }
        } else{
            Log.w(TAG, "No sensor manager found.");
        }

    }

    protected  void stopRecording() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }
        if (output != null) {
            try {
                output.flush();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(new Date());
//        Log.d(TAG, "Received sensor data " + event.sensor.getName() + " at " + timestamp + " = " + Arrays.toString(event.values));
        try {
            String[] values = new String[event.values.length];
            for (int i = 0; i < event.values.length; i++) {
                values[i] = "" + event.values[i];
            }
            output.append(
                    String.format("%s, %s, %d, %s\n",
                            event.sensor.getStringType(), timestamp,
                            event.accuracy, TextUtils.join(", ", values)));
            output.flush();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
//            mButton.setChecked(false); // TODO: change this

        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
        }
    }
}
