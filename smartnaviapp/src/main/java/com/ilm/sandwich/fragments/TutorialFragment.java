package com.ilm.sandwich.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.R;
import com.ilm.sandwich.sensors.Core;

import java.text.DecimalFormat;

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

        startTutorial();
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

    public void startTutorial() {
        welcomeView = fragmentView.findViewById(R.id.welcomeView);
        welcomeView.setVisibility(View.VISIBLE);

        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, null);

        Button welcomeButton = (Button) fragmentView.findViewById(R.id.welcomeButton);
        welcomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFirebaseAnalytics.logEvent("Tutorial_Button1_pressed", null);
                welcomeView.setVisibility(View.INVISIBLE);
                tutorialOverlay = fragmentView.findViewById(R.id.tutorialOverlay);
                tutorialOverlay.setVisibility(View.VISIBLE);
            }
        });

        SharedPreferences settings = this.getActivity().getSharedPreferences(this.getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String stepLengthString = settings.getString("step_length", null);
        Spinner spinner = (Spinner) fragmentView.findViewById(R.id.tutorialSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.dimension, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        if (stepLengthString != null) {
            try {
                stepLengthString = stepLengthString.replace(",", ".");
                int savedBodyHeight = Integer.parseInt(stepLengthString);
                if (savedBodyHeight < 241 && savedBodyHeight > 119) {
                    EditText editText = (EditText) fragmentView.findViewById(R.id.tutorialEditText);
                    editText.setText(savedBodyHeight);
                    spinner.setSelection(0);
                } else if (savedBodyHeight < 95 && savedBodyHeight > 45) {
                    EditText editText = (EditText) fragmentView.findViewById(R.id.tutorialEditText);
                    editText.setText(savedBodyHeight);
                    spinner.setSelection(1);
                }
            } catch (Exception e) {
                if (BuildConfig.debug)
                    e.printStackTrace();
            }
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    metricUnits = true;
                } else {
                    metricUnits = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        Button startButton = (Button) fragmentView.findViewById(R.id.startbutton);
        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                mFirebaseAnalytics.logEvent("Tutorial_Button2_pressed", null);
                boolean tutorialDone = false;
                final EditText heightField = (EditText) fragmentView.findViewById(R.id.tutorialEditText);
                int op = heightField.length();
                float number;
                if (op != 0) {
                    try {
                        number = Float.valueOf(heightField.getText().toString());
                        if (number < 241 && number > 119 && metricUnits) {
                            String numberString = df0.format(number);
                            fragmentView.getContext().getSharedPreferences(fragmentView.getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE).edit().putString("step_length", numberString).commit();
                            Core.stepLength = (number / 222);
                            tutorialDone = true;
                        } else if (number < 95 && number > 45 && !metricUnits) {
                            String numberString = df0.format(number);
                            fragmentView.getContext().getSharedPreferences(TutorialFragment.this.getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE).edit().putString("step_length", numberString).apply();
                            Core.stepLength = (float) (number * 2.54 / 222);
                            tutorialDone = true;
                        } else {
                            Toast.makeText(fragmentView.getContext(), fragmentView.getContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
                        }
                    } catch (NumberFormatException e) {
                        if (BuildConfig.debug)
                            Toast.makeText(fragmentView.getContext(), fragmentView.getContext().getResources().getString(R.string.tx_32), Toast.LENGTH_LONG).show();
                    }
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

        EditText bodyHeightField = (EditText) fragmentView.findViewById(R.id.tutorialEditText);
        bodyHeightField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT) {
                    try {
                        InputMethodManager inputManager = (InputMethodManager) fragmentView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        try {
                            inputManager.hideSoftInputFromWindow(fragmentView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        EditText bodyHeightField = (EditText) fragmentView.findViewById(R.id.tutorialEditText); //Workaround Coursor out off textfield
                        bodyHeightField.setFocusable(false);
                        bodyHeightField.setFocusableInTouchMode(true);
                        bodyHeightField.setFocusable(true);
                    } catch (Exception e) {
                        if (BuildConfig.debug)
                            e.printStackTrace();
                    }
                }
                return false;
            }
        });
    }

    public interface onTutorialFinishedListener {
        void onTutorialFinished();
    }
}
