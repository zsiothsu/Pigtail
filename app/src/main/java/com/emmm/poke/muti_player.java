package com.emmm.poke;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.annotation.Nullable;

import com.emmm.poke.databinding.MultiPlayerSwitchBinding;

public class muti_player extends Activity {
    MultiPlayerSwitchBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MultiPlayerSwitchBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setListener();
    }

    public void setListener() {
        Button Button_create = findViewById(R.id.Button_new_game);

        Button_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton RBSchool = findViewById(R.id.Button_school_server);
                RadioButton RBLocal = findViewById(R.id.Button_local_server);

                if (RBSchool.isChecked()) {
                    Intent intent = new Intent();
                    intent.putExtra("Server", 'S');
                    intent.setClass(muti_player.this, CrateGameActivity.class);
                    startActivity(intent);
                } else if (RBLocal.isChecked()) {
                    Intent intent = new Intent();
                    intent.putExtra("Server", 'L');
                    intent.setClass(muti_player.this, CrateGameActivity.class);
                    startActivity(intent);
                }
            }
        });

        Button Button_join = findViewById(R.id.Button_join_game);

        Button_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton RBSchool = findViewById(R.id.Button_school_server);
                RadioButton RBLocal = findViewById(R.id.Button_local_server);

                if (RBSchool.isChecked()) {
                    Intent intent = new Intent();
                    intent.putExtra("Server", 'S');
                    intent.setClass(muti_player.this, JoinGameActivity.class);
                    startActivity(intent);
                } else if (RBLocal.isChecked()) {
                    Intent intent = new Intent();
                    intent.putExtra("Server", 'L');
                    intent.setClass(muti_player.this, JoinGameActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
}
