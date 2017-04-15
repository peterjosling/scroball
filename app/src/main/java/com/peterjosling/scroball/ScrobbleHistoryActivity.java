package com.peterjosling.scroball;

import android.os.Bundle;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class ScrobbleHistoryActivity extends AppCompatActivity {

  private ArrayAdapter adapter;
  private ScroballDB scroballDB;
  private List<Scrobble> scrobbles = new ArrayList<>();
  private LongSparseArray<Scrobble> scrobbleMap = new LongSparseArray<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_scrobble_history);

    scroballDB = ((ScroballApplication) getApplication()).getScroballDB();
    refreshData();

    adapter = new ScrobbleHistoryItemAdapter(this, android.R.layout.simple_list_item_1, scrobbles);
    ListView listView = (ListView) findViewById(R.id.scrobble_history_list_view);
    listView.setAdapter(adapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    ScroballApplication.getEventBus().register(this);
    refreshData();
  }

  @Override
  protected void onPause() {
    super.onPause();
    ScroballApplication.getEventBus().unregister(this);
  }

  @Subscribe
  private void onScrobbleDBUpdate(ScroballDBUpdateEvent event) {
    final Scrobble scrobble = event.scrobble();
    final long id = scrobble.status().getDbId();

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (scrobbleMap.get(id) != null) {
          scrobbleMap.get(id).status().setFrom(scrobble.status());
        } else {
          scrobbleMap.put(id, scrobble);
          adapter.insert(scrobble, 0);
        }

        adapter.notifyDataSetChanged();
      }
    });
  }

  private void refreshData() {
    scrobbles.clear();
    scrobbles.addAll(scroballDB.readScrobbles());

    for (Scrobble scrobble : scrobbles) {
      scrobbleMap.put(scrobble.status().getDbId(), scrobble);
    }
  }
}
