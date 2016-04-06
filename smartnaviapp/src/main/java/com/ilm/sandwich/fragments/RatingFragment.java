package com.ilm.sandwich.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.ilm.sandwich.R;
import com.ilm.sandwich.tools.Analytics;
import com.ilm.sandwich.tools.Config;

/**
 * Fragment to show Rating Dialog.
 *
 * @author Christian
 */
public class RatingFragment extends Fragment {

    private onRatingFinishedListener mListener;
    private View fragmentView;
    private Analytics mAnalytics;

    public RatingFragment() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentView = view;
        SharedPreferences settings = this.getActivity().getSharedPreferences(this.getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        boolean trackingAllowed = settings.getBoolean("nutzdaten", true);
        mAnalytics = new Analytics(trackingAllowed);
        showRateDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rating, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onRatingFinishedListener) {
            mListener = (onRatingFinishedListener) context;
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

    private void showRateDialog() {
        mAnalytics.trackEvent("Rating_Dialog_View", "View");
        Button rateButton1 = (Button) fragmentView.findViewById(R.id.rateButton);
        if (rateButton1 != null) {
            rateButton1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAnalytics.trackEvent("Rating_Dialog_Action", "No");
                    SharedPreferences prefs = RatingFragment.this.getActivity().getSharedPreferences(RatingFragment.this.getActivity().getPackageName() + "_preferences", 0);
                    int notRated = prefs.getInt("not_rated", 0) + 1;

                    new changeSettings("not_rated", notRated).execute();

                    if (notRated == 1) {
                        new changeSettings("launch_count", -6).execute();
                    } else if (notRated == 2) {
                        new changeSettings("launch_count", -8).execute();
                    } else if (notRated == 3) {
                        new changeSettings("launch_count", -10).execute();
                    } else if (notRated == 4) {
                        new changeSettings("dontshowagain", true).execute();
                    }
                    if (mListener != null) {
                        mListener.onRatingFinished();
                    }
                }
            });
        }
        Button rateButton3 = (Button) fragmentView.findViewById(R.id.rateButton2);
        if (rateButton3 != null) {
            rateButton3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAnalytics.trackEvent("Rating_Dialog_Action", "Yes_Button");
                    new changeSettings("not_rated", 999).execute();
                    new changeSettings("dontshowagain", true).execute();
                    if (mListener != null) {
                        mListener.onRatingFinished();
                    }
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
                }
            });
        }

        ImageView stars = (ImageView) fragmentView.findViewById(R.id.stars);
        if (stars != null) {
            stars.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new changeSettings("not_rated", 999).execute();
                    mAnalytics.trackEvent("Rating_Dialog_Action", "Yes_Stars");
                    new changeSettings("dontshowagain", true).execute();
                    if (mListener != null) {
                        mListener.onRatingFinished();
                    }
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
                }
            });
        }
    }

    public interface onRatingFinishedListener {
        void onRatingFinished();
    }

    private class changeSettings extends AsyncTask<Void, Void, Void> {

        private String key;
        private int dataType;
        private boolean setting1;
        private int setting3;

        private changeSettings(String key, boolean setting1) {
            this.key = key;
            this.setting1 = setting1;
            dataType = 0;
        }

        private changeSettings(String key, int setting3) {
            this.key = key;
            this.setting3 = setting3;
            dataType = 2;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences settings = RatingFragment.this.getActivity().getSharedPreferences(RatingFragment.this.getActivity().getPackageName() + "_preferences", Context.MODE_PRIVATE);
            if (dataType == 0) {
                settings.edit().putBoolean(key, setting1).apply();
            } else if (dataType == 2) {
                settings.edit().putInt(key, setting3).apply();
            }
            return null;
        }
    }
}
