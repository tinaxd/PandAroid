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

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.prefs.PreferenceChangeEvent;

public class LoginInfoSettings extends AppCompatActivity {

    private EditText ecsIdEdit;
    private EditText passwordEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_info_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ecsIdEdit = findViewById(R.id.ecsIdEdit);
        passwordEdit = findViewById(R.id.passwordEdit);

        // set field
        LoginInfo info = getLoginInfoFromSharedPreferences();
        if (info != null) {
            ecsIdEdit.setText(info.getId());
            passwordEdit.setText(info.getPassword());
        }

        ((Button)findViewById(R.id.okButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveLoginInfoInSharedPreferences(ecsIdEdit.getText().toString().trim(), passwordEdit.getText().toString().trim());
                finish();
            }
        });
    }

    private void saveLoginInfoInSharedPreferences(LoginInfo li) {
        boolean success = PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString("id", li.getId())
                .putString("password", li.getPassword())
                .commit();
        if (!success) {
            // show toast
            String text = "Failed to set login info. Please report this bug to the developer.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        }
    }

    private void saveLoginInfoInSharedPreferences(String id, String password) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        if (id == null || id.isEmpty()) {
            editor.remove("id");
            Log.d("login info", "id removed");
        } else {
            editor.putString("id", id);
            Log.d("login info", "put id info");
        }

        if (password == null || password.isEmpty()) {
            editor.remove("password");
            Log.d("login info", "password removed");
        } else {
            editor.putString("password", password);
            Log.d("login info", "put password info");
        }

        boolean success = editor.commit();
        if (!success) {
            // show toast
            String text = "Failed to set login info. Please report this bug to the developer.";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        }
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
}