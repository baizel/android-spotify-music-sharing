package com.baizelmathew.spotifycontroller;

import com.baizelmathew.spotifycontroller.web.utils.OnFailSocketCallBack;
import com.baizelmathew.spotifycontroller.web.WebServer;

import org.java_websocket.WebSocket;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        try {
            WebServer s = WebServer.getInstance(new OnFailSocketCallBack() {
                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {

                }

                @Override
                public void onError(WebSocket conn, Exception ex) {

                }
            });
            s.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(4, 2 + 2);
    }
}