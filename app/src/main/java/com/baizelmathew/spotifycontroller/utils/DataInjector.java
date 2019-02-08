package com.baizelmathew.spotifycontroller.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Objects;

public final class DataInjector {

    public DataInjector() {
    }

    public String injectData(String fileName, HashMap<String, String> data) throws NullPointerException, IOException {
        InputStream inputStream = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("res/raw/"+fileName);
        if (inputStream == null)
            throw new NullPointerException("Error Reading file "+fileName);

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
