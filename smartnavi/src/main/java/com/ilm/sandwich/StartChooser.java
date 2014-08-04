package com.ilm.sandwich;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;


@SuppressLint("NewApi")
public class StartChooser extends Activity {

	private boolean playServicesAvaliable = true;
	private int surveyPopup = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Remove title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);	
		setContentView(R.layout.startchoose);    

		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		if (status != ConnectionResult.SUCCESS) {
			playServicesAvaliable = false;
		} else {
			playServicesAvaliable = true;
		}
		
		SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		surveyPopup = settings.getInt("surveyPopup1", 0);
		Log.d("egal", "Popup: "+surveyPopup);
		
		Handler waitingTimer = new Handler();
		waitingTimer.postDelayed(new Runnable() {			
			@Override
			public void run() {
				
				if(surveyPopup < 5){
					// Dialog for beta-Testers
					View betaFrame = (View) findViewById(R.id.betaDialogFrame);
					betaFrame.setVisibility(View.VISIBLE);
					
					Button cancel = (Button) findViewById(R.id.betaDialogButton2);
					cancel.setOnClickListener(new OnClickListener() {
						public void onClick(View arg0) {
							SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
							surveyPopup++;
							settings.edit().putInt("surveyPopup1", surveyPopup).commit();
							Log.d("egal", "Popup: "+surveyPopup);
							startMap();
						}
					});
					
					Button survey = (Button) findViewById(R.id.betaDialogButton1);
					survey.setOnClickListener(new OnClickListener() {
						public void onClick(View arg0) {
							SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
							settings.edit().putInt("surveyPopup1", 5).commit();
							Log.d("egal", "Ppopup: "+5);
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.tx_beta_url)));
							startActivity(browserIntent);
							finish();
						}
					});	
				}else{
					startMap();
				}
			}
		}, 1000);
	}

	
	private void startMap(){
		if(playServicesAvaliable){
		SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		if(settings.getString("MapSource", "GoogleMaps").equalsIgnoreCase("GoogleMaps")){
			startActivity(new Intent(StartChooser.this, GoogleMapActivity.class));
		}else{
			startActivity(new Intent(StartChooser.this, OsmMapActivity.class));
		}					
		}else{
			startActivity(new Intent(StartChooser.this, OsmMapActivity.class));
		}
		finish();
	}

}
