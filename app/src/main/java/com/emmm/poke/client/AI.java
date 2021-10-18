package com.emmm.poke.client;

import android.util.Pair;

import com.emmm.poke.utils.GameOperation;

import java.util.Vector;

public class AI {
    Player player;
    boolean status;

    public AI(Player player) {
        this.player = player;
    }

    public void enable() {
        status = true;
    }

    public void disable() {
        status = false;
    }

    public void active() throws InterruptedException {
        char top_card = player.game.card_placement.empty()
                ? 'N'
                : player.game.card_placement.peek().charAt(0);

        Pair<Double, Character> max_card = new Pair<Double, Character>(0.0, 'N');
        Pair<Double, Character> gtS = new Pair<Double, Character>((double) player.game.rest_S / player.game.card_group, 'S');
        if (gtS.first > max_card.first) max_card = gtS;
        Pair<Double, Character> gtH = new Pair<Double, Character>((double) player.game.rest_H / player.game.card_group, 'H');
        if (gtH.first > max_card.first) max_card = gtH;
        Pair<Double, Character> gtC = new Pair<Double, Character>((double) player.game.rest_C / player.game.card_group, 'C');
        if (gtC.first > max_card.first) max_card = gtC;
        Pair<Double, Character> gtD = new Pair<Double, Character>((double) player.game.rest_D / player.game.card_group, 'D');
        if (gtD.first > max_card.first) max_card = gtD;

        int otS = player.game.own_S;
        int otH = player.game.own_H;
        int otC = player.game.own_C;
        int otD = player.game.own_D;

        Vector<String> own_card = player.host == 0 ? player.game.card_at_p1 : player.game.card_at_p2;

        boolean status = false;
        /* put card */
        if ((top_card != 'S' && max_card.second == 'S' && otS > 0)
                || (top_card != 'H' && max_card.second == 'H' && otH > 0)
                || (top_card != 'C' && max_card.second == 'C' && otC > 0)
                || (top_card != 'D' && max_card.second == 'D' && otD > 0)
        ) {
            String card = null;
            for (String c : own_card) {
                if(c.charAt(0) == max_card.second) {
                    card = c;
                    player.operate_update(GameOperation.putCard, card);
                    status = true;
                    break;
                }
            }
        }
        /* turn over */
        if(!status) {
            player.operate_update(GameOperation.turnOver, null);
        }
    }
}
