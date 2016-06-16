package com.peterjosling.scroball;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

  private ScroballApplication application;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    application = (ScroballApplication) getApplication();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settings_item:
        Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
        startActivityForResult(intent, 1);
        return true;
      case R.id.logout_item:
        logout();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public void logout() {
    new AlertDialog.Builder(this)
        .setTitle(R.string.are_you_sure)
        .setMessage(R.string.logout_confirm)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            SharedPreferences preferences = application.getSharedPreferences();
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(getString(R.string.saved_session_key));
            editor.apply();

            application.getScroballDB().clear();
          }
        })
        .setNegativeButton(android.R.string.no, null).show();
  }
}
