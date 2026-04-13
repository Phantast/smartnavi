package com.ilm.sandwich;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up custom top bar
        View topbarRoot = findViewById(R.id.topbar_root);
        TextView topbarTitle = findViewById(R.id.topbar_title);
        ImageButton topbarBack = findViewById(R.id.topbar_back);
        topbarTitle.setText(getResources().getString(R.string.tx_15));
        topbarBack.setOnClickListener(v -> finish());

        // Show overflow menu with "About" item
        ImageButton topbarOverflow = findViewById(R.id.topbar_overflow);
        topbarOverflow.setVisibility(View.VISIBLE);
        topbarOverflow.setColorFilter(android.graphics.Color.WHITE);
        topbarOverflow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(Settings.this, v);
            popup.getMenu().add(getResources().getString(R.string.tx_65));
            popup.setOnMenuItemClickListener(item -> {
                startActivity(new Intent(Settings.this, Info.class));
                return true;
            });
            popup.show();
        });

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(topbarRoot, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

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
            if (com.ilm.sandwich.tools.StepLengthCalculator.isImperial(stepLength)) {
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
            seekBarTimer.setThumb(ContextCompat.getDrawable(this, R.drawable.seek_thumb_normal));
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
                com.ilm.sandwich.tools.PreferencesHelper.putInt(Settings.this, "gpstimer", seekBar.getProgress());
                mFirebaseAnalytics.logEvent("Settings_Changed_Autocorrect_to_" + seekBar.getProgress(), null);
                // start Autocorrect after 3sek
                // because after this time the activity_settings are surely updated correctly
                GoogleMap.enableAutocorrectDelayed(3000);

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
                    float parsed = com.ilm.sandwich.tools.StepLengthCalculator.calculateStepLength(stepLengthString);
                    if (parsed > 0) {
                        Core.stepLength = parsed;
                        com.ilm.sandwich.tools.PreferencesHelper.putString(Settings.this, "step_length", stepLengthString);
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String key = "";

        // AutoCorrect
        if (buttonView.getId() == R.id.checkBoxGPS) {

            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

            final SeekBar seekBarTimer = findViewById(R.id.seekBarTimer);
            seekBarTimer.setEnabled(isChecked);

            final TextView timerText = findViewById(R.id.textTimer);
            if (isChecked) {
                seekBarTimer.setThumb(ContextCompat.getDrawable(this, R.drawable.seek_thumb_normal));
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
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(Settings.this, getApplicationContext().getResources().getString(R.string.tx_49), Toast.LENGTH_LONG).show();
                }

            } else {
                timerText.setText(getApplicationContext().getResources().getString(R.string.tx_25));
            }

            //Here AutoCorrect is enabled/disabled, IF is has been changed in the activity_settings
            //everything else is done via GoogleMapsActivity/... . onCreate()
            if (!isChecked) {
                // deactivate Autocorrect
                GoogleMap.disableAutocorrect();
            } else {
                // start Autocorrect after 2sec
                // because then settings are surely updated
                GoogleMap.enableAutocorrectDelayed(2000);
            }
            key = "autocorrect";

        } else if (buttonView.getId() == R.id.checkBoxExport) {
            if (isChecked) {
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
        com.ilm.sandwich.tools.PreferencesHelper.putBoolean(this, key, isChecked);
    }

    private void checkWriteStoragePermission(boolean isChecked) {
        // API 29+ uses MediaStore (no WRITE_EXTERNAL_STORAGE needed)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this,
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
            com.ilm.sandwich.tools.PreferencesHelper.putBoolean(this, key, isChecked);
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

}
