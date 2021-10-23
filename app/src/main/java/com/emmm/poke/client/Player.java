package com.emmm.poke.client;

import android.util.Log;

import com.emmm.poke.utils.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;


/**
 * OneGame_Simple
 * Implement a game instance. Update status by information
 * from game server.
 */
class OneGame_Simple {
    /******************************************
     *            game information            *
     ******************************************/
    public static final String[] card_new = {
        "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "SJ", "SQ", "SK",
        "H1", "H2", "H3", "H4", "H5", "H6", "H7", "H8", "H9", "H10", "HJ", "HQ", "HK",
        "C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "CJ", "CQ", "CK",
        "D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10", "DJ", "DQ", "DK"
    };

    /* cards */
    public int card_group;
    public Stack<String> card_placement;
    public int rest_S;
    public int rest_H;
    public int rest_C;
    public int rest_D;

    public Vector<String> card_at_p1;
    public Vector<String> card_at_p2;
    public int own_S;
    public int own_H;
    public int own_C;
    public int own_D;

    public int winner = -1;
    public boolean finished = false;

    /******************************************
     *      game controlling information      *
     ******************************************/
    public Vector<String> log;
    public Vector<String> log_msg;

    public OneGame_Simple() {
        this.card_placement = new Stack<>();
        this.card_at_p1 = new Vector<>();
        this.card_at_p2 = new Vector<>();

        this.card_group = 52;
        this.rest_S = 13;
        this.rest_H = 13;
        this.rest_C = 13;
        this.rest_D = 13;

        this.own_S = 0;
        this.own_H = 0;
        this.own_C = 0;
        this.own_D = 0;

        this.finished = false;
        this.winner = -1;

        log = new Vector<String>();
        log_msg = new Vector<String>();
    }

    /**
     * do an operation: put a card or turn over from card group
     *
     * @param host  player order, 0: host  1:guest
     * @param order own order.
     * @param op    game operation
     * @param card  card put or turned over
     * @return Tuple.first: isSuccess
     * Tuple.second: code likes '0 0 H7'
     * Tuple.third: full log message
     */
    public Tuple<Boolean, String, String> operate(int host, int order, GameOperation op, String card) {
        if (this.card_group <= 0) {
            return new Tuple<Boolean, String, String>(false, new String(), new String());
        }

        String id_string = host == 0 ? "P1" : "P2";
        String op_string = op == GameOperation.turnOver ? "从\u003c牌库\u003e翻开了一张" : "从\u003c手牌\u003e打出了一张";
        String res = null;

        if (op == GameOperation.turnOver) {
            String old_top = this.card_placement.empty() ? null : this.card_placement.peek();
            card_group -= 1;
            this.card_placement.push(card);

            /* turn over a card with 'type' from card group */
            char type = card.charAt(0);
            switch (type) {
                case 'S':
                    rest_S--;
                    break;
                case 'H':
                    rest_H--;
                    break;
                case 'C':
                    rest_C--;
                    break;
                case 'D':
                    rest_D--;
                    break;
            }

            /*
              if types of new card and card on old top is the same,
              move all cards to player
             */
            if (!(old_top == null) && old_top.charAt(0) == type) {
                Vector<String> player_card_group = host == 0 ? this.card_at_p1 : this.card_at_p2;

                while (!this.card_placement.empty()) {
                    String top_card = this.card_placement.pop();
                    player_card_group.add(top_card);

                    if (host == order) {
                        char ctype = top_card.charAt(0);
                        switch (ctype) {
                            case 'S':
                                own_S++;
                                break;
                            case 'H':
                                own_H++;
                                break;
                            case 'C':
                                own_C++;
                                break;
                            case 'D':
                                own_D++;
                                break;
                        }
                    }
                }

                res = " 并拿走了\u003c放置区\u003e的卡牌";
            }

            if (this.card_group <= 0) {
                if (this.card_at_p1.size() < this.card_at_p2.size()) this.winner = 0;
                else if (this.card_at_p1.size() > this.card_at_p2.size()) this.winner = 1;
                else this.winner = -1;
                this.finished = true;
            }

            String code = host + " 0 " + card;
            String msg = id_string + " " + op_string + " " + card;
            if (res != null) msg += res;

            this.log.add(code);
            this.log_msg.add(msg);


            return new Tuple<Boolean, String, String>(true, code, msg);
        } else {
            String old_top = this.card_placement.empty() ? null : this.card_placement.peek();
            Vector<String> player_card_group = host == 0 ? this.card_at_p1 : this.card_at_p2;

            this.card_placement.push(card);
            player_card_group.remove(card);

            if (host == order) {
                char type = card.charAt(0);
                switch (type) {
                    case 'S':
                        own_S--;
                        break;
                    case 'H':
                        own_H--;
                        break;
                    case 'C':
                        own_C--;
                        break;
                    case 'D':
                        own_D--;
                        break;
                }
            }

            if (!(old_top == null) && old_top.charAt(0) == card.charAt(0)) {
                while (!this.card_placement.empty()) {
                    String top_card = this.card_placement.pop();
                    player_card_group.add(top_card);

                    if (host == order) {
                        char ctype = top_card.charAt(0);
                        switch (ctype) {
                            case 'S':
                                own_S++;
                                break;
                            case 'H':
                                own_H++;
                                break;
                            case 'C':
                                own_C++;
                                break;
                            case 'D':
                                own_D++;
                                break;
                        }
                    }
                }
                res = " 并拿走了\u003c放置区\u003e的卡牌";
            }

            id_string = host == 0 ? "P1" : "P2";

            String code = host + " 1 " + card;
            String msg = id_string + " " + op_string + " " + card;
            if (res != null) msg += res;

            this.log.add(code);
            this.log_msg.add(msg);

            return new Tuple<Boolean, String, String>(true, code, msg);
        }
    }
}

