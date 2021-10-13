package com.emmm.poke.server;

import com.emmm.poke.utils.*;

public class RemoteServer implements GameServer{
    public static String createGame(String token, boolean priv) {
        return null;
    }

    public static String joinGame(String token, String uuid) {
        return null;
    }

    public static boolean operate(String token, String uuid, GameOperation op, String card) {
        return true;
    }

    public static String getLastOp(String token, String uuid) {
        return null;
    }
}
