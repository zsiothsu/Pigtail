package com.emmm.poke.server;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import com.emmm.poke.utils.*;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.*;

class OneGame {
    /******************************************
     *            const variable              *
     ******************************************/
    public static final String[] card_new = {
            "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "SJ", "SQ", "SK",
            "H1", "H2", "H3", "H4", "H5", "H6", "H7", "H8", "H9", "H10", "HJ", "HQ", "HK",
            "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "CJ", "CQ", "CK",
            "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10", "DJ", "DQ", "DK"
    };

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
    public boolean private_status;
    public boolean finished;

    public GameStatus gameStatus;
    public int turn;
    public int winner;


    /* player information */
    public int host_id;
    public int guest_id;

    private Vector<String> log;
    private Vector<String> log_msg;

    /******************************************
     *             game operation             *
     ******************************************/
    public OneGame(int ID, String uuid, boolean private_status) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        this.card_placement = new Stack<String>();
        this.card_at_p1 = new Vector<String>();
        this.card_at_p2 = new Vector<String>();
        this.gameStatus = GameStatus.WAITING;
        this.ID = ID;
        this.updated_at = null;
        this.deleted_at = null;
        this.uuid = uuid;
        this.private_status = private_status;
        this.finished = false;

        this.turn = 0;
        this.winner = -1;

        host_id = 0;
        guest_id = 0;

        this.log = new Vector<String>();
        this.log_msg = new Vector<String>();

        this.create_at = formatter.format(new Date()).trim();
        String[] timestr = this.create_at.split(" ");
        this.create_at = timestr[0] + "T" + timestr[1] + "Z";

        /* shuffle card */
        Vector<String> card_group_new = new Vector<String>(Arrays.asList(card_new));
        Random ran = new Random(new Date().getTime());
        int len = card_group_new.size();
        for (int i = 0; i < len; i++) {
            int k = ran.nextInt() % (i + 1);
            k = k < 0 ? -k : k;
            String t = card_group_new.get(k);
            card_group_new.set(k, card_group_new.get(i));
            card_group_new.set(i, t);
        }

