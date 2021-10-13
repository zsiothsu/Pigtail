package com.emmm.poke.server;

import com.emmm.poke.utils.*;

public interface GameServer {
    public interface opt {
        public String createGame(String token, boolean priv);
        public String joinGame(String token, String uuid);

        public boolean operate(String token, String uuid, GameOperation op, String card);
        public String getLastOp(String token, String uuid);
    }
}


