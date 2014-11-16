package com.ilm.sandwich;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class Info extends SherlockActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView betaTest = (TextView) findViewById(R.id.textBetaCom);
        Spanned spanned = Html.fromHtml(getString(R.string.tx_89));
        betaTest.setMovementMethod(LinkMovementMethod.getInstance());
        betaTest.setText(spanned);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return (true);
            default:
                return super.onOptionsItemSelected(item);
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