        this.card_group = new Stack<String>();
        for (String i : card_group_new) {
            this.card_group.push(i);
        }
    }

    /**
     * @param host_id: id of game host player
     * @apiNote set host of this game
     */
    public void set_host(int host_id) {
        this.host_id = host_id;
    }

    /**
     * @param guest_id: id of guest player
     * @apiNote set guest of this game
     */
    public void add_guest(int guest_id) {
        this.guest_id = guest_id;
        this.gameStatus = GameStatus.READY;
    }

    /**
     * @param player_id: player id, NOT the id of host/guest
     * @param op:        game operation
     * @param card:      if op equals GameOperation.putCard, card must be non-null
     * @return Tuple.first: isSuccess
     * Tuple.second: full message
     * Tuple.third: operated card
     */
    public Tuple<Boolean, String, String> operate(int player_id, GameOperation op, String card) {
        if (this.gameStatus == GameStatus.WAITING) {
            return new Tuple<Boolean, String, String>(false, "人还没齐", new String());
        } else if (this.gameStatus == GameStatus.OVER) {
            return new Tuple<Boolean, String, String>(false, "对局已结束", new String());
        }

        int turn_bak = turn;

        if (this.gameStatus == GameStatus.PLAYING) {
            if (!(turn == 0 ? (host_id == player_id) : (guest_id == player_id))) {
                return new Tuple<Boolean, String, String>(false, "还不是你的回合", new String());
            }
        }

        int id = turn;
        String id_string = id == 0 ? "P1" : "P2";
        String op_string = op == GameOperation.turnOver ? "从\u003c牌库\u003e翻开了一张" : "从\u003c手牌\u003e打出了一张";
        String res = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        if (op == GameOperation.turnOver) {
            String new_card = this.card_group.pop();
            String old_top = this.card_placement.empty() ? null : this.card_placement.peek();

            card = new_card;
            this.card_placement.push(new_card);

            if (!(old_top == null) && old_top.charAt(0) == new_card.charAt(0)) {
                Vector<String> player_card_group = turn == 0 ? this.card_at_p1 : this.card_at_p2;

                while (!this.card_placement.empty()) {
                    player_card_group.add(this.card_placement.pop());
                }

                res = " 并拿走了\u003c放置区\u003e的卡牌";
            }

            if (this.card_group.empty()) {
                this.gameStatus = GameStatus.OVER;

                if (this.card_at_p1.size() > this.card_at_p2.size()) this.winner = 0;
                else if (this.card_at_p1.size() < this.card_at_p2.size()) this.winner = 1;
                else this.winner = -1;

                this.finished = true;
            }

            if (this.gameStatus == GameStatus.READY) {
                this.turn = player_id == host_id ? 1 : 0;
                this.gameStatus = GameStatus.PLAYING;
            } else {
                this.turn = (this.turn + 1) % 2;
            }

            id = (this.turn + 1) % 2;
            id_string = id == 0 ? "P1" : "P2";

            String code = id + " 0 " + card;
            String msg = id_string + " " + op_string + " " + card;
            if (res != null) msg += res;

            this.log.add(code);
            this.log_msg.add(msg);


            this.updated_at = formatter.format(new Date()).trim();
            String[] timestr = this.updated_at.split(" ");
            this.updated_at = timestr[0] + "T" + timestr[1] + "Z";

            return new Tuple<Boolean, String, String>(true, code, msg);
        } else {
            String old_top = this.card_placement.empty() ? null : this.card_placement.peek();
            Vector<String> player_card_group = turn == 0 ? this.card_at_p1 : this.card_at_p2;

            if (card == null || !player_card_group.contains(card)) {
                this.gameStatus = GameStatus.READY;
                this.turn = turn_bak;
                return new Tuple<Boolean, String, String>(false, "非法操作", new String());
            }
            this.card_placement.push(card);

            if (!(old_top == null) && old_top.charAt(0) == card.charAt(0)) {
                while (!this.card_placement.empty()) {
                    player_card_group.add(this.card_placement.pop());
                }
                res = " 并拿走了\u003c放置区\u003e的卡牌";
            }

            if (this.gameStatus == GameStatus.READY) {
                this.turn = player_id == host_id ? 1 : 0;
                this.gameStatus = GameStatus.PLAYING;
            } else {
                this.turn = (this.turn + 1) % 2;
            }

            id = (this.turn + 1) % 2;
            id_string = id == 0 ? "P1" : "P2";

            String code = id + " 0 " + card;
            String msg = id_string + " " + op_string + " " + card;
            if (res != null) msg += res;

            this.log.add(code);
            this.log_msg.add(msg);

            this.updated_at = formatter.format(new Date()).trim();
            String[] timestr = this.updated_at.split(" ");
            this.updated_at = timestr[0] + "T" + timestr[1] + "Z";

            return new Tuple<Boolean, String, String>(true, code, msg);
        }
    }

    public GameStatus getGameStatus() {
        return this.gameStatus;
    }

    public int getUserOrder(int id) {
        return id == host_id ? 0 : 1;
    }

    public Tuple<String, String, Integer> getLast() {
        /* <log, log_msg, turn now> */
        if (this.updated_at != null) {
            return new Tuple<String, String, Integer>(log.lastElement(), this.log_msg.lastElement(), turn);
        }

        return new Tuple<String, String, Integer>(new String(), new String(), 0);
    }

    public boolean getTurn(int id) {
        return (this.turn == 0 ? (this.host_id == id) : (this.guest_id == id));
    }
}

public class LocalServer extends RouterNanoHTTPD {
    /* {uuid, OneGame} */
    public static HashMap<String, OneGame> gameList = new HashMap<>();
    /* {ID, token} */
    public static HashMap<String, Integer> playerList = new HashMap<>();
    public static int cnt = 0;
    public static int cnt_player = 0;

    public static LocalServer server = new LocalServer(8888);

