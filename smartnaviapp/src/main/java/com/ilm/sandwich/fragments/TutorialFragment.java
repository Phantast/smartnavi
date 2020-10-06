package com.ilm.sandwich.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.R;
import com.ilm.sandwich.sensors.Core;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Fragment to show TutorialFragment for first app start or if requested by user.
 */
public class TutorialFragment extends Fragment {

    static DecimalFormat df0 = new DecimalFormat("0");
    private onTutorialFinishedListener mListener;
    private View tutorialOverlay;
    private View welcomeView;
    private boolean metricUnits = true;
    private View fragmentView;
    private FirebaseAnalytics mFirebaseAnalytics;

    public TutorialFragment() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentView = view;

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(view.getContext());
        mFirebaseAnalytics.logEvent("Tutorial_Start", null);

        startTutorial(view);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tutorial, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onTutorialFinishedListener) {
            mListener = (onTutorialFinishedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void startTutorial(View view) {
        //Remote Config for AB Testing for Tutorial Wording
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        boolean showTutorialPage1 = mFirebaseRemoteConfig.getBoolean("tutorial_page1_shown");
        Log.i("Tutorial AB Test", "showTutorialPage1 = " + showTutorialPage1);
        if (showTutorialPage1) {
            //AB TEst about hiding the first page, proceed normally and show page 1
            welcomeView = view.findViewById(R.id.welcomeView);
            welcomeView.setVisibility(View.VISIBLE);

            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, null);

            Button welcomeButton = view.findViewById(R.id.welcomeButton);
            welcomeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mFirebaseAnalytics.logEvent("Tutorial_Button1_pressed", null);
                    welcomeView.setVisibility(View.INVISIBLE);
                    tutorialOverlay = fragmentView.findViewById(R.id.tutorialOverlay);
                    tutorialOverlay.setVisibility(View.VISIBLE);
                }
            });

        } else {
            //AB TEst with hiding the first page, just show page 2
            tutorialOverlay = fragmentView.findViewById(R.id.tutorialOverlay);
            tutorialOverlay.setVisibility(View.VISIBLE);
        }


        SharedPreferences settings = this.getActivity().getSharedPreferences(this.getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String stepLengthString = settings.getString("step_length", null);
        Spinner spinner = view.findViewById(R.id.tutorialSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.dimension, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        if (stepLengthString != null) {
            try {
                if (stepLengthString.contains("'")) {
                    EditText editText = view.findViewById(R.id.tutorialEditText);
                    editText.setText(stepLengthString, TextView.BufferType.EDITABLE);
                    spinner.setSelection(1);
                } else {
                    stepLengthString = stepLengthString.replace(",", ".");
                    int savedBodyHeight = Integer.parseInt(stepLengthString);
                    String savedBodyHeightString = "" + savedBodyHeight;
                    EditText editText = view.findViewById(R.id.tutorialEditText);
                    editText.setText(savedBodyHeightString, TextView.BufferType.EDITABLE);
                    spinner.setSelection(0);
                }
            } catch (Exception e) {
                    e.printStackTrace();
            }
        } else {
            //For US users switch to foot inch bodyheight
            Log.i("Language", "" + Locale.getDefault().getISO3Country());
            if (Locale.getDefault().getISO3Country().contains("USA")) {
                spinner.setSelection(1, false);
                metricUnits = false;
            }
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) metricUnits = true;
                else metricUnits = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        Button startButton = view.findViewById(R.id.startbutton);
        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                mFirebaseAnalytics.logEvent("Tutorial_Button2_pressed", null);
                boolean tutorialDone = false;
                final EditText heightField = fragmentView.findViewById(R.id.tutorialEditText);
                int op = heightField.length();
                float number;
                if (op != 0) {
                    if (metricUnits) {
                        try {
                            number = Float.valueOf(heightField.getText().toString());
                            if (number < 241 && number > 49) {
                                String numberString = df0.format(number);
                                fragmentView.getContext().getSharedPreferences(fragmentView.getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE).edit().putString("step_length", numberString).commit();
                                Core.stepLength = (number / 222);
                                tutorialDone = true;
                            } else {
                                Toast.makeText(fragmentView.getContext(), fragmentView.getContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
                            }
                        } catch (NumberFormatException e) {
                            if (BuildConfig.debug)
                                Toast.makeText(fragmentView.getContext(), fragmentView.getContext().getResources().getString(R.string.tx_32), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        try {
                            String numberString = heightField.getText().toString();
                            fragmentView.getContext().getSharedPreferences(TutorialFragment.this.getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE).edit().putString("step_length", numberString).apply();
                            String[] feetInchString = numberString.split("'");
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
                            tutorialDone = true;
                        } catch (NumberFormatException e) {
                            if (BuildConfig.debug)
                                Toast.makeText(fragmentView.getContext(), fragmentView.getContext().getResources().getString(R.string.tx_32), Toast.LENGTH_LONG).show();
                        }
                    }
                    if (BuildConfig.DEBUG) Log.i("Step length", "Step length = " + Core.stepLength);
                } else {
                    Toast.makeText(fragmentView.getContext(), fragmentView.getContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
                }

                if (tutorialDone) {
                    // TutorialFragment finished
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, null);
                    if (mListener != null) {
                        mListener.onTutorialFinished();
                    }
                }
            }
        });

        final EditText bodyHeightField = view.findViewById(R.id.tutorialEditText);
        bodyHeightField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                EditText bodyHeightField = fragmentView.findViewById(R.id.tutorialEditText);
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT) {
                    try {
                        InputMethodManager inputManager = (InputMethodManager) fragmentView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        try {
                            inputManager.hideSoftInputFromWindow(fragmentView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //Workaround Coursor out off textfield
                        bodyHeightField.setFocusable(false);
                        bodyHeightField.setFocusableInTouchMode(true);
                        bodyHeightField.setFocusable(true);
                    } catch (Exception e) {
                            e.printStackTrace();
                    }
                }
                return false;
            }
        });

        bodyHeightField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (!metricUnits) {
                    if (event.getKeyCode() != KeyEvent.KEYCODE_DEL) {
                        String input = bodyHeightField.getText().toString();
                        if (input.length() == 1) {
                            input = input + "'";
                            bodyHeightField.setText(input);
                            int pos = bodyHeightField.getText().length();
                            bodyHeightField.setSelection(pos);
                        }
                    }
                }
                return false;
            }
        });

        //If remoteConfig String is NOT EMPTY, then use it.
        //Page1
        if (!mFirebaseRemoteConfig.getString("step1_text1").equalsIgnoreCase("")) {
            TextView s1t1 = view.findViewById(R.id.welcomeNumber1Text);
            s1t1.setText(mFirebaseRemoteConfig.getString("step1_text1"));
        }
        if (!mFirebaseRemoteConfig.getString("step1_text2").equalsIgnoreCase("")) {
            TextView s1t1 = view.findViewById(R.id.welcomeNumber2TextBarText);
            s1t1.setText(mFirebaseRemoteConfig.getString("step1_text2"));
        }
        if (!mFirebaseRemoteConfig.getString("step1_text3").equalsIgnoreCase("")) {
            TextView s1t1 = view.findViewById(R.id.welcomeNumber4Text);
            s1t1.setText(mFirebaseRemoteConfig.getString("step1_text3"));
        }
        //Page 2
        if (!mFirebaseRemoteConfig.getString("step2_text1").equalsIgnoreCase("")) {
            TextView s1t1 = view.findViewById(R.id.tutorialNumber1Text);
            s1t1.setText(mFirebaseRemoteConfig.getString("step2_text1"));
        }
        if (!mFirebaseRemoteConfig.getString("step2_text2").equalsIgnoreCase("")) {
            TextView s1t1 = view.findViewById(R.id.tutorialNumber2TextBarText);
            s1t1.setText(mFirebaseRemoteConfig.getString("step2_text2"));
        }
        if (!mFirebaseRemoteConfig.getString("step2_text3").equalsIgnoreCase("")) {
            TextView s1t1 = view.findViewById(R.id.tutorialNumber4Text);
            s1t1.setText(mFirebaseRemoteConfig.getString("step2_text3"));
        }
    }

    public interface onTutorialFinishedListener {
        void onTutorialFinished();
    }
}
