package com.emmm.poke;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.emmm.poke.client.Player;
import com.emmm.poke.databinding.CreateGameBinding;
import com.emmm.poke.databinding.JoinGameBinding;

import java.util.Vector;

public class JoinGameActivity extends Activity {

    JoinGameBinding binding;
    char server_mode = 'N';

    Vector<String> game_list;
    int choice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = JoinGameBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        Intent intent = this.getIntent();
        server_mode = intent.getCharExtra("Server", 'N');

        binding = JoinGameBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setListener();
    }

    public void setListener() {
        Button join = findViewById(R.id.Button_join);

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText Text_username = findViewById(R.id.Text_join_username);
                EditText Text_join_password = findViewById(R.id.Text_join_password);
                EditText Text_server_ip = findViewById(R.id.Text_server_ip);
                EditText Text_uuid = findViewById(R.id.Text_uuid);

                String username = Text_username.getText().toString();
                String password = Text_join_password.getText().toString();
                String server_ip = Text_server_ip.getText().toString();
                String uuid = Text_uuid.getText().toString();

                Player player = new Player(username, password);

                if (server_mode == 'S') {
                    player.setLoginServer("172.17.173.97", 8080);
                    player.setGameServer("172.17.173.97", 9000);
                } else {
                    player.setLoginServer(server_ip, 9000);
                    player.setGameServer(server_ip, 9000);
                }

                try {
                    boolean status = player.login();

                    if (status) {
                        String token = player.token;

                        boolean isJoined = player.joinGame(uuid);

                        if(!isJoined) {
                            TextView msg = findViewById(R.id.Text_join_msg_box);
                            msg.setText("加入失败，请检查 uuid");
                            return;
                        }

                        Intent newIntent = new Intent();
                        newIntent.putExtra("token", token);
                        newIntent.putExtra("serverip", player.server_game_ip);
                        newIntent.putExtra("login_port", player.server_login_port);
                        newIntent.putExtra("game_port", player.server_game_port);

                        newIntent.putExtra("uuid", uuid);

                        newIntent.putExtra("username", player.username);
                        newIntent.putExtra("password", player.password);

                        newIntent.putExtra("game_mode", 'J');

                        newIntent.setClass(JoinGameActivity.this, player_with_remote.class);
                        startActivity(newIntent);
                    } else {
                        TextView msg = findViewById(R.id.Text_join_msg_box);
                        msg.setText("登录失败，请检查用户名和密码");
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Button Button_search = findViewById(R.id.Button_search);
        Button_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText Text_username = findViewById(R.id.Text_join_username);
                EditText Text_join_password = findViewById(R.id.Text_join_password);
                EditText Text_server_ip = findViewById(R.id.Text_server_ip);

                String username = Text_username.getText().toString();
                String password = Text_join_password.getText().toString();
                String server_ip = Text_server_ip.getText().toString();

                Player player = new Player(username, password);

                if (server_mode == 'S') {
                    player.setLoginServer("172.17.173.97", 8080);
                    player.setGameServer("172.17.173.97", 9000);
                } else {
                    player.setLoginServer(server_ip, 9000);
                    player.setGameServer(server_ip, 9000);
                }

                try {
                    boolean status = player.login();

                    if (status) {
                        game_list = player.gameList();

                        if(game_list.size() == 0) game_list.add("");

                        AlertDialog.Builder uuid_choose = new AlertDialog.Builder(JoinGameActivity.this);
                        uuid_choose.setTitle("游戏列表");

                        uuid_choose.setSingleChoiceItems(game_list.toArray(new String[0]), 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                choice = which;
                            }
                        });

                        uuid_choose.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (choice != -1) {
                                    EditText Text_uuid = findViewById(R.id.Text_uuid);
                                    Text_uuid.setText(game_list.get(choice));
                                }
                            }
                        });

                        uuid_choose.show();
                    }
                    else {
                        TextView msg = findViewById(R.id.Text_join_msg_box);
                        msg.setText("登录失败，请检查用户名和密码");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {

                }
            }
        });
    }
}