    public LocalServer(int port) {
        super(port);
        cnt = 0;
        cnt_player = 1;

        addMappings();

        try {
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class LoginHandler extends DefaultHandler {
        public LoginHandler() {
            super();
        }

        @Override
        public String getText() {
            return "{\"data\":{\"error_msg\":\"\\u8d26\\u53f7\\u6216\\u5bc6\\u7801\\u9519\\u8bef\"},\"message\":\"Parameter Error\",\"status\":400}\n";
        }

        @Override
        public String getMimeType() {
            return "application/json";
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.BAD_REQUEST;
        }

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            HashMap<String, String> map = new HashMap<>();
            try {
                session.parseBody(map);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ResponseException e) {
                e.printStackTrace();
            }

            String student_id = null;
            String password = null;

            student_id = session.getParameters().get("student_id").get(0);
            password = session.getParameters().get("password").get(0);

            /* param error */
            if (student_id == null || password == null) {
                return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), "{\"student\":\"" + student_id + "\",\"pa\":\"" + password + "\",\"full\":\"" + password + "\"}");
            }

            String token = TokenEncrypt.encode(student_id + (new Date().getTime()) + password);
            LocalServer.playerList.put(token, LocalServer.cnt_player++);

            JSONObject res = new JSONObject();
            JSONObject data = new JSONObject();
            JSONObject detail = new JSONObject();

            try {
                detail.put("id", LocalServer.cnt_player - 1);
                detail.put("name", "本地玩家" + (LocalServer.cnt_player - 1));
                detail.put("student_id", student_id);

                data.put("detail", detail);
                data.put("token", token);

                res.put("data", data);
                res.put("message", "Success");
                res.put("status", 200);

                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(), res.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), getText());
            }
        }
    }

    public static class GameControlHandler extends DefaultHandler {
        public GameControlHandler() {
            super();
        }

        @Override
        public String getText() {
            return "{\"code\":401,\"data\":{},\"msg\":\"鉴权失败\"}";
        }

        @Override
        public String getMimeType() {
            return "application/json";
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.UNAUTHORIZED;
        }

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            String token = session.getHeaders().get("authorization");

            /* unauthorized */
            if (token == null ||
                    !LocalServer.playerList.containsKey(token)) {
                return NanoHTTPD.newFixedLengthResponse(getStatus(), "application/json", getText());
            }

            boolean pri = false;

            try {
                HashMap<String, String> map = new HashMap<>();
                session.parseBody(map);

                String body = map.toString();
                JSONObject query = null;
                if (body != null) {
                    query = new JSONObject(body);
                }

                if (query != null && !query.isNull("private")) {
                    pri = query.getBoolean("private");
                }

            } catch (IOException | ResponseException | JSONException e) {
                /* nothing to be done */
            }

            String uuid = createGame(LocalServer.playerList.get(token), pri);

            JSONObject res = new JSONObject();
            JSONObject data = new JSONObject();

            try {
                data.put("uuid", uuid);

                res.put("data", data);
                res.put("code", 200);
                res.put("msg", "操作成功");


                server.addMapping("/api/game/" + uuid, LocalServer.GameHandler.class);
                server.addMapping("/api/game/" + uuid + "/last", LocalServer.GameHandler.class);
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", res.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                return NanoHTTPD.newFixedLengthResponse(getStatus(), "application/json", "getText()");
            }
        }
    }

    public static class GameHandler extends DefaultHandler {
        public GameHandler() {
            super();
        }

        @Override
        public String getText() {
            return "{\"code\":401,\"data\":{},\"msg\":\"鉴权失败\"}";
        }

        @Override
        public String getMimeType() {
            return "application/json";
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.UNAUTHORIZED;
        }

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            String token = session.getHeaders().get("authorization");

            /* unauthorized */
            if (token == null ||
                    !LocalServer.playerList.containsKey(token)) {
                return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), getText());
            }

            String baseUri = uriResource.getUri();
            String[] uriSq = baseUri.trim().split("/");

            String uuid = uriSq[2];

            if (LocalServer.gameList.containsKey(uuid)) {
                OneGame game = LocalServer.gameList.get(uuid);

                assert game != null;

                if (game.guest_id != 0) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), "{\"code\":403,\"data\":{\"err_msg\":\"队伍已满\"},\"msg\":\"非法操作\"}");
                }

                game.add_guest(LocalServer.playerList.get(token));

                JSONObject res = new JSONObject();

                try {
                    res.put("code", 200);
                    res.put("data", new JSONObject());
                    res.put("msg", "操作成功");

                    return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(), res.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, getMimeType(), "{\"code\": 404, \"data\": {}, \"msg\": \"Not Found\"}");
        }

        @Override
        public Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            String token = session.getHeaders().get("authorization");

            /* unauthorized */
            if (token == null ||
                    !LocalServer.playerList.containsKey(token)) {
                return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), getText());
            }

            String baseUri = uriResource.getUri();
            String[] uriSq = baseUri.trim().split("/");

            String uuid = uriSq[2];

            int type = 0;
            String card = null;

            try {
                HashMap<String, String> map = new HashMap<>();
                session.parseBody(map);

                String filename = map.get("content");
                BufferedReader reader = new BufferedReader(new FileReader(filename));
                String body = reader.readLine();

                JSONObject query = new JSONObject(body);

                if (query.has("type")) {
                    type = query.getInt("type");
                }

                if (type != 0 && type != 1) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), "{\"code\":403,\"data\":{\"err_msg\":\"type 错误\"},\"msg\":\"非法操作\"}");
                }

                if (query.has("card")) {
                    card = query.getString("card");
                }

                if (type == 1 && card == null) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), "{\"code\":403,\"data\":{\"err_msg\":\"无效操作: 未指定卡牌\"},\"msg\":\"非法操作\"}");
                }
            } catch (IOException | ResponseException | JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), "{\"code\":403,\"data\":{\"err_msg\":\"type 错误\"},\"msg\":\"非法操作\"}");
            }

            if (LocalServer.gameList.containsKey(uuid)) {
                OneGame game = LocalServer.gameList.get(uuid);
                assert game != null;

                int id = LocalServer.playerList.get(token);

                if(game.getGameStatus() == GameStatus.PLAYING || game.getGameStatus() == GameStatus.READY) {
                    try {
                        Tuple<Boolean, String, String> opres = game.operate(id, type == 0 ? GameOperation.turnOver : GameOperation.putCard, card);
                        if (!opres.first) {
                            JSONObject res = new JSONObject();
                            JSONObject data = new JSONObject();

                            data.put("err_msg", opres.second);

                            res.put("code", 403);
                            res.put("data", data);
                            res.put("msg", "非法操作");

                            return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), res.toString());
                        } else {
                            JSONObject res = new JSONObject();
                            JSONObject data = new JSONObject();

                            String last_code = opres.third;

                            data.put("last_msg", opres.second);
                            data.put("last_code", last_code);

                            res.put("code", 200);
                            res.put("data", data);
                            res.put("msg", "操作成功");

                            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(), res.toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if(game.getGameStatus() == GameStatus.OVER) {
                    String msg = "{\"code\":401,\"data\":{\"err_msg\":\"对局已结束\"}, \"msg\":\"鉴权失败\"}";
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), msg);
                } else if(game.getGameStatus() == GameStatus.WAITING) {
                    String msg = "{\"code\":403,\"data\":{\"err_msg\":\"人还没齐\"}, \"msg\":\"非法操作\"}";
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), msg);
                }
            }
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, getMimeType(), "{\"code\": 404, \"data\": {}, \"msg\": \"Not Found\"}");
        }

        @Override
        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            String token = session.getHeaders().get("authorization");

            /* unauthorized */
            if (token == null ||
                    !LocalServer.playerList.containsKey(token)) {
                return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), getText());
            }

            String baseUri = uriResource.getUri();
            String[] uriSq = baseUri.trim().split("/");

            String uuid = uriSq[2];

            if (uriSq.length >= 4 && uriSq[3].equals("last") && LocalServer.gameList.containsKey(uuid)) {
                OneGame game = LocalServer.gameList.get(uuid);
                assert game != null;

                int id = LocalServer.playerList.get(token);

                try {
                    if (game.getGameStatus() == GameStatus.PLAYING || game.getGameStatus() == GameStatus.OVER) {
                        Tuple<String, String, Integer> last = game.getLast();

                        JSONObject res = new JSONObject();
                        JSONObject data = new JSONObject();

                        data.put("last_code", last.first);
                        data.put("last_msg", last.second);

                        data.put("your_turn", game.getTurn(id));

                        res.put("code", game.getGameStatus() == GameStatus.PLAYING ? 200 : 401);
                        res.put("data", data);
                        res.put("msg", "操作成功");

                        return NanoHTTPD.newFixedLengthResponse(game.getGameStatus() == GameStatus.PLAYING ? Response.Status.OK : Response.Status.UNAUTHORIZED, getMimeType(), res.toString());
                    } else if (game.getGameStatus() == GameStatus.READY) {
                        String msg = "{\"code\":200,\"data\":{\"last_code\":\"\", \"last_msg\": \"对局刚开始\", \"your_turn\": true}, \"msg\":\"操作成功\"}";
                        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(), msg);
                    } else if (game.getGameStatus() == GameStatus.WAITING) {
                        String msg = "{\"code\":403,\"data\":{\"err_msg\":\"人还没齐\"}, \"msg\":\"非法操作\"}";
                        return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), msg);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return NanoHTTPD.newChunkedResponse(getStatus(), getMimeType(), getData());
            }

            return NanoHTTPD.newChunkedResponse(getStatus(), getMimeType(), getData());
        }
    }

    public static class _404Handler extends DefaultHandler {
        public _404Handler() {

        }

        @Override
        public String getText() {
            return "{\"code\": 404, \"msg\": \"Not Found\"}";
        }

        @Override
        public String getMimeType() {
            return "application/json";
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.NOT_FOUND;
        }
    }

    public void addMappings() {
        addRoute("/api/user/login/", LoginHandler.class, this);
        addRoute("/api/game/", GameControlHandler.class, this);
        setNotFoundHandler(_404Handler.class);
    }

    public void addMapping(String url, Class<?> obj) {
        addRoute(url, obj);
    }

    public static String createGame(int id, boolean priv) {
        String guuid = UUID.randomUUID().toString();
        guuid = guuid.substring(0, 8) +
                guuid.substring(9, 13) +
                guuid.substring(14, 18) +
                guuid.substring(19, 23) +
                guuid.substring(24);
        guuid = guuid.substring(0, 16).toLowerCase();

        OneGame game = new OneGame(cnt++, guuid, priv);
        game.set_host(id);
        gameList.put(guuid, game);

        return guuid;
    }
}

