// Copyright 2020 tinaxd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package work.tinax.pandaroid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import work.tinax.pandagui.Kadai;
import work.tinax.pandagui.KadaiBuilder;
import work.tinax.pandagui.PandAAPI;
import work.tinax.pandagui.PandAAPIException;
import work.tinax.pandagui.SiteIdInfo;

public class MainActivity extends AppCompatActivity {

    private List<Kadai> kadaiList;
    private KadaiAdapter kadaiAdapter;

    private Executor networkExecutor;

    private void toggleButtons(boolean enabled) {
        ((Button)findViewById(R.id.updateButton)).setEnabled(enabled);
        ((Button)findViewById(R.id.settingsButton)).setEnabled(enabled);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        networkExecutor = Executors.newSingleThreadExecutor();
        kadaiList = new ArrayList<>();

        RecyclerView listView = findViewById(R.id.kadaiList);
        listView.setLayoutManager(new LinearLayoutManager(this));
        kadaiAdapter = new KadaiAdapter(kadaiList);
        listView.setAdapter(kadaiAdapter);

        ((Button)findViewById(R.id.updateButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final LoginInfo login = getLoginInfoFromSharedPreferences();

                // show error message if id or password is not set.
                if (login == null) {
                    Toast.makeText(getApplicationContext(), "Please set ID and password", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                networkExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final Handler mainHandler = new Handler(getMainLooper());
                        List<Kadai> kadais = new ArrayList<>();
                        LocalDateTime now = LocalDateTime.now();
                        Log.i("update kadai", "PandA access starts");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "接続開始", Toast.LENGTH_SHORT).show();
                                toggleButtons(false);
                            }
                        });
                        try (PandAAPI api = PandAAPI.newLogin(login.getId(), login.getPassword())) {
                            kadais.clear();
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "ログイン完了", Toast.LENGTH_SHORT).show();
                                }
                            });
                            for (final SiteIdInfo site : api.fetchSiteIds()) {
                                Log.i("update kadai", "fetching " + site.getSiteId());
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), site.getLectureName() + "を取得中", Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                });
                                for (Kadai kadai : api.getAssignments(site.getSiteId())) {
                                    // skip if due is before now
                                    if (kadai.getDue().isAfter(now)) {
                                        kadais.add(kadai);
                                    }
                                }
                            }
                        } catch (PandAAPIException ex) {
                            Toast.makeText(getApplicationContext(), "通信エラー. ログイン情報を確認してください.", Toast.LENGTH_LONG)
                                    .show();
                        } finally {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    toggleButtons(true);
                                }
                            });
                        }
                        Log.i("update kadai", "PandA access ends");

                        // sort by due (ascending order)
                        kadais.sort(new Comparator<Kadai>() {
                            @Override
                            public int compare(Kadai kadai, Kadai t1) {
                                return kadai.getDue().compareTo(t1.getDue());
                            }
                        });
                        updateKadaiList(kadais);

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "課題更新完了. 何も表示されない場合は本当に課題がないか, ログイン情報が間違っています.", Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
                    }
                });
            }
        });

        ((Button)findViewById(R.id.settingsButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, LoginInfoSettings.class);
                startActivity(intent);
            }
        });

        //updateKadaiList(makeSampleKadai());
    }

    public void updateKadaiList(List<Kadai> kadais) {
        kadaiList.clear();
        kadaiList.addAll(kadais);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                kadaiAdapter.notifyDataSetChanged();
            }
        });
    }

    private LoginInfo getLoginInfoFromSharedPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String id = sp.getString("id", null);
        String password = sp.getString("password", null);
        if (id == null || password == null) {
            return null;
        }
        return new LoginInfo(id, password);
    }

    private List<Kadai> makeSampleKadai() {
        List<Kadai> kadais = new ArrayList<>();
        kadais.add(new KadaiBuilder()
                .title("レポートあいうえお")
                .lecture("実験")
                .id("ID-1-2-3")
                .due(LocalDateTime.now().plusDays(2))
                .description("レポートの説明です")
                .build());
        kadais.add(new KadaiBuilder()
                .title("かきくけこ課題")
                .lecture("数学")
                .id("ID-EXP-143")
                .due(LocalDateTime.now().plusDays(3))
                .description("さしすせそ")
                .build());
        return kadais;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateKadai() {

    }
}