package com.emmm.poke.client;

import android.util.Log;

import com.emmm.poke.utils.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
    }

    public Tuple<Boolean, String, String> operate(int host, GameOperation op, String card) {
        return new Tuple<Boolean, String, String>(false, new String(), new String());
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
    public int host;

    public String username;
    public String password;

    /******************************************
     *            game information            *
     ******************************************/
    private final Lock lock = new ReentrantLock();
    private final Semaphore semaphore = new Semaphore(0,true);
    Object ret_value = null;

    /******************************************
     *            game information            *
     ******************************************/
    private OneGame_Simple game;

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
        return (boolean)ret_value;
    }

    public String createGame(boolean priv) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if(server_game_ip == null || server_game_port == -1 || token == null){
                        ret_value = new String();
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

                    if(response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        uuid = json.getJSONObject("data").getString("uuid");
                        ret_value = uuid;
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
        }).run();

        semaphore.acquire();
        return (String)ret_value;
    }

    public boolean joinGame(String _uuid) {
        if(this.server_game_ip == null || this.server_game_port == -1 || this.token == null || this.uuid == null){
            return false;
        }

        String url = "http://" + this.server_game_ip + ":" + this.server_game_port + "/api/game/" + uuid;


        return false;
    }

    public Tuple<Boolean, String, String> operate(GameOperation op, String card) {
        return new Tuple<>(false, new String(), new String());
    }

    public boolean isGameOver() {
        return false;
    }

    public boolean isWinner() {
        return false;
    }
}
