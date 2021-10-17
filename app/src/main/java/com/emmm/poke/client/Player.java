package com.emmm.poke.client;

import android.util.Log;

import com.emmm.poke.utils.*;

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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;


class OneGame_Simple {
    public int card_group;
    public Stack<String> card_placement;
    public int rest_S;
    public int rest_H;
    public int rest_C;
    public int rest_D;

    private Vector<String> card_at_p1;
    private Vector<String> card_at_p2;
    public int own_S;
    public int own_H;
    public int own_C;
    public int own_D;

    public int winner;
    public boolean finished;

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



        log = new Vector<String>();
        log_msg = new Vector<String>();
    }

    public Tuple<Boolean, String, String> operate(int host, GameOperation op, String card) {
        String id_string = host == 0 ? "P1" : "P2";
        String op_string = op == GameOperation.turnOver ? "从\u003c牌库\u003e翻开了一张" : "从\u003c手牌\u003e打出了一张";
        String res = null;

        if (op == GameOperation.turnOver) {
            String old_top = this.card_placement.empty() ? null : this.card_placement.peek();
            card_group -= 1;
            this.card_placement.push(card);

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

            if (!(old_top == null) && old_top.charAt(0) == type) {
                Vector<String> player_card_group = host == 0 ? this.card_at_p1 : this.card_at_p2;

                while (!this.card_placement.empty()) {
                    String top_card = this.card_placement.pop();
                    player_card_group.add(top_card);

                    if(host == 0) {
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
                if (this.card_at_p1.size() > this.card_at_p2.size()) this.winner = 0;
                else if (this.card_at_p1.size() < this.card_at_p2.size()) this.winner = 1;
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

            if (!(old_top == null) && old_top.charAt(0) == card.charAt(0)) {
                while (!this.card_placement.empty()) {
                    String top_card = this.card_placement.pop();
                    player_card_group.add(top_card);

                    if(host == 0) {
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

            String code = host + " 0 " + card;
            String msg = id_string + " " + op_string + " " + card;
            if (res != null) msg += res;

            this.log_msg.add(msg);

            return new Tuple<Boolean, String, String>(true, code, msg);
        }
    }
}

public class Player {
    /******************************************
     *           server information           *
     ******************************************/
    public String server_login_ip = null;
    public int server_login_port = -1;
    public String server_game_ip = null;
    public int server_game_port = -1;

    private String token = null;
    public String uuid;
    public int host = 0;

    public String username;
    public String password;

    /******************************************
     *            game information            *
     ******************************************/
    private final Lock lock = new ReentrantLock();
    private final Semaphore semaphore = new Semaphore(0, true);
    Object ret_value = null;

    /******************************************
     *            game information            *
     ******************************************/
    private OneGame_Simple game = null;

    /******************************************
     *             basic function             *
     ******************************************/
    public Player(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setLoginServer(String ip, int port) {
        this.server_login_ip = ip;
        this.server_login_port = port;
    }

    public void setGameServer(String ip, int port) {
        this.server_game_ip = ip;
        this.server_game_port = port;
    }

    /******************************************
     *             game controller            *
     ******************************************/
    public boolean login() throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if (server_login_ip == null || server_login_port == -1) {
                        ret_value = false;
                    }

                    String url = "http://" + server_login_ip + ":" + server_login_port + "/api/user/login/";
                    String urlParam = "student_id=" + username + "&password=" + password;

                    OkHttpClient client = new OkHttpClient();

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

                        token = json.getJSONObject("data")
                                .getString("token");

                        ret_value = true;
                    } else {
                        ret_value = false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                    semaphore.release();
                }
            }
        }).start();

        semaphore.acquire();
        return (boolean) ret_value;
    }

    public String createGame(boolean priv) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if (server_game_ip == null || server_game_port == -1 || token == null) {
                        ret_value = null;
                    }

                    String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/";
                    String urlParam = priv ? "{\"private\":true}" : "{\"private\":false}";
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = RequestBody.create(JSON, urlParam);
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", token)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();
                    Response response = null;

                    response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        uuid = json.getJSONObject("data").getString("uuid");
                        ret_value = uuid;

                        host = 0;
                        game = new OneGame_Simple();
                    } else {
                        ret_value = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                    semaphore.release();
                }
            }
        }).start();

        semaphore.acquire();
        return (String) ret_value;
    }

    public boolean joinGame(String _uuid) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if (server_game_ip == null || server_game_port == -1 || token == null || _uuid == null) {
                        ret_value = false;
                    }

                    String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/" + _uuid;

                    OkHttpClient client = new OkHttpClient();
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

                        host = 1;
                        game = new OneGame_Simple();
                    } else {
                        ret_value = false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                    semaphore.release();
                }
            }
        }).start();

        semaphore.acquire();
        return (boolean) ret_value;
    }

    public Tuple<Boolean, String, String> operate(GameOperation op, String card) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if (server_game_ip == null || server_game_port == -1 || token == null || uuid == null || game == null) {
                        ret_value = new Tuple<Boolean, String, String>(false, "请创建或加入一个游戏", "");
                    }

                    String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/" + uuid;
                    String urlParam = op == GameOperation.putCard ?
                            "{\"type\":1, \"card\":\"" + card + "\"}" :
                            "{\"type\":0}";

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = RequestBody.create(JSON, urlParam);
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", token)
                            .addHeader("Content-Type", "application/json")
                            .put(body)
                            .build();
                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful() || response.code() == 403) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);
                        JSONObject data = json.getJSONObject("data");

                        String last_code = data.getString("last_code");
                        String last_msg = data.getString("last_msg");

                        ret_value = new Tuple<Boolean, String, String>(true, last_code, last_msg);
                    } else {
                        ret_value = new Tuple<Boolean, String, String>(false, "请求超时", "");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                    semaphore.release();
                }
            }
        }).start();

        semaphore.acquire();
        return (Tuple<Boolean, String, String>) ret_value;
    }

    public boolean getLast() throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if (server_game_ip == null || server_game_port == -1 || token == null || uuid == null || game == null) {
                        ret_value = new Tuple<Boolean, String, String>(false, "请创建或加入一个游戏", "");
                    }

                    String url = "http://" + server_game_ip + ":" + server_game_port + "/api/game/" + uuid + "/last";

                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", token)
                            .get()
                            .build();
                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);
                        JSONObject data = json.getJSONObject("data");

                        String last_code = data.getString("last_code");
                        boolean your_turn = data.getBoolean("your_turn");

                        String[] code = last_code.trim().split(" ");
                        if (last_code.length() != 0 && your_turn && (game.log.isEmpty() || (!game.log.isEmpty() && !game.log.lastElement().equals(last_code)))) {
                            game.log.add(last_code);
                            if (code[1].equals("0")) {
                                game.operate(Integer.parseInt(code[0]), GameOperation.turnOver, code[2]);
                            } else {
                                game.operate(Integer.parseInt(code[0]), GameOperation.putCard, code[2]);
                            }
                        }

                        ret_value = your_turn;
                    } else {
                        ret_value = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                    semaphore.release();
                }
            }
        }).start();

        semaphore.acquire();
        return (boolean) ret_value;
    }

    public boolean isGameOver() {
        return game.finished;
    }

    public boolean winner() {
        if(isGameOver()){
            return this.host == game.winner;
        }
        return false;
    }
}
