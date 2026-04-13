package com.ilm.sandwich;


import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ilm.sandwich.fragments.RatingFragment;


/**
 * @author Christian Henke
 *         https://smartnavi.app
 */
public class Info extends AppCompatActivity implements RatingFragment.onRatingFinishedListener {

    private FirebaseAnalytics mFirebaseAnalytics;
    private RatingFragment ratingFragment;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // Set up custom top bar
        View topbarRoot = findViewById(R.id.topbar_root);
        TextView topbarTitle = findViewById(R.id.topbar_title);
        ImageButton topbarBack = findViewById(R.id.topbar_back);
        topbarTitle.setText(getResources().getString(R.string.tx_65));
        topbarBack.setOnClickListener(v -> finish());

        // Handle edge-to-edge insets: add top padding to the topbar root
        ViewCompat.setOnApplyWindowInsetsListener(topbarRoot, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        TextView versionNameText = findViewById(R.id.versionName);
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = pInfo.versionName;
            versionNameText.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Secret debug trigger: tap the SmartNavi icon to show rating dialog
        if (BuildConfig.debug) {
            ImageView icon = findViewById(R.id.icon);
            icon.setOnClickListener(v -> showTestRatingDialog());
        }
    }

    /**
     * Shows the rating dialog for testing purposes (debug builds only).
     */
    private void showTestRatingDialog() {
        // Don't show if already showing
        if (ratingFragment != null && ratingFragment.isAdded()) {
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        ratingFragment = new RatingFragment();
        fragmentTransaction.add(R.id.rating_container, ratingFragment).commitAllowingStateLoss();
    }

    @Override
    public void onRatingFinished() {
        if (ratingFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(ratingFragment).commit();
            ratingFragment = null;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

}