/**
 * Player
 * Store player information and perform player actions
 */
public class Player {
    /******************************************
     *           server information           *
     ******************************************/
    public String server_login_ip = null;
    public int server_login_port = -1;
    public String server_game_ip = null;
    public int server_game_port = -1;

    public String token = null;
    public String uuid;
    public int host = 0;

    public String username;
    public String password;

    public AI ai;
    /******************************************
     *           thread controller            *
     ******************************************/
    /*
       Android must use a non-UI thread to use the network.
       Semaphore and lock are used for thread control
     */
    /* prevent players from making multiple requests to the server at the same time */
    private final static Lock lock = new ReentrantLock();
    private final static Lock plock = new ReentrantLock();
    private final static Lock llock = new ReentrantLock();
    /* waiting for network thread to return */
    private final Semaphore semaphore = new Semaphore(0, true);
    Object ret_value = null;

    OkHttpClient client = new OkHttpClient().newBuilder()
        .readTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build();

    /******************************************
     *                 game                   *
     ******************************************/
    public OneGame_Simple game = null;

    /******************************************
     *             basic function             *
     ******************************************/
    /**
     * Player initializing
     *
     * @param username user name login to server, randomly username and
     *                 password is ok if login to local server
     * @param password password
     */
    public Player(String username, String password) {
        this.username = username;
        this.password = password;
        this.ai = new AI(this);
    }

    /**
     * Set Login server
     *
     * @param ip   server ip
     * @param port server port
     */
    public void setLoginServer(String ip, int port) {
        this.server_login_ip = ip;
        this.server_login_port = port;
    }

    /**
     * Set game server
     *
     * @param ip   server ip
     * @param port server port
     */
    public void setGameServer(String ip, int port) {
        this.server_game_ip = ip;
        this.server_game_port = port;
    }

    /******************************************
     *             game controller            *
     ******************************************/
    /**
     * Login to server
     *
     * @return true: login successfully
     * false: failed to login in to server
     * @throws InterruptedException Thrown when a thread is waiting,
     *                              sleeping, or otherwise occupied, and the thread is interrupted.
     */
    public boolean login() throws InterruptedException {
        ret_value = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if (server_login_ip == null || server_login_port == -1) {
                        ret_value = false;
                        return;
                    }

                    String url = "http://" + server_login_ip + ":" + server_login_port + "/api/user/login/";
                    String urlParam = "student_id=" + username + "&password=" + password;


                    FormBody.Builder formBody = new FormBody.Builder();
                    formBody.add("student_id", username)
                        .add("password", password);

                    Request request = new Request.Builder()
                        .url(url)
                        .post(formBody.build())
                        .build();
                    Response response = null;

                    response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        /* GET 200/OK, return user token */
                        if (json.has("status") && json.getInt("status") == 200) {
                            token = json.getJSONObject("data")
                                .getString("token");
                            ret_value = true;
                        }
                        /* return 404/NOT_FOUND， error username or password */
                        else {
                            ret_value = false;
                        }
                    } else {
                        ret_value = false;
                    }

