package com.emmm.poke;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.emmm.poke.client.Player;
import com.emmm.poke.databinding.PlayerWithPlayerBinding;
import com.emmm.poke.utils.GameOperation;

import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class player_with_ai extends Activity {
    PlayerWithPlayerBinding p2pbinding;
    Player PA;
    Player PB;

    Button AIA;
    Button AIB;

    Thread flush_Thread;
    boolean thread_active;

    Lock flush_lock = new ReentrantLock();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        p2pbinding = PlayerWithPlayerBinding.inflate(LayoutInflater.from(this));
        setContentView(p2pbinding.getRoot());

        TextView t = findViewById(R.id.card_group_num);
        t.setText("?");

        setListener();

        PA = new Player("playerA", "1234");
        PB = new Player("playerB", "1234");

        PA.setLoginServer("127.0.0.1", 9000);
        PA.setGameServer("127.0.0.1", 9000);

        PB.setLoginServer("127.0.0.1", 9000);
        PB.setGameServer("127.0.0.1", 9000);

        try {
            PA.login();
            PB.login();

            String uuid = PA.createGame(false);
            PB.joinGame(uuid);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        flush_Thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(!thread_active) {
                        return;
                    }
                    try {
                        Thread.sleep(1000);
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
                                if(PA!=null && !PA.isGameOver()) {
                                    PA.getLast();
                                    PB.getLast();
                                    flush();
                                    return;
                                }

                                PA.getLast();
                                PB.getLast();
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

        PB.ai.enable();
        t = findViewById(R.id.Text_P2_AIStatus);
        t.setText("AI RUNNING");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {

        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        thread_active = false;
        PA = null;
        PB = null;
    }

    @SuppressLint("SetTextI18n")
    public void flush() {
        flush_lock.lock();
        try {
            Vector<String> card_PA = PA.host == 0 ? PA.get_card_host() : PA.get_card_guest();
            Vector<String> card_PB = PB.host == 0 ? PA.get_card_host() : PA.get_card_guest();
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
        AIB = findViewById(R.id.Button_AIP2);

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

        AIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView t = findViewById(R.id.Text_P2_AIStatus);
                if (PB.ai.status) {
                    PB.ai.disable();
                    t.setText("AI OFF");
                }
                else {
                    PB.ai.enable();
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
                    boolean isP2 = PB.getLast();

                    String log = new String();

                    if (isP1) {
                        log = PA.operate_update(GameOperation.turnOver, null).third;
                        PB.getLast();
                    } else if (isP2) {
                        log = PB.operate_update(GameOperation.turnOver, null).third;
                        PA.getLast();
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
                                PB.getLast();
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
                                PB.getLast();
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
                                PB.getLast();
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
                                PB.getLast();
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

        RelativeLayout Button_P2_card_S = findViewById(R.id.P2cardS);
        Button_P2_card_S.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP2 = PB.getLast();
                    Vector<String> card = PB.host == 0 ? PB.get_card_host() : PB.get_card_guest();
                    if (isP2) {
                        for (String i : card) {
                            if (i.charAt(0) == 'S') {
                                String log = PB.operate_update(GameOperation.putCard, i).third;
                                TextView t = findViewById(R.id.msg_box);
                                t.setText(log);
                                PA.getLast();
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

        RelativeLayout Button_P2_card_H = findViewById(R.id.P2cardH);
        Button_P2_card_H.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP2 = PB.getLast();
                    Vector<String> card = PB.host == 0 ? PB.get_card_host() : PB.get_card_guest();
                    if (isP2) {
                        for (String i : card) {
                            if (i.charAt(0) == 'H') {
                                String log = PB.operate_update(GameOperation.putCard, i).third;
                                TextView t = findViewById(R.id.msg_box);
                                t.setText(log);
                                PA.getLast();
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

        RelativeLayout Button_P2_card_C = findViewById(R.id.P2cardC);
        Button_P2_card_C.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP2 = PB.getLast();
                    Vector<String> card = PB.host == 0 ? PB.get_card_host() : PB.get_card_guest();
                    if (isP2) {
                        for (String i : card) {
                            if (i.charAt(0) == 'C') {
                                String log = PB.operate_update(GameOperation.putCard, i).third;
                                TextView t = findViewById(R.id.msg_box);
                                t.setText(log);
                                PA.getLast();
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

        RelativeLayout Button_P2_card_D = findViewById(R.id.P2cardD);
        Button_P2_card_D.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PA.isGameOver()) {
                    return;
                }
                try {
                    boolean isP2 = PB.getLast();
                    Vector<String> card = PB.host == 0 ? PB.get_card_host() : PB.get_card_guest();
                    if (isP2) {
                        for (String i : card) {
                            if (i.charAt(0) == 'D') {
                                String log = PB.operate_update(GameOperation.putCard, i).third;
                                TextView t = findViewById(R.id.msg_box);
                                t.setText(log);
                                PA.getLast();
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
