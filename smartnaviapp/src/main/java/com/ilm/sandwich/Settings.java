package com.ilm.sandwich;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ilm.sandwich.sensors.Core;
import com.ilm.sandwich.tools.Config;

import java.text.DecimalFormat;

/**
 * @author Christian Henke
 *         https://smartnavi.app
 */
public class Settings extends AppCompatActivity implements OnEditorActionListener, OnCheckedChangeListener {

    static DecimalFormat df = new DecimalFormat("0");
    EditText editText;
    CheckBox checkBoxVibration;
    CheckBox checkBoxSatellite;
    CheckBox checkBoxSpeech;
    CheckBox checkBoxGPS;
    CheckBox checkBoxExport;
    private boolean metricUnits = true;
    private LocationManager mLocationManager;
    private SubMenu subMenu1;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getResources().getString(R.string.tx_15));
        } catch (Exception e) {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_settings);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        editText = findViewById(R.id.editText);
        checkBoxVibration = findViewById(R.id.checkBoxVibration);
        checkBoxSatellite = findViewById(R.id.checkBoxSatellite);
        checkBoxSpeech = findViewById(R.id.checkBoxSpeech);
        checkBoxGPS = findViewById(R.id.checkBoxGPS);
        final SeekBar seekBarTimer = findViewById(R.id.seekBarTimer);
        checkBoxExport = findViewById(R.id.checkBoxExport);
        final TextView timerText = findViewById(R.id.textTimer);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        String stepLength = settings.getString("step_length", null);
        if (stepLength != null) {
            if (stepLength.contains("'")) {
                metricUnits = false;
            }
            editText.setText(stepLength);
        }

        boolean vibration = settings.getBoolean("vibration", true);
        checkBoxVibration.setChecked(vibration);

        boolean view = settings.getBoolean("view", false);
        checkBoxSatellite.setChecked(view);

        boolean speech = settings.getBoolean("language", false);
        checkBoxSpeech.setChecked(speech);

        boolean export = settings.getBoolean("export", false);
        checkBoxExport.setChecked(export);

        boolean autocorrect = settings.getBoolean("autocorrect", false);
        checkBoxGPS.setChecked(autocorrect);

        int gpsTimer = settings.getInt("gpstimer", 1);
        seekBarTimer.setEnabled(autocorrect);
        if (autocorrect) {
            seekBarTimer.setThumb(getResources().getDrawable(R.drawable.seek_thumb_normal));
            if (gpsTimer == 0) {
                seekBarTimer.setProgress(0);
                timerText.setText(getApplicationContext().getResources().getString(R.string.tx_75));
            } else if (gpsTimer == 1) {
                seekBarTimer.setProgress(1);
                timerText.setText(getApplicationContext().getResources().getString(R.string.tx_76));
            } else {
                seekBarTimer.setProgress(2);
                timerText.setText(getApplicationContext().getResources().getString(R.string.tx_80));
            }
        } else {
            timerText.setText(getApplicationContext().getResources().getString(R.string.tx_25));
        }

        checkBoxVibration.setOnCheckedChangeListener(this);
        checkBoxSatellite.setOnCheckedChangeListener(this);
        checkBoxSpeech.setOnCheckedChangeListener(this);
        checkBoxExport.setOnCheckedChangeListener(this);

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (!metricUnits) {
                    if (event.getKeyCode() != KeyEvent.KEYCODE_DEL) {
                        String input = editText.getText().toString();
                        if (input.length() == 1) {
                            input = input + "'";
                            editText.setText(input);
                            int pos = editText.getText().length();
                            editText.setSelection(pos);
                        }
                    }
                }
                return false;
            }
        });

        editText.setOnEditorActionListener(this);
        checkBoxGPS.setOnCheckedChangeListener(this);
        seekBarTimer.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    timerText.setText(getApplicationContext().getResources().getString(R.string.tx_75));
                } else if (progress == 1) {
                    timerText.setText(getApplicationContext().getResources().getString(R.string.tx_76));
                } else {
                    timerText.setText(getApplicationContext().getResources().getString(R.string.tx_80));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                new writeSettings("gpstimer", seekBar.getProgress()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                mFirebaseAnalytics.logEvent("Settings_Changed_Autocorrect_to_" + seekBar.getProgress(), null);
                // start Autocorrect after 3sek
                // because after this time the activity_settings are surely updated correctly
                try {
                    GoogleMap.listHandler.sendEmptyMessageDelayed(6, 3000);
                } catch (Exception e) {
                    //Happens if user switched MapSource BEFORE enabling AutoCorrect
                    //Because then, the requested Activity does not exist UNTIL user leaves Settings
                    //No Problem, just ignore this case.
                }

            }
        });
    }


    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT) {
            int op = editText.length();
            String stepLengthString = editText.getText().toString();
            if (op != 0) {
                try {
                    if (stepLengthString.contains("'")) {
                        String[] feetInchString = stepLengthString.split("'");
                        String feetString = feetInchString[0];
                        float feet = Float.valueOf(feetString);

                        //Check if user provided inch, if so set that. If not assume 0
                        float inch = 0;
                        if (feetInchString.length > 1) {
                            String inchString = feetInchString[1];
                            inch = Float.valueOf(inchString);
                        } else {
                            inch = 0;
                        }
                        float totalInch = 12 * feet + inch;
                        Core.stepLength = (float) (totalInch * 2.54 / 222);
                        new writeSettings("step_length", stepLengthString).execute();
                        mFirebaseAnalytics.logEvent("Settings_Changed_Bodyheight", null);
                    } else {
                        stepLengthString = stepLengthString.replace(",", ".");
                        float number = Float.valueOf(stepLengthString);
                        String numberString = df.format(number);
                        Core.stepLength = (number / 222);
                        new writeSettings("step_length", numberString).execute();
                        mFirebaseAnalytics.logEvent("Settings_Changed_Bodyheight", null);
                    }
                    // close Keyboard after pressing the button
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    editText.setFocusableInTouchMode(false); // Workaround: Cursor out of textfield
                    editText.setFocusable(false);
                    editText.setFocusableInTouchMode(true);
                    editText.setFocusable(true);
                } catch (NumberFormatException e) {
                    // close Keyboard after pressing the button
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    editText.setFocusableInTouchMode(false); // Workaround: Cursor out of textfield
                    editText.setFocusable(false);
                    editText.setFocusableInTouchMode(true);
                    editText.setFocusable(true);
                    Toast.makeText(Settings.this, getApplicationContext().getResources().getString(R.string.tx_32), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(Settings.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
                // close Keyboard after pressing the button
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                editText.setFocusableInTouchMode(false); // Workaround: Cursor out of textfield
                editText.setFocusable(false);
                editText.setFocusableInTouchMode(true);
                editText.setFocusable(true);
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        subMenu1 = menu.addSubMenu(0, 3, 3, "").setIcon(R.drawable.ic_menu_moreoverflow_normal_holo_dark);
        subMenu1.add(0, 7, 7, getApplicationContext().getResources().getString(R.string.tx_65));

        MenuItem subMenu1Item = subMenu1.getItem();
        subMenu1Item.setIcon(R.drawable.ic_menu_moreoverflow_normal_holo_dark);
        subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 7:
                startActivity(new Intent(this, Info.class));
                return true;
            case android.R.id.home:
                // back
                finish();
                return (true);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String key = "";

        // AutoCorrect
        if (buttonView.getId() == R.id.checkBoxGPS) {

            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

            final SeekBar seekBarTimer = findViewById(R.id.seekBarTimer);
            seekBarTimer.setEnabled(isChecked);

            final TextView timerText = findViewById(R.id.textTimer);
            if (isChecked == true) {
                seekBarTimer.setThumb(getResources().getDrawable(R.drawable.seek_thumb_normal));
                int gpsTimer = settings.getInt("gpstimer", 1);
                if (gpsTimer == 0) {
                    timerText.setText(getApplicationContext().getResources().getString(R.string.tx_75));
                } else if (gpsTimer == 1) {
                    timerText.setText(getApplicationContext().getResources().getString(R.string.tx_76));
                } else {
                    timerText.setText(getApplicationContext().getResources().getString(R.string.tx_80));
                }

                // check is GPS is allowed/enabled, if not: give a warning
                mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
                    Toast.makeText(Settings.this, getApplicationContext().getResources().getString(R.string.tx_49), Toast.LENGTH_LONG).show();
                }

            } else {
                timerText.setText(getApplicationContext().getResources().getString(R.string.tx_25));
            }

            //Here AutoCorrect is enabled/disabled, IF is has been changed in the activity_settings
            //everything else is done via GoogleMapsActivity/... . onCreate()
            if (isChecked == false) {
                // deactivate Autocorrect
                GoogleMap.listHandler.sendEmptyMessage(7);
            } else {
                // start Autocorrect anwerfern after 3sek
                // because then activity_settings are surely updated
                GoogleMap.listHandler.sendEmptyMessageDelayed(6, 2000);
            }
            key = "autocorrect";

        } else if (buttonView.getId() == R.id.checkBoxExport) {
            if (isChecked == true) {
                //Check permission before starting export
                checkWriteStoragePermission(isChecked);
                return;
            }
        } else if (buttonView.getId() == R.id.checkBoxSatellite) {
            key = "view";
        } else if (buttonView.getId() == R.id.checkBoxSpeech) {
            key = "language";
        } else if (buttonView.getId() == R.id.checkBoxVibration) {
            key = "vibration";
        }
        mFirebaseAnalytics.logEvent("Settings_" + key + "_changed_to_" + isChecked, null);
        new writeSettings(key, isChecked).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void checkWriteStoragePermission(boolean isChecked) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Config.PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {
            String key = "export";
            if (isChecked) {
                Toast.makeText(Settings.this, getResources().getString(R.string.tx_88), Toast.LENGTH_LONG).show();
            }
            new writeSettings(key, isChecked).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_101), Toast.LENGTH_LONG).show();
        }
    }

    private class writeSettings extends AsyncTask<Void, Void, Void> {

        private String key;
        private boolean setting1;
        private String setting2;
        private int setting3 = 0;
        private int dataType = 0;

        private writeSettings(String key, boolean setting1) {
            this.key = key;
            this.setting1 = setting1;
            dataType = 0;
        }

        private writeSettings(String key, String setting2) {
            this.key = key;
            this.setting2 = setting2;
            dataType = 1;
        }

        private writeSettings(String key, int setting3) {
            this.key = key;
            this.setting3 = setting3;
            dataType = 2;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
            if (dataType == 0) {
                settings.edit().putBoolean(key, setting1).commit();
            } else if (dataType == 1) {
                settings.edit().putString(key, setting2).commit();
            } else if (dataType == 2) {
                settings.edit().putInt(key, setting3).commit();
            }
            return null;
        }
    }

}