                    response.close();
                    response = null;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    /* unlock thread, tell main thread that result is ready */
                    lock.unlock();
                    semaphore.release();
                }
            }
        }, "Thread_login").start();

        /* waiting for network thread to return */
        semaphore.acquire();
        System.gc();
        return ret_value != null && (boolean) ret_value;
    }

    /**
     * Create a new game
     *
     * @param priv private status
     * @return uuid
     * @throws InterruptedException Thrown when a thread is waiting,
     *                              sleeping, or otherwise occupied, and the thread is interrupted
     */
    public String createGame(boolean priv) throws InterruptedException {
        ret_value = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    /* return if user haven't logged in */
                    if (server_game_ip == null || server_game_port == -1 || token == null) {
                        ret_value = null;
                        return;
                    }

                    String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/";
                    String urlParam = priv ? "{\"private\":true}" : "{\"private\":false}";
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    RequestBody body = RequestBody.create(JSON, urlParam);
                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", token)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();
                    Response response = null;

                    response = client.newCall(request).execute();

                    /* Successfully create a new game */
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        uuid = json.getJSONObject("data").getString("uuid");
                        ret_value = uuid;

                        /* set the player as host */
                        host = 0;
                        game = new OneGame_Simple();
                    }
                    /* return 401/UNAUTHORIZED */
                    else {
                        ret_value = null;
                    }

                    response.close();
                    response = null;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    /* unlock thread, tell main thread that result is ready */
                    lock.unlock();
                    semaphore.release();
                }
            }
        }, "Thread_createGame").start();

        /* waiting for network thread to return */
        semaphore.acquire();
        System.gc();
        return (String) ret_value;
    }

    /**
     * Join specific game
     *
     * @param _uuid game uuid
     * @return is joined the game
     * @throws InterruptedException Thrown when a thread is waiting,
     *                              sleeping, or otherwise occupied, and the thread is interrupted
     */
    public boolean joinGame(String _uuid) throws InterruptedException {
        ret_value = null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    /* error parameter */
                    if (server_game_ip == null || server_game_port == -1 || token == null || _uuid == null) {
                        ret_value = false;
                        return;
                    }

                    String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/" + _uuid;

                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", token)
                        .post(new FormBody.Builder().build())
                        .build();
                    Response response = null;
                    response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        ret_value = true;
                        uuid = _uuid;

                        /* set the player as guest */
                        host = 1;
                        game = new OneGame_Simple();
                    } else {
                        ret_value = false;
                    }

                    response.close();
                    response = null;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    /* unlock thread, tell main thread that result is ready */
                    lock.unlock();
                    semaphore.release();
                }
            }
        }, "Thread_joinGame").start();

        /* waiting for network thread to return */
        semaphore.acquire();
        System.gc();
        return ret_value != null && (boolean) ret_value;
    }

    /**
     * Perform user operation
     *
     * @param op   game operation. put a card or turn over from card group
     * @param card card
     * @return Tuple.first: last code
     * Tuple.second: last log message
     * Tuple.third: turn now
     * @throws InterruptedException Thrown when a thread is waiting,
     *                              sleeping, or otherwise occupied, and the thread is interrupted
     */
    public Tuple<Boolean, String, String> operate(GameOperation op, String card) throws InterruptedException {
        ret_value = new Tuple<Boolean, String, String>(false, new String(), new String());
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    ret_value = null;
                    if (server_game_ip == null || server_game_port == -1 || token == null || uuid == null || game == null) {
                        ret_value = new Tuple<Boolean, String, String>(false, "请创建或加入一个游戏", "");
                        return;
                    }

                    String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/" + uuid;
                    String urlParam = op == GameOperation.putCard ?
                        "{\"type\":1, \"card\":\"" + card + "\"}" :
                        "{\"type\":0}";

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    RequestBody body = RequestBody.create(JSON, urlParam);
                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", token)
                        .addHeader("Content-Type", "application/json")
                        .put(body)
                        .build();
                    Response response = client.newCall(request).execute();

                    /*
                       200/OK: success
                       403/FORBIDDEN: game is waiting or error operation
                     */
                    if (response.isSuccessful() || response.code() == 403) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);
                        JSONObject data = json.getJSONObject("data");

                        String last_code = data.getString("last_code");
                        String last_msg = data.getString("last_msg");

                        ret_value = new Tuple<Boolean, String, String>(response.code() == 200, last_code, last_msg);
                    } else {
                        ret_value = new Tuple<Boolean, String, String>(false, "请求超时", "");
                    }

                    response.close();
                    response = null;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                    semaphore.release();
                }
            }
        }, "Thread_operate").start();

        semaphore.acquire();
        System.gc();
        return (Tuple<Boolean, String, String>) ret_value;
    }

    /**
     * Get last operation code to update game. judge whether is player's turn
     *
     * @return true: is player's turn
     * false: is not player's turn
     * @throws InterruptedException Thrown when a thread is waiting,
     *                              sleeping, or otherwise occupied, and the thread is interrupted
     */
    public boolean getLast() throws InterruptedException {
        llock.lock();
        try {
            ret_value = null;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    try {
                        if (server_game_ip == null || server_game_port == -1 || token == null || uuid == null || game == null) {
                            ret_value = new Tuple<Boolean, String, String>(false, "请创建或加入一个游戏", "");
                            return;
                        }

                        String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/" + uuid + "/last";

                        Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", token)
                            .addHeader("Connection", "close")
                            .get()
                            .build();
                        Response response = client.newCall(request).execute();
                        try {

                            /* game is playing or over */
                            if (response.isSuccessful() || response.code() == 400) {
                                String res = response.body().string();
                                JSONObject json = new JSONObject(res);
                                JSONObject data = json.getJSONObject("data");

                                if (json.getInt("code") == 200) {
                                    String last_code = data.getString("last_code");
                                    boolean your_turn = data.getBoolean("your_turn");

                                    /* perform the player's steps in the locally stored game */
                                    String[] code = last_code.trim().split(" ");
                                    if (last_code.length() != 0 && (
                                        /* game is just begin */
                                        game.log_msg.isEmpty() || (
                                            /* block duplicate steps */
                                            !game.log.isEmpty() && !game.log.lastElement().equals(last_code))
                                    )
                                    ) {
                                        if (code[1].equals("0")) {
                                            game.operate(Integer.parseInt(code[0]), host, GameOperation.turnOver, code[2]);
//                                        game.operate((host + 1 % 2), host, GameOperation.turnOver, code[2]);
                                        } else {
                                            game.operate(Integer.parseInt(code[0]), host, GameOperation.putCard, code[2]);
//                                        game.operate((host + 1 % 2), host, GameOperation.putCard, code[2]);
                                        }
                                    }
                                    ret_value = your_turn;
                                }

                            /*
                               for the server will not return last code if game is over,
                               we should find out which card was turned over
                             */
                                if (json.getInt("code") == 400) {
                                    Log.v("code", "400end");

                                    if (game.card_group == 1) {
                                        int base = 0;
                                        if (game.rest_S == 1) base = 0;
                                        else if (game.rest_H == 1) base = 13;
                                        else if (game.rest_C == 1) base = 26;
                                        else if (game.rest_D == 1) base = 39;

                                        /* final card must be not in placement or player's card */
                                        for (int i = base; i < base + 13; i++) {
                                            String final_card = OneGame_Simple.card_new[i];
                                            if (!game.card_at_p1.contains(final_card) &&
                                                !game.card_at_p2.contains(final_card) &&
                                                !game.card_placement.contains(final_card)) {
                                                game.operate((host + 1) % 2, host, GameOperation.turnOver, final_card);
                                                break;
                                            }
                                        }
                                    }
                                    ret_value = true;
                                }

                            } else {
                                ret_value = false;
                            }
                            response.body().close();
                        } finally {

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                        semaphore.release();
                        System.gc();
                    }
                }
            }, "Thread_getLast");

            thread.start();
            semaphore.acquire();
            thread.interrupt();
            System.gc();
            return ret_value == null ? false : (boolean) ret_value;
        } finally {
            llock.unlock();
        }
    }

    /**
     * Operate and update game stored locally
     *
     * @param op   game operation. put a card or turn over from card group
     * @param card card
     * @return Tuple.first: last code
     * Tuple.second: last log message
     * Tuple.third: turn now
     * @throws InterruptedException Thrown when a thread is waiting,
     *                              sleeping, or otherwise occupied, and the thread is interrupted
     */
    public Tuple<Boolean, String, String> operate_update(GameOperation op, String card) throws InterruptedException {
        plock.lock();
        try {
            Tuple<Boolean, String, String> ret = operate(op, card);
            /* perform the player's steps in the locally stored game */
            if (ret != null && ret.first == true) {
                String[] code = ret.second.trim().split(" ");
                if (ret.second.length() != 0 && (
                    /* game is just begin */
                    game.log.isEmpty() || (
                        /* block duplicate steps */
                        !game.log.isEmpty() && !game.log.lastElement().equals(ret.second))
                )
                ) {
                    if (code[1].equals("0")) {
                        game.operate(Integer.parseInt(code[0]), host, GameOperation.turnOver, code[2]);
//                        game.operate(host, host, GameOperation.turnOver, code[2]);
                    } else {
                        game.operate(Integer.parseInt(code[0]), host, GameOperation.putCard, code[2]);
//                        game.operate(host, host, GameOperation.putCard, code[2]);
                    }
                }
            }
            Log.v("play", this.host + " rest group " + game.card_group);
            return ret;
        } finally {
            plock.unlock();
        }
    }

    public Vector<String> gameList() throws InterruptedException {

        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    ret_value = null;
                    if (server_game_ip == null || server_game_port == -1 || token == null) {
                        ret_value = new Tuple<Boolean, String, String>(false, "", "");
                        return;
                    }

                    String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/index"
                        + "?page_size=10&page_num=1";

                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", token)
                        .addHeader("Connection", "close")
                        .get()
                        .build();
                    Response response = client.newCall(request).execute();

                    /*
                       200/OK: success
                       403/FORBIDDEN: game is waiting or error operation
                     */
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);
                        JSONObject data = json.getJSONObject("data");
                        int tot = data.getInt("total_page_num");

                        url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/index"
                            + "?page_size=10&page_num=" + tot;

                        request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", token)
                            .addHeader("Connection", "close")
                            .get()
                            .build();

                        response = client.newCall(request).execute();

                        Vector<String> glist = new Vector<String>();

                        if (response.isSuccessful()) {
                            res = response.body().string();
                            json = new JSONObject(res);
                            data = json.getJSONObject("data");
                            JSONArray list = data.getJSONArray("games");

                            for (int i = 0; i < list.length(); i++) {
                                JSONObject _game = list.getJSONObject(i);
                                glist.add(_game.getString("uuid"));
                            }
                        }

                        ret_value = glist;
                    } else {
                        ret_value = new Vector<String>();
                    }

                    response.close();
                    response = null;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                    semaphore.release();
                }
            }
        }, "Thread_gameList").start();

        semaphore.acquire();
        System.gc();
        return (Vector<String>)ret_value;
    }

    /**
     * Determine whether game is over
     *
     * @return true: finished
     * false: unfinished
     */
    public boolean isGameOver() {
        return this.game.finished;
    }

    /**
     * Determine whether the player is winner
     *
     * @return true: win
     * false: lose or draw
     */
    public boolean isWinner() {
        if (isGameOver()) {
            return this.host == game.winner;
        }
        return false;
    }

    public int getWinner() {
        return game.winner;
    }

    public Vector<String> get_card_host() {
        return game.card_at_p1;
    }

    public Vector<String> get_card_guest() {
        return game.card_at_p2;
    }

    public String get_top_card() {
        return game.card_placement.isEmpty() ? null : game.card_placement.peek();
    }

    public boolean get_card_group() {
        return game.card_group != 0;
    }

    public String get_msg() {
        return game.log_msg.isEmpty() ? new String() : game.log_msg.lastElement();
    }

    public void __createGame() {
        game = new OneGame_Simple();
    }
}
