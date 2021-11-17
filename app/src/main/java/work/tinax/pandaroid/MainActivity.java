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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import work.tinax.pandagui.Kadai;
import work.tinax.pandagui.KadaiBuilder;
import work.tinax.pandagui.PandAAPI;
import work.tinax.pandagui.PandAAPIException;
import work.tinax.pandagui.SiteIdInfo;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Kadai> kadaiList;
    private KadaiAdapter kadaiAdapter;

    private Executor networkExecutor;

    private LocalDateTime updateTime;

    public static final Version PANDAROID_VERSION = new Version(0, 3, 0);

    private StatusTextManager statusTextManager;

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

        statusTextManager = new StatusTextManager(findViewById((R.id.stateText)));

        ((Button)findViewById(R.id.updateButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final LoginInfo login = getLoginInfoFromSharedPreferences();

                // show error message if id or password is not set.
                if (login == null) {
                    Toast.makeText(getApplicationContext(), "Please set ID and password", Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                networkExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final Handler mainHandler = new Handler(getMainLooper());
                        final ArrayList<Kadai> kadais = new ArrayList<>();
                        LocalDateTime now = LocalDateTime.now();
                        Log.i("update kadai", "PandA access starts");
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                statusTextManager.showText("接続開始");
                                toggleButtons(false);
                            }
                        });
                        try (PandAAPI api = PandAAPI.newLogin(login.getId(), login.getPassword())) {
                            kadais.clear();
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    statusTextManager.showText("ログイン完了");
                                }
                            });
                            for (final SiteIdInfo site : api.fetchSiteIds()) {
                                Log.i("update kadai", "fetching " + site.getSiteId());
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        statusTextManager.showText(site.getLectureName() + "を取得中");
                                    }
                                });
                                for (Kadai kadai : api.getAssignments(site.getSiteId())) {
                                    // skip if due is before now
                                    if (kadai.getDue().isAfter(now)) {
                                        kadais.add(kadai);
                                    }
                                }
                                for (Kadai quiz : api.getQuiz(site.getSiteId())) {
                                    // skip if due is before now
                                    if (quiz.getDue().isAfter(now)) {
                                        kadais.add(quiz);
                                    }
                                }
                            }
                        } catch (PandAAPIException ex) {
                            mainHandler.post(() -> {
                                    statusTextManager.showText("通信エラー. ログイン情報を確認してください.");
                            });
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
                                statusTextManager.showText("課題更新完了. 何も表示されない場合は本当に課題がないか, ログイン情報が間違っています.");
                                updateTime = LocalDateTime.now();
                                showUpdateTime(updateTime);
                                Log.i("update kadai", "saving kadai list to cache");
                                saveKadaiList(kadais, updateTime);
                                Log.i("update kadai", "saved kadai list to cache");
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

        if (updateKadaiListFromCache()) {
            statusTextManager.showText("キャッシュを読み込みました");
        }

        UpdateChecker checker = new UpdateChecker(PANDAROID_VERSION);
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean updateAvailable = checker.isNewVersionAvailable().get();
                    Log.i("update checker", "updateAvailable: " + updateAvailable);
                    if (updateAvailable) {
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                statusTextManager.showText("PandAroid の更新があります. https://tinaxd.github.io/PandAroid-updates");
                            }
                        });
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showUpdateTime(LocalDateTime time) {
        ((TextView)findViewById(R.id.listUpdateTime)).setText("取得: " + time.format(DateTimeFormatter.ofPattern("y/M/d HH:mm")));
    }

    public boolean updateKadaiListFromCache() {
        KadaisWithTime kadaist = loadKadaiList();
        if (kadaist == null) {
            return false;
        }
        updateKadaiList(kadaist.getKadais());
        updateTime = kadaist.getTime();
        showUpdateTime(updateTime);
        return true;
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
                .due(LocalDateTime.now().plusHours(12))
                .description("レポートの説明です")
                .build());
        kadais.add(new KadaiBuilder()
                .title("かきくけこ課題")
                .lecture("数学")
                .id("ID-EXP-143")
                .due(LocalDateTime.now().plusDays(3))
                .description("さしすせそ")
                .build());
        kadais.add(new KadaiBuilder()
                .title("さしすせそ課題")
                .lecture("英語")
                .id("ID-EXP-123")
                .due(LocalDateTime.now().plusDays(13))
                .description("まだ二週間もある")
                .build());
        kadais.add(new KadaiBuilder()
                .title("小テスト")
                .lecture("フランス語")
                .id("ID-EXP-FR")
                .due(LocalDateTime.now().plusDays(21))
                .description("3週間後です")
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

    private final String CACHE_FILENAME = "kadaiCache";

    private void saveKadaiList(KadaisWithTime kadaist) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                getApplicationContext().openFileOutput(CACHE_FILENAME, Context.MODE_PRIVATE)
        )) {
            oos.writeObject(kadaist);
            Log.i("saveKadaiList", "wrote kadai list");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("saveKadaiList", e.getMessage());
        }
    }

    private void saveKadaiList(ArrayList<Kadai> kadais, LocalDateTime updateTime) {
        saveKadaiList(new KadaisWithTime(kadais, updateTime));
    }

    public KadaisWithTime loadKadaiList() {
        try (ObjectInputStream ois = new ObjectInputStream(
                getApplicationContext().openFileInput(CACHE_FILENAME)
        )) {
            KadaisWithTime kadais = (KadaisWithTime) ois.readObject();
            Log.i("loadKadaiList", "find kadai list in cache");
            return kadais;
        } catch (IOException | ClassNotFoundException e) {
            Log.i("loadKadaiList", "kadai cache not found");
            return null;
        } catch (ClassCastException e) {
            Log.i("loadKadaiList", "wrong class. cache layout is too old");
            return null;
        }
    }
}
