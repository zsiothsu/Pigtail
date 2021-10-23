package com.emmm.poke;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import com.emmm.poke.client.Player;
import com.emmm.poke.utils.GameOperation;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.emmm.poke.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.emmm.poke.server.*;

import java.io.IOException;

public class MainActivity extends Activity {

    private ActivityMainBinding binding;
    public static LocalServer s = LocalServer.server;
    public volatile static boolean p1 = false, p2 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setListener();
    }

    public void setListener() {
        Button two_player = findViewById(R.id.Button_TwoPlayer);
        two_player.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, player_with_player.class);
                startActivity(intent);
            }
        });

        Button player_with_ai = findViewById(R.id.Button_AIPlayer);
        player_with_ai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, player_with_ai.class);
                startActivity(intent);
            }
        });

        Button player_with_remote = findViewById(R.id.Button_MultiPlayer);
        player_with_remote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, muti_player.class);
                startActivity(intent);
            }
        });

        Button quit_game = findViewById(R.id.Button_Quit);
        quit_game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });

        Button help = findViewById(R.id.Button_help);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, HelpActivity.class);
                startActivity(intent);
            }
        });
    }
}