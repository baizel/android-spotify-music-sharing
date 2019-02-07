package com.baizelmathew.spotifycontroller.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public final class DataInjector {

    private DataInjector() {
    }

    public static String injectData(Context context, String fileName, HashMap<String, String> data) throws IOException {
        InputStream inputStream = context.getAssets().open(fileName);
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder total = new StringBuilder();
        for (String line; (line = r.readLine()) != null; ) {
            for (String key : data.keySet()) {
                String val = data.get(key);
                line = line.replace("{{"+key+"}}", val != null ? val : "Error");
            }
            total.append(line).append('\n');
        }
        return total.toString();

    }
}
