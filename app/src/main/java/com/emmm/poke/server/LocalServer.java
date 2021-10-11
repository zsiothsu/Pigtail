package com.emmm.poke.server;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import com.emmm.poke.utils.GameOperation;
import com.emmm.poke.server.TokenEncrypt;

import fi.iki.elonen.*;

class OneGame {
    /******************************************
     *           enumeration class            *
     ******************************************/
    enum GameStatus {WAITING, READY, PLAYING, OVER}

    /******************************************
     *            game information            *
     ******************************************/
    /* game status */
    private Stack<String> card_group;
    private Stack<String> card_placement;
    private Vector<String> card_at_p1;
    private Vector<String> card_at_p2;

    public int ID;
    public String create_at;
    public String updated_at;
    public String deleted_at;
    public String uuid;
    public int host_id;
    public int guest_id;
    public boolean private_status;
    public boolean finished;

    private GameStatus gameStatus;
    private int turn;
    private String winner;


    /* player information */
    private String p1_token;
    private String p2_token;
    private Vector<String> log;

    /******************************************
     *             game operation             *
     ******************************************/
    public OneGame(int ID, String uuid, boolean private_status) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ss.SSSZ");

        this.card_group = new Stack<String>();
        this.card_placement = new Stack<String>();
        this.card_at_p1 = new Vector<String>();
        this.card_at_p2 = new Vector<String>();
        this.gameStatus = GameStatus.WAITING;
        this.ID = ID;
        this.create_at = formatter.format(new Date());
        this.updated_at = null;
        this.deleted_at = null;
        this.uuid = uuid;
        this.private_status = private_status;
        this.finished = false;

        this.turn = 0;
        this.winner = null;

        this.log = new Vector<String>();
    }

    /**
     * @param token: token of game host player
     * @apiNote set host of this game
     */
    public void set_host(String token, int host_id) {
        this.p1_token = token;
        this.host_id = host_id;
    }

    /**
     * @param token: token of guest player
     * @apiNote set guest of this game
     */
    public void add_guest(String token, int guest_id) {
        this.p2_token = token;
        this.guest_id = guest_id;
        this.gameStatus = GameStatus.READY;
    }

    public boolean operate(String token, GameOperation op, String card) {
        //TODO
        return true;
    }

    public GameStatus getGameStatus() {
        return this.gameStatus;
    }

    public int getUserOrder(String token) {
        return token.equals(p1_token) ? 0 : 1;
    }

    public String getLast() {
        //TODO
        return new String();
    }

    public int getTurn() {
        //TODO
        return turn;
    }
}

public class LocalServer extends NanoHTTPD implements GameServer {
    /* {uuid, OneGame} */
    HashMap<String, OneGame> gameList;
    /* {ID, token} */
    HashMap<Integer, String> playerList;
    int cnt;
    int cnt_player;

    private static final String MIME_JSON = "application/json";

    public LocalServer(int port) {
        super(port);
        this.cnt = 0;
        this.cnt_player = 0;
    }

    private Response return404() {
        String msg = "{\"code\":404, \"msg\": \"API Not Found\"}";
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_JSON, msg);
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (Method.POST.equals(session.getMethod())) {
            String uri = session.getUri();
            String[] path = uri.trim().split("/");

            /* login service*/
            if (path[0].equals("api") && path[1].equals("user")) {
                try {
                    session.parseBody(new HashMap<String, String>());
                    String body = session.getQueryParameterString();
                    JSONObject json = new JSONObject(body);

                    String student_id = json.getString("student_id");
                    String password = json.getString("password");

                    String token = TokenEncrypt.encode(student_id + (new Date()).toString() + password);
                    this.playerList.put(cnt_player++, token);

                    JSONObject res = new JSONObject();
                    res.put("status", 200);
                    res.put("message", "Success");

                    JSONObject data = new JSONObject();
                    JSONObject detail = new JSONObject();
                    detail.put("id", cnt_player - 1);
                    detail.put("name", "本地" + String.valueOf(cnt_player - 1));
                    detail.put("student_id", student_id);
                    data.put("detail", detail);
                    data.put("token", token);
                    res.put("data", data);

                    return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_JSON, res.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ResponseException | JSONException e) {
                    e.printStackTrace();
                }
            }
            /* game logic */
            else if (path[0].equals("api") && path[1].equals("game")) {
                try {
                    /* get body*/
                    session.parseBody(new HashMap<String, String>());
                    String body = session.getQueryParameterString();
                    JSONObject json = new JSONObject(body);

                    Map<String, String> header = session.getHeaders();
                    String token = header.get("Authorization");

                    if (token == null) {
                        String msg = "Unauthorized";
                        return NanoHTTPD.newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, msg);
                    }

                    /* get uri param*/
                    String uuid = null;
                    boolean last = false;
                    if (path.length >= 3) {
                        uuid = path[2].trim().substring(1);
                    }

                    if (path.length >= 4 && path[3].equals("last")) {
                        last = true;
                    }


                    /* create a new game */
                    if (!json.isNull("private")) {
                        boolean priv = json.getBoolean("private");
                        String guuid = this.createGame(token, priv);

                        JSONObject res = new JSONObject();
                        JSONObject data = new JSONObject();

                        res.put("code", 200);
                        data.put("uuid", guuid);
                        res.put("data", data);
                        res.put("msg", "操作成功");

                        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_JSON, res.toString());
                    }
                    /* join a game */
                    else if (json.length() == 0 && uuid != null) {
                        if (gameList.containsKey(uuid)) {
                            joinGame(token, uuid);

                            JSONObject res = new JSONObject();
                            JSONObject data = new JSONObject();

                            res.put("code", 200);
                            res.put("data", data);
                            res.put("msg", "操作成功");

                            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_JSON, res.toString());
                        } else {
                            return return404();
                        }
                    }
                    /* operation */
                    else if (!json.isNull("type")) {
                        if (gameList.containsKey(uuid)) {
                            int type = json.getInt("type");
                            String card = json.getString("card");
                            boolean isSuc = operate(token, uuid, type == 0 ? GameOperation.turnOver : GameOperation.putCard, card);

                            if (isSuc) {
                                String last_code = getLastOp(token, uuid);
                                String[] opstr = last_code.split(" ");
                                String last_msg = (opstr[0].equals("0") ? "1P" : "2P") +
                                        (opstr[1].equals("0") ? " 从<牌库>翻开了一张 " : "从<手牌>打出了一张") +
                                        opstr[2];

                                JSONObject res = new JSONObject();
                                JSONObject data = new JSONObject();

                                res.put("code", 200);
                                data.put("last_code", last_code);
                                data.put("last_msg", last_msg);
                                res.put("data", data);
                                res.put("msg", "操作成功");
                                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_JSON, res.toString());
                            } else {
                                String msg = "{\"code\":403, \"msg\": \"非法操作\"}";
                                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_JSON, msg);
                            }
                        } else {
                            return return404();
                        }
                    }
                    /* get last */
                    else if (last == true) {
                        if (gameList.containsKey(uuid)) {
                            if(gameList.get(uuid).getGameStatus() == OneGame.GameStatus.PLAYING) {
                                String last_code = getLastOp(token, uuid);
                                String[] opstr = last_code.split(" ");
                                String last_msg = (opstr[0].equals("0") ? "1P" : "2P") +
                                        (opstr[1].equals("0") ? " 从<牌库>翻开了一张 " : "从<手牌>打出了一张") +
                                        opstr[2];

                                boolean is_your_turn = gameList.get(uuid).getTurn() == gameList.get(uuid).getUserOrder(token);

                                JSONObject res = new JSONObject();
                                JSONObject data = new JSONObject();

                                res.put("code", 200);
                                data.put("last_code", last_code);
                                data.put("last_msg", last_msg);
                                data.put("your_turn", is_your_turn);
                                res.put("data", data);
                                res.put("msg", "操作成功");
                                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_JSON, res.toString());
                            } else if(gameList.get(uuid).getGameStatus() == OneGame.GameStatus.READY) {
                                String msg = "{\"code\": 200,\"data\": {\"last_code\": \"\",\"last_msg\": \"对局刚开始\",\"your_turn\": true },\"msg\": \"操作成功\" }";
                                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_JSON, msg);
                            } else if(gameList.get(uuid).getGameStatus() == OneGame.GameStatus.WAITING) {
                                String msg = "{\"code\": 403,\"data\": {\"err_msg\": \"人还没齐\"},\"msg\": \"非法操作\" }";
                                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_JSON, msg);
                            }
                        } else {
                            return return404();
                        }
                    }
                } catch (IOException | ResponseException | JSONException e) {
                    e.printStackTrace();
                }
            } else {
                return return404();
            }
        } else if (Method.GET.equals(session.getMethod())) {
            String msg = "{\"code\":405, \"msg\": \"Method Not Allowed\"}";
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_JSON, msg);
        } else {
            String msg = "{\"code\":405, \"msg\": \"Method Not Allowed\"}";
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_JSON, msg);
        }
        return return404();
    }

    @Override
    public String createGame(String token, boolean priv) {
        String guuid = UUID.randomUUID().toString();
        guuid = guuid.substring(0, 8) +
                guuid.substring(9, 13) +
                guuid.substring(14, 18) +
                guuid.substring(19, 23) +
                guuid.substring(24);
        guuid = guuid.substring(0, 16).toLowerCase();

        OneGame game = new OneGame(cnt++, guuid, priv);
        return guuid;
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
