package com.emmm.poke;

import android.content.res.Resources;
import android.os.Bundle;

import com.emmm.poke.client.Player;
import com.emmm.poke.utils.GameOperation;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.StrictMode;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.emmm.poke.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import com.emmm.poke.server.*;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public static LocalServer s = LocalServer.server;
    public volatile static boolean p1 = false, p2 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

//        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                .detectDiskReads()
//                .detectDiskWrites()
//                .detectNetwork()
//                .penaltyLog()
//                .build());
//        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                .detectLeakedSqlLiteObjects()
//                .detectLeakedClosableObjects()
//                .penaltyLog()
//                .penaltyDeath()
//                .build());

        Player player = new Player("123456", "123456");
        player.setLoginServer("127.0.0.1", 8888);
        player.setGameServer("127.0.0.1", 8888);
        Player player2 = new Player("123456", "123456");
        player2.setLoginServer("127.0.0.1", 8888);
        player2.setGameServer("127.0.0.1", 8888);

        boolean p1 = false, p2 = false;

        try {
            player.login();
            player2.login();
            String uuid = player.createGame(false);
            player2.joinGame(uuid);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        while (!player.getLast()) {
                            Thread.sleep(50);
                        }
                        player.ai.active();
                        if(player.isGameOver()) break;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                MainActivity.p1 = true;
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        while (!player2.getLast()) {
                            Thread.sleep(50);
                        }
                        player2.ai.active();
                        Thread.sleep(50);
                        if(player2.isGameOver()) break;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                MainActivity.p2 = true;
            }
        }).start();

        while (!(player.isGameOver() && player2.isGameOver()));
        System.out.println("OK");
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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}