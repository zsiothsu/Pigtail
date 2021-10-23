/**
 * LocalServer
 *
 * Including definitions of game server.
 * It has the same API with remote server that SE course
 * applied. Not only served for single player mode but
 * also multi-player mode if game on another device connected
 * to this server by ip address.
 */

package com.emmm.poke.server;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.emmm.poke.utils.*;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.*;

/**
 * OneGame:
 * A class implements game PigTail, including operation, log
 * and automatic sequence management.
 */
class OneGame {
    /******************************************
     *            const variable              *
     ******************************************/
    /**
     * card_new
     * a enumeration of all cards for initializing of card group
     */
    public static final String[] card_new = {
            "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "SJ", "SQ", "SK",
            "H1", "H2", "H3", "H4", "H5", "H6", "H7", "H8", "H9", "H10", "HJ", "HQ", "HK",
            "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "CJ", "CQ", "CK",
            "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10", "DJ", "DQ", "DK"
    };

    /******************************************
     *            game information            *
     ******************************************/
    /* cards */
    private Stack<String> card_group;
    public Stack<String> card_placement;
    public Vector<String> card_at_p1;
    public Vector<String> card_at_p2;

    /* game status information */
    public GameStatus gameStatus;
    public int turn;
    public int winner;

    /******************************************
     *      game controlling information      *
     ******************************************/
    /* game controlling information*/
    public int ID;
    public String create_at;
    public String updated_at;
    public String deleted_at;
    public String uuid;
    public boolean private_status;
    public boolean finished;

    /* player information */
    public int host_id;
    public int guest_id;

    /**
     * lock
     * reentrant lock for multithreaded query,
     * for ensuring only one player getting the
     * first hand
     */
    Lock turn_lock = new ReentrantLock();

    /* logs */
    public Vector<String> log;
    public Vector<String> log_msg;

    /******************************************
     *             game operation             *
     ******************************************/
    /**
     * create a game instant.
     *
     * @param ID             game ID, managed by server
     * @param uuid           game uuid
     * @param private_status if set private, the game will not be shown on game list
     */
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

        /* turn will be changed after first operation */
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
     * set host of this game
     *
     * @param host_id host player id on server
     */
    public void set_host(int host_id) {
        this.host_id = host_id;
    }

    /**
     * set guest of this game
     *
     * @param guest_id: guest player id on server
     */
    public void add_guest(int guest_id) {
        this.guest_id = guest_id;
        /* reached two players, game is ready */
        this.gameStatus = GameStatus.READY;
    }

    /**
     * do an operation: put a card or turn over from card group
     *
     * @param player_id player id, NOT the id of host/guest
     * @param op        game operation
     * @param card      if op equals GameOperation.putCard, card must be non-null
     * @return Tuple.first: isSuccess
     * Tuple.second: code likes '0 0 H7'
     * Tuple.third: full log message
     */
    public Tuple<Boolean, String, String> operate(int player_id, GameOperation op, String card) {
        if (this.gameStatus == GameStatus.WAITING) {
            return new Tuple<Boolean, String, String>(false, "人还没齐", new String());
        } else if (this.gameStatus == GameStatus.OVER) {
            return new Tuple<Boolean, String, String>(false, "对局已结束", new String());
        }

        turn_lock.lock();

        try {
            /* backup turn for rollback */
            int turn_bak = turn;

            /* block player not with this turn */
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

                /* turn over from  card group */
                card = new_card;
                this.card_placement.push(new_card);

            /*
              if types of new card and card on old top is the same,
              move all cards to player
             */
                if (!(old_top == null) && old_top.charAt(0) == new_card.charAt(0)) {
                    Vector<String> player_card_group = turn == 0 ? this.card_at_p1 : this.card_at_p2;

                    while (!this.card_placement.empty()) {
                        player_card_group.add(this.card_placement.pop());
                    }

                    res = " 并拿走了\u003c放置区\u003e的卡牌";
                }

                /* game comes to end */
                if (this.card_group.empty()) {
                    this.gameStatus = GameStatus.OVER;

                    if (this.card_at_p1.size() > this.card_at_p2.size()) this.winner = 0;
                    else if (this.card_at_p1.size() < this.card_at_p2.size()) this.winner = 1;
                    else this.winner = -1; // no winner

                    this.finished = true;
                }

                if (this.gameStatus == GameStatus.READY) {
                    /* set turn for first hand player */
                    this.turn = player_id == host_id ? 1 : 0;
                    /* game start after first operation */
                    this.gameStatus = GameStatus.PLAYING;
                } else {
                    this.turn = (this.turn + 1) % 2;
                }

                /* logger */
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

                /* error operation */
                if (card == null || !player_card_group.contains(card)) {
                    this.gameStatus = GameStatus.READY;
                    this.turn = turn_bak;
                    return new Tuple<Boolean, String, String>(false, "非法操作", new String());
                }

                this.card_placement.push(card);
                player_card_group.remove(card);

            /*
              if types of new card and card on old top is the same,
              move all cards to player
             */
                if (!(old_top == null) && old_top.charAt(0) == card.charAt(0)) {
                    while (!this.card_placement.empty()) {
                        player_card_group.add(this.card_placement.pop());
                    }
                    res = " 并拿走了\u003c放置区\u003e的卡牌";
                }

                /* set turn for first hand player */
                if (this.gameStatus == GameStatus.READY) {
                    this.turn = player_id == host_id ? 1 : 0;
                    this.gameStatus = GameStatus.PLAYING;
                } else {
                    this.turn = (this.turn + 1) % 2;
                }

                /* logger */
                id = (this.turn + 1) % 2;
                id_string = id == 0 ? "P1" : "P2";

                String code = id + " 1 " + card;
                String msg = id_string + " " + op_string + " " + card;
                if (res != null) msg += res;

                this.log.add(code);
                this.log_msg.add(msg);

                this.updated_at = formatter.format(new Date()).trim();
                String[] timestr = this.updated_at.split(" ");
                this.updated_at = timestr[0] + "T" + timestr[1] + "Z";

                return new Tuple<Boolean, String, String>(true, code, msg);
            }
        } finally {
            turn_lock.unlock();
        }
    }

    /**
     * get game status
     *
     * @return GameStatus, PLAYING, READY, WAITING or OVER
     */
    public GameStatus getGameStatus() {
        return this.gameStatus;
    }

    /**
     * judge whether specific player is host
     *
     * @param id player id
     * @return order 0: host  1: guest
     */
    public int getUserOrder(int id) {
        return id == host_id ? 0 : 1;
    }

    /**
     * get last operation code
     *
     * @return Tuple.first: last code
     *         Tuple.second: last log message
     *         Tuple.third: turn now
     */
    public Tuple<String, String, Integer> getLast() {
        /* <log, log_msg, turn now> */
        if (this.updated_at != null) {
            return new Tuple<String, String, Integer>(log.lastElement(), this.log_msg.lastElement(), turn);
        }

        return new Tuple<String, String, Integer>(new String(), new String(), 0);
    }

    /**
     * get turn of player, and it will set turn for first hand player
     *
     * @param id player id
     * @return true: is turn of player
     * false: is not player's turn
     */
    public boolean getTurn(int id) {
        turn_lock.lock();
        boolean ret = false;
        if (this.gameStatus == GameStatus.PLAYING)
            ret = (this.turn == 0 ? (this.host_id == id) : (this.guest_id == id));
        else if (this.gameStatus == GameStatus.READY) {
            /* if player is first hand, set turn for this player */
            this.turn = id == host_id ? 0 : 1;
            this.gameStatus = GameStatus.PLAYING;
            ret = true;
        }
        turn_lock.unlock();
        return ret;
    }
}

/** Localserver
 * Game server of PigTail. It has the same api of remote server SE
 * class offered. Local server will be started while APP creating.
 * Served not only for single player mode, but also multi-player
 * games. Player can connect to this server by ip address.
 *
 * @see fi.iki.elonen.NanoHTTPD
 */
public class LocalServer extends RouterNanoHTTPD {
    /*
       Game list stored in server, game will be shown on game list
       if its not set private.
    */
    public static HashMap<String, OneGame> gameList = new HashMap<>();
    public static int game_public = 0;

    /* player list */
    public static HashMap<String, Integer> playerList = new HashMap<>();
    public static int cnt = 0;
    public static int cnt_player = 0;

    /* global server instance */
    public static LocalServer server = new LocalServer(9000);

    /**
     * Create a server
     * @param port server port
     */
    public LocalServer(int port) {
        super(port);
        cnt = 0;
        cnt_player = 1;

        /* add route for different uri */
        addMappings();

        try {
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** LoginHandler
     * Handler for api "/api/user/login/"
     */
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

        /**
         * Handler for POST method
         * User login
         */
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

            /* parameter error */
            if (student_id == null || password == null) {
                return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), getText());
            }

            /* generate token for user */
            Random ran = new Random(new Date().getTime());

            String token1 = TokenEncrypt.encode(student_id);
            String token2 = TokenEncrypt.encode(password);
            String token3 = TokenEncrypt.encode(String.valueOf(new Date().getTime()));
            String token4 = TokenEncrypt.encode(String.valueOf(ran.nextInt()));
            String token5 = TokenEncrypt.encode(String.valueOf(ran.nextInt()));
            String token = token1 + token2 + "." + token3 + token4 + "_" + token5 ;
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

    /** GameControlHandler
     * Including create a new game and get last operation.
     * For api "/api/game/"
     */
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

        /**
         * Handler for POST method
         * Create a new game
         * @see <a href="https://www.showdoc.com.cn/1605811132048301/7630672720835211"></a>
         */
        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            String token = session.getHeaders().get("authorization");

            /* unauthorized */
            if (token == null ||
                    !LocalServer.playerList.containsKey(token)) {
                return NanoHTTPD.newFixedLengthResponse(getStatus(), "application/json", getText());
            }

            boolean pri = false;

            /* parse private status from parameter*/
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

            /* create a new game */
            String uuid = createGame(LocalServer.playerList.get(token), pri);
            if (!pri) LocalServer.game_public++;

            JSONObject res = new JSONObject();
            JSONObject data = new JSONObject();

            try {
                data.put("uuid", uuid);

                res.put("data", data);
                res.put("code", 200);
                res.put("msg", "操作成功");

                /* add route for new game */
                server.addMapping("/api/game/" + uuid + "/last", LocalServer.GameControlHandler.class);
                server.addMapping("/api/game/" + uuid, LocalServer.GameHandler.class);
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", res.toString());
            } catch (JSONException e) {
                e.printStackTrace();
                return NanoHTTPD.newFixedLengthResponse(getStatus(), "application/json", "getText()");
            }
        }

        /**
         * Handler for GET method
         * Get last status for specific game
         * @see <a href="https://www.showdoc.com.cn/1605811132048301/7630674486176687"></a>
         */
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

                /* get user id, for judging whether is the turn of player*/
                int id = LocalServer.playerList.get(token);

                try {
                    if (game.getGameStatus() == GameStatus.PLAYING) {
                        Tuple<String, String, Integer> last = game.getLast();

                        JSONObject res = new JSONObject();
                        JSONObject data = new JSONObject();

                        data.put("last_code", last.first);
                        data.put("last_msg", last.second);

                        data.put("your_turn", game.getTurn(id));

                        /*
                           return
                           200/OK for PLAYING
                           401/UNAUTHORIZED for OVER
                         */
                        res.put("code", game.getGameStatus() == GameStatus.PLAYING ? 200 : 401);
                        res.put("data", data);
                        res.put("msg", "操作成功");

                        return NanoHTTPD.newFixedLengthResponse(game.getGameStatus() == GameStatus.PLAYING ? Response.Status.OK : Response.Status.UNAUTHORIZED, getMimeType(), res.toString());
                    } else if (game.getGameStatus() == GameStatus.OVER) {
                        String msg = "{\"code\":400,\"data\":{\"err_msg\":\"对局已结束\"}, \"msg\":\"鉴权失败\"}";
                        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(), msg);
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

    /** GameHandler
     * Game process handler
     * Handler for api "/api/game/:uuid"
     */
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

        /**
         * Handler for POST method
         * Join a game created
         * @see <a href="https://www.showdoc.com.cn/1605811132048301/7630673091659110"></a>
         */
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

                /* set player as guest on specific game */
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

        /**
         * Handler for PUT method
         * game operation: put a card or turn over from card group
         * @see <a href="https://www.showdoc.com.cn/1605811132048301/7630673637380927"></a>
         */
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

            /* get json parameter */
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

                /* error operational type */
                if (type != 0 && type != 1) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), "{\"code\":403,\"data\":{\"err_msg\":\"type 错误\"},\"msg\":\"非法操作\"}");
                }

                if (query.has("card")) {
                    card = query.getString("card");
                }

                /* put no card */
                if (type == 1 && card == null) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), "{\"code\":403,\"data\":{\"err_msg\":\"无效操作: 未指定卡牌\"},\"msg\":\"非法操作\"}");
                }
            } catch (IOException | ResponseException | JSONException e) {
                /* error parameter */
                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), "{\"code\":403,\"data\":{\"err_msg\":\"type 错误\"},\"msg\":\"非法操作\"}");
            }

            if (LocalServer.gameList.containsKey(uuid)) {
                OneGame game = LocalServer.gameList.get(uuid);
                assert game != null;

                int id = LocalServer.playerList.get(token);

                if (game.getGameStatus() == GameStatus.PLAYING || game.getGameStatus() == GameStatus.READY) {
                    try {
                        /* commit an operation on game*/
                        Tuple<Boolean, String, String> opres = game.operate(id, type == 0 ? GameOperation.turnOver : GameOperation.putCard, card);
                        if (!opres.first) {
                            /* error operation */
                            JSONObject res = new JSONObject();
                            JSONObject data = new JSONObject();

                            data.put("err_msg", opres.second);

                            res.put("code", 403);
                            res.put("data", data);
                            res.put("msg", "非法操作");

                            return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), res.toString());
                        } else {
                            /* success */
                            JSONObject res = new JSONObject();
                            JSONObject data = new JSONObject();

                            String last_code = opres.third;

                            data.put("last_code", opres.second);
                            data.put("last_msg", last_code);

                            res.put("code", 200);
                            res.put("data", data);
                            res.put("msg", "操作成功");

                            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(), res.toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (game.getGameStatus() == GameStatus.OVER) {
                    String msg = "{\"code\":400,\"data\":{\"err_msg\":\"对局已结束\"}, \"msg\":\"鉴权失败\"}";
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(), msg);
                } else if (game.getGameStatus() == GameStatus.WAITING) {
                    String msg = "{\"code\":403,\"data\":{\"err_msg\":\"人还没齐\"}, \"msg\":\"非法操作\"}";
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN, getMimeType(), msg);
                }
            }
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, getMimeType(), "{\"code\": 404, \"data\": {}, \"msg\": \"Not Found\"}");
        }

        /**
         * Handle for GET method
         * Get full message of specific game, only can be used after game over
         * @see <a href="https://www.showdoc.com.cn/1605811132048301/7630674256869020"></a>
         */
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

            if (LocalServer.gameList.containsKey(uuid)) {
                OneGame game = LocalServer.gameList.get(uuid);
                assert game != null;

                try {
                    if (game.getGameStatus() == GameStatus.OVER) {
                        Tuple<String, String, Integer> last = game.getLast();

                        JSONObject res = new JSONObject();
                        JSONObject data = new JSONObject();

                        StringBuilder host_hand = new StringBuilder();
                        StringBuilder client_hand = new StringBuilder();
                        StringBuilder card_placement = new StringBuilder();

                        /* show cards in each place */
                        for (String i : game.card_at_p1) {
                            host_hand.append(i).append(",");
                        }
                        for (String i : game.card_at_p2) {
                            client_hand.append(i).append(",");
                        }
                        for (String i : game.card_placement) {
                            card_placement.append(i).append(",");
                        }
                        host_hand.deleteCharAt(host_hand.length() - 1);
                        client_hand.deleteCharAt(client_hand.length() - 1);
                        card_placement.deleteCharAt(card_placement.length() - 1);

                        data.put("ID", game.ID)
                                .put("CreatedAt", game.create_at)
                                .put("UpdatedAt", game.updated_at)
                                .put("DeletedAt", null)
                                .put("uuid", uuid)
                                .put("host_id", game.host_id)
                                .put("client_id", game.guest_id)
                                .put("host_hand", host_hand.toString())
                                .put("client_hand", client_hand.toString())
                                .put("turn", game.turn)
                                .put("card_group", "")
                                .put("card_placement", card_placement.toString())
                                .put("private", game.private_status)
                                .put("finished", true)
                                .put("top", game.card_placement.empty() ? "nil" : game.card_placement.peek())
                                .put("last", game.log.lastElement())
                                .put("winner", game.winner);

                        res.put("code", 200);
                        res.put("data", data);
                        res.put("msg", "操作成功");

                        return NanoHTTPD.newFixedLengthResponse(game.getGameStatus() == GameStatus.PLAYING ? Response.Status.OK : Response.Status.UNAUTHORIZED, getMimeType(), res.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return NanoHTTPD.newChunkedResponse(getStatus(), getMimeType(), getData());
            }

            return NanoHTTPD.newChunkedResponse(getStatus(), getMimeType(), getData());
        }
    }

    /** indexHandler
     * Handler for api "/api/game/index/"
     * List all public games on server
     */
    public static class indexHandler extends DefaultHandler {

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

        /**
         * Hanlder for GET method
         * return list of public games
         * @see <a href="https://www.showdoc.com.cn/1605811132048301/7630727968009683"></a>
         */
        @Override
        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            String token = session.getHeaders().get("authorization");

            /* unauthorized */
            if (token == null ||
                    !LocalServer.playerList.containsKey(token)) {
                return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), getText());
            }

            int page_size = Integer.parseInt(Objects.requireNonNull(urlParams.get("page_size")));
            int page_num = Integer.parseInt(Objects.requireNonNull(urlParams.get("page_num")));

            if (page_num <= 0 || page_size * (page_num - 1) > game_public - 1) page_num = 1;
            if (page_size <= 0) page_size = game_public;

            /* mark begin index for recording */
            int cnt = 0;
            int begin = page_size * (page_num - 1);

            JSONObject json = new JSONObject();
            JSONObject data = new JSONObject();

            JSONArray games = new JSONArray();

            //TODO
            for (Map.Entry<String, OneGame> game_entry : LocalServer.gameList.entrySet()) {
                String uuid = game_entry.getKey();
                OneGame game = game_entry.getValue();

                if (game.private_status = true) continue;

                if (cnt >= begin && cnt < begin + page_size) {
                    JSONObject new_game = new JSONObject();

                    try {
                        new_game.put("uuid", uuid)
                                .put("host_id", game.host_id)
                                .put("client_id", game.guest_id)
                                .put("created_at", game.create_at);
                        games.put(new_game);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                cnt++;
            }

            if (cnt == 0) games = null;

            try {
                data.put("games", games)
                        .put("total", LocalServer.game_public)
                        .put("total_page_num", game_public / page_size);
                json.put("code", 200)
                        .put("data", data)
                        .put("msg", "操作成功");
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, getMimeType(), json.toString());
            } catch (JSONException e) {
                e.printStackTrace();
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

    /**
     * add routers for apis
     */
    public void addMappings() {
        addRoute("/api/user/login/", LoginHandler.class);
        addRoute("/api/game/", GameControlHandler.class);
        addRoute("/api/game/index/", indexHandler.class);
        setNotFoundHandler(_404Handler.class);
    }

    /**
     * add route on server
     * private usage
     */
    public void addMapping(String url, Class<?> obj) {
        addRoute(url, obj);
    }

    /**
     * create a new game and generate uuid for it.
     * @param id player id to be set host
     * @param priv private status
     * @return uuid
     */
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

