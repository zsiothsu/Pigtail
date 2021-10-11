package com.emmm.poke.server;

import com.emmm.poke.utils.*;

public class RemoteServer implements GameServer{
    @Override
    public String createGame(String token, boolean priv) {
        return null;
    }

    @Override
    public String joinGame(String token, String uuid) {
        return null;
    }

    @Override
    public boolean operate(String token, String uuid, GameOperation op, String card) {
        return true;
    }

    @Override
    public String getLastOp(String token, String uuid) {
        return null;
    }
}
