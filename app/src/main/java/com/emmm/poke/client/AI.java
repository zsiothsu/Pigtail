package com.emmm.poke.client;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Pair;

import com.emmm.poke.utils.GameOperation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import android.os.Debug;

import androidx.annotation.RequiresApi;

public class AI {
    Player player;
    public boolean status;
    Thread AI_Thread;

    public AI(Player player) {
        this.player = player;
    }

    public void enable() {
        status = true;
        AI_Thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (status) {
                    try {
                        while (!player.getLast()) {
                            Thread.sleep(1000);
                            if (player.isGameOver()) return;
                        }
                        Thread.sleep(500);
                        player.ai.active();
                        if (player.isGameOver()) return;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }
        }, "AI_Thread_" + player.host);

        AI_Thread.start();
    }

    public void disable() {
        status = false;
    }

    @SuppressLint("NewApi")
    public void active() throws InterruptedException {

//        Debug.startMethodTracing();

        char top_card = player.game.card_placement.empty()
            ? 'N'
            : player.game.card_placement.peek().charAt(0);

        /* get the most card type in card group */
        Pair<Double, Character> most_type_in_group = new Pair<Double, Character>(0.0, 'N');
        Pair<Double, Character> get_chance_S = new Pair<Double, Character>((double) player.game.rest_S / player.game.card_group, 'S');
        if (get_chance_S.first > most_type_in_group.first) most_type_in_group = get_chance_S;
        Pair<Double, Character> get_chance_H = new Pair<Double, Character>((double) player.game.rest_H / player.game.card_group, 'H');
        if (get_chance_H.first > most_type_in_group.first) most_type_in_group = get_chance_H;
        Pair<Double, Character> get_chance_C = new Pair<Double, Character>((double) player.game.rest_C / player.game.card_group, 'C');
        if (get_chance_C.first > most_type_in_group.first) most_type_in_group = get_chance_C;
        Pair<Double, Character> get_chance_D = new Pair<Double, Character>((double) player.game.rest_D / player.game.card_group, 'D');
        if (get_chance_D.first > most_type_in_group.first) most_type_in_group = get_chance_D;

        int player_own_S = player.game.own_S;
        int player_own_H = player.game.own_H;
        int player_own_C = player.game.own_C;
        int player_own_D = player.game.own_D;

        Vector<String> own_card = player.host == 0 ? player.game.card_at_p1 : player.game.card_at_p2;

        boolean status = false;
    /*
       put card
       if have the same type as the card type in card group
     */
        if ((top_card != 'S' && most_type_in_group.second == 'S' && player_own_S > 0)
            || (top_card != 'H' && most_type_in_group.second == 'H' && player_own_H > 0)
            || (top_card != 'C' && most_type_in_group.second == 'C' && player_own_C > 0)
            || (top_card != 'D' && most_type_in_group.second == 'D' && player_own_D > 0)
        ) {
            String card = null;
            for (String c : own_card) {
                if (c.charAt(0) == most_type_in_group.second) {
                    card = c;
                    player.operate_update(GameOperation.putCard, card);
                    status = true;
                    break;
                }
            }
        }
        if (!status && top_card == most_type_in_group.second) {
            String card = null;
            char type_can_be_put = 'N';

            Pair<Character, Integer>[] t = new Pair[]{
                new Pair<>('S', player_own_S),
                new Pair<>('H', player_own_H),
                new Pair<>('C', player_own_C),
                new Pair<>('D', player_own_D)};
            Vector<Pair<Character, Integer>> types = new Vector<Pair<Character, Integer>>(Arrays.asList(t));

            for (int i = 0; i < 4; i++) {
                if (types.get(i).first.equals(top_card)) {
                    types.remove(i);
                    break;
                }
            }

            types.sort(
                (Pair<Character, Integer> a, Pair<Character, Integer> b) -> {return b.second - a.second;}
            );
            if(types.get(0).second != 0) type_can_be_put = types.get(0).first;

            for (String c : own_card) {
                if (c.charAt(0) == type_can_be_put) {
                    card = c;
                    player.operate_update(GameOperation.putCard, card);
                    status = true;
                    break;
                }
            }
        }
        /* turn over */
        if (!status) {
            player.operate_update(GameOperation.turnOver, null);
        }

//        Debug.stopMethodTracing();
    }
}
