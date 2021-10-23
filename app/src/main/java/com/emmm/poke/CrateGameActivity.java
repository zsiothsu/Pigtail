package com.emmm.poke;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.emmm.poke.client.Player;
import com.emmm.poke.databinding.CreateGameBinding;

public class CrateGameActivity extends Activity {

    CreateGameBinding binding;
    char server_mode = 'N';

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        server_mode = intent.getCharExtra("Server", 'N');

        binding = CreateGameBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setListener();
    }

    public void setListener() {
        Button login = findViewById(R.id.Button_login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText text_username = findViewById(R.id.Text_username);
                EditText text_password = findViewById(R.id.Text_password);

                String username = text_username.getText().toString();
                String password = text_password.getText().toString();

                Player player = new Player(username, password);

                if (server_mode == 'S') {
                    player.setLoginServer("172.17.173.97", 8080);
                    player.setGameServer("172.17.173.97", 9000);
                } else {
                    player.setLoginServer("127.0.0.1", 9000);
                    player.setGameServer("127.0.0.1", 9000);
                }

                try {
                    boolean status = player.login();

                    if (status) {
                        String token = player.token;

                        Intent newIntent = new Intent();
                        newIntent.putExtra("token", token);
                        newIntent.putExtra("serverip", player.server_game_ip);
                        newIntent.putExtra("login_port", player.server_login_port);
                        newIntent.putExtra("game_port", player.server_game_port);

                        String uuid = player.createGame(false);
                        newIntent.putExtra("uuid", uuid);

                        newIntent.putExtra("username", player.username);
                        newIntent.putExtra("password", player.password);

                        newIntent.putExtra("game_mode", 'C');

                        newIntent.setClass(CrateGameActivity.this, player_with_remote.class);
                        startActivity(newIntent);
                    } else {
                        TextView msg = findViewById(R.id.Text_login_msg_box);
                        msg.setText("登录失败，请检查用户名和密码");
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        });
    }

}
