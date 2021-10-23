package com.emmm.poke;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.emmm.poke.client.Player;
import com.emmm.poke.databinding.PlayerWithPlayerBinding;
import com.emmm.poke.utils.GameOperation;

import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class player_with_remote extends Activity {
    PlayerWithPlayerBinding binding;

    Player PA;

    Button AIA;

    Thread flush_Thread;
    boolean thread_active;

    Lock flush_lock = new ReentrantLock();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = PlayerWithPlayerBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        TextView t = findViewById(R.id.card_group_num);
        t.setText("?");


        /* get context */
        Intent intent = this.getIntent();

        String token = intent.getStringExtra("token");
        String serverip = intent.getStringExtra("serverip");
        int login_port = intent.getIntExtra("login_port",8080);
        int game_port = intent.getIntExtra("game_port", 9000);
        String uuid = intent.getStringExtra("uuid");

        String username = intent.getStringExtra("username");
        String password = intent.getStringExtra("password");

        char game_mode = intent.getCharExtra("game_mode", 'N');

        PA = new Player(username, password);
        PA.setLoginServer(serverip, login_port);
        PA.setGameServer(serverip, game_port);
        PA.token = token;
        PA.uuid = uuid;

        if(game_mode == 'J') {
            PA.host = 1;
            PA.__createGame();
        } else {
            PA.host = 0;
            PA.__createGame();
        }

        setListener();


        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle("游戏 UUID")
            .setMessage(uuid);

        dialog.setPositiveButton("确定", null);
        dialog.setNeutralButton("复制到剪贴板", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("uuid",uuid);
                clipboard.setPrimaryClip(clip);
            }
        });

        dialog.show();

        flush_Thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(!thread_active) {
                        return;
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if(!thread_active) {
                                    return;
                                }
                                if(PA!=null && PA.isGameOver()) {
                                    PA.getLast();
                                    flush();
                                    return;
                                }
                                PA.getLast();
                                flush();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                System.gc();
                            }
                        }
                    });
                }
            }
        });

        thread_active = true;
        flush_Thread.start();

        t = findViewById(R.id.Text_P2_AIStatus);
        t.setText("REMOTE PLAYER");

        dialog.show();
    }

    @SuppressLint("SetTextI18n")
    public void flush() {
        flush_lock.lock();
        try {
            Vector<String> card_PA = PA.host == 0 ? PA.get_card_host() : PA.get_card_guest();
            Vector<String> card_PB = PA.host == 0 ? PA.get_card_guest() : PA.get_card_host();
            String top_card = PA.get_top_card();
            boolean has_card = PA.get_card_group();

            String last_msg = PA.get_msg();
            TextView t = findViewById(R.id.msg_box);
            t.setText(last_msg);

            if (!has_card) {
                thread_active = false;
                int winner = PA.getWinner();
                if (winner == 0) {
                    t.setText("P1 获胜了！");
                } else if (winner == 1) {
                    t.setText("P2 获胜了！");
                } else {
                    t.setText("平局！");
                }
            }

            int cntAS = 0, cntAH = 0, cntAC = 0, cntAD = 0;
            int cntBS = 0, cntBH = 0, cntBC = 0, cntBD = 0;

            for (String i : card_PA) {
                char type = i.charAt(0);
                if (type == 'S') cntAS++;
                else if (type == 'H') cntAH++;
                else if (type == 'C') cntAC++;
                else if (type == 'D') cntAD++;
            }

            for (String i : card_PB) {
                char type = i.charAt(0);
                if (type == 'S') cntBS++;
                else if (type == 'H') cntBH++;
                else if (type == 'C') cntBC++;
                else if (type == 'D') cntBD++;
            }

            TextView text;
            text = (TextView) findViewById(R.id.P1_spade_num1);
            text.setText(String.valueOf(cntAS));
            text = (TextView) findViewById(R.id.P1_spade_num2);
            text.setText(String.valueOf(cntAS));

            text = findViewById(R.id.P1_heart_num1);
            text.setText(String.valueOf(cntAH));
            text = findViewById(R.id.P1_heart_num2);
            text.setText(String.valueOf(cntAH));

            text = findViewById(R.id.P1_club_num1);
            text.setText(String.valueOf(cntAC));
            text = findViewById(R.id.P1_club_num2);
            text.setText(String.valueOf(cntAC));

            text = findViewById(R.id.P1_diamond_num1);
            text.setText(String.valueOf(cntAD));
            text = findViewById(R.id.P1_diamond_num2);
            text.setText(String.valueOf(cntAD));

            text = findViewById(R.id.P2_spade_num1);
            text.setText(String.valueOf(cntBS));
            text = findViewById(R.id.P2_spade_num2);
            text.setText(String.valueOf(cntBS));

            text = findViewById(R.id.P2_heart_num1);
            text.setText(String.valueOf(cntBH));
            text = findViewById(R.id.P2_heart_num2);
            text.setText(String.valueOf(cntBH));

            text = findViewById(R.id.P2_club_num1);
            text.setText(String.valueOf(cntBC));
            text = findViewById(R.id.P2_club_num2);
            text.setText(String.valueOf(cntBC));

            text = findViewById(R.id.P2_diamond_num1);
            text.setText(String.valueOf(cntBD));
            text = findViewById(R.id.P2_diamond_num2);
            text.setText(String.valueOf(cntBD));

            if (top_card != null) {
                TextView topc;
                topc = findViewById(R.id.card_placement_num1);
                topc.setText(top_card.substring(1));
                topc = findViewById(R.id.card_placement_num2);
                topc.setText(top_card.substring(1));

                ImageView img = findViewById(R.id.card_placement_img);
                switch (top_card.charAt(0)) {
                    case 'S':
                        img.setImageResource(R.drawable.spade);
                        break;
                    case 'H':
                        img.setImageResource(R.drawable.heart);
                        break;
                    case 'C':
                        img.setImageResource(R.drawable.club);
                        break;
                    case 'D':
                        img.setImageResource(R.drawable.diamond);
                        break;
                }
            } else {
                TextView topc;
                topc = findViewById(R.id.card_placement_num1);
                topc.setText("");
                topc = findViewById(R.id.card_placement_num2);
                topc.setText("");

                ImageView img = findViewById(R.id.card_placement_img);
                img.setImageDrawable(null);
            }

            t = findViewById(R.id.card_group_num);
            if (has_card) {
                t.setText("?");
            } else {
                t.setText(null);
                RelativeLayout rl = findViewById(R.id.card_group);
                rl.setAlpha(0.7f);
            }
        } finally {
            flush_lock.unlock();
        }
    }

    public void setListener() {
        AIA = findViewById(R.id.Button_AIP1);

        AIA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView t = findViewById(R.id.Text_P1_AIStatus);
                if (PA.ai.status) {
                    PA.ai.disable();
                    t.setText("AI OFF");
                }
                else {
                    PA.ai.enable();
                    t.setText("AI RUNNING");
                }
            }
        });

        RelativeLayout Button_card_group = findViewById(R.id.card_group);
        Button_card_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP1 = PA.getLast();

                    String log = new String();

                    if (isP1) {
                        log = PA.operate_update(GameOperation.turnOver, null).third;
                    }
                    TextView t = findViewById(R.id.msg_box);
                    t.setText(log);
                    flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        RelativeLayout Button_P1_card_S = findViewById(R.id.P1cardS);
        Button_P1_card_S.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP1 = PA.getLast();
                    Vector<String> card = PA.host == 0 ? PA.get_card_host() : PA.get_card_guest();
                    if (isP1) {
                        for (String i : card) {
                            if (i.charAt(0) == 'S') {
                                String log = PA.operate_update(GameOperation.putCard, i).third;
                                TextView t = findViewById(R.id.msg_box);
                                t.setText(log);
                                flush();
                                break;
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        RelativeLayout Button_P1_card_H = findViewById(R.id.P1cardH);
        Button_P1_card_H.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP1 = PA.getLast();
                    Vector<String> card = PA.host == 0 ? PA.get_card_host() : PA.get_card_guest();
                    if (isP1) {
                        for (String i : card) {
                            if (i.charAt(0) == 'H') {
                                String log = PA.operate_update(GameOperation.putCard, i).third;
                                TextView t = findViewById(R.id.msg_box);
                                t.setText(log);
                                flush();
                                break;
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        RelativeLayout Button_P1_card_C = findViewById(R.id.P1cardC);
        Button_P1_card_C.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP1 = PA.getLast();
                    Vector<String> card = PA.host == 0 ? PA.get_card_host() : PA.get_card_guest();
                    if (isP1) {
                        for (String i : card) {
                            if (i.charAt(0) == 'C') {
                                String log = PA.operate_update(GameOperation.putCard, i).third;
                                TextView t = findViewById(R.id.msg_box);
                                t.setText(log);
                                flush();
                                break;
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });

        RelativeLayout Button_P1_card_D = findViewById(R.id.P1cardD);
        Button_P1_card_D.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP1 = PA.getLast();
                    Vector<String> card = PA.host == 0 ? PA.get_card_host() : PA.get_card_guest();
                    if (isP1) {
                        for (String i : card) {
                            if (i.charAt(0) == 'D') {
                                String log = PA.operate_update(GameOperation.putCard, i).third;
                                TextView t = findViewById(R.id.msg_box);
                                t.setText(log);
                                flush();
                                break;
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}
