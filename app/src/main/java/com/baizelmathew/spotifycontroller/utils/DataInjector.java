package com.baizelmathew.spotifycontroller.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Objects;

/**
 * Class to inject data into the HTML page. For example if the HTML page contains the code
 * ...
 * var token = '{{token}}'
 * ...
 * then by passing in the hasmap of map.put("token","abcd123") will inject that data into the page.
 * The above code will therefore become
 * ...
 * var token = 'abcd123'
 * ...
 */
public final class DataInjector {
    /**
     * Empty constructor because an instance is needed
     */
    public DataInjector() {
    }

    /**
     * Cant be static because it requires the class to get resources from the Raw folder
     *
     * @param fileName as a string which should be located in /res/raw/ and contain the HTML code
     * @param data     data to be injected as a hashmap of keys and values. the keys should be the variable name that is present in the HTML page
     * @return the file as a string with injected data
     * @throws NullPointerException throen when inputStream is null
     * @throws IOException when buffer reader fails
     */
    public String injectData(String fileName, HashMap<String, String> data) throws NullPointerException, IOException {
        InputStream inputStream = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("res/raw/" + fileName);
        if (inputStream == null)
            throw new NullPointerException("Error Reading file " + fileName);

        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder total = new StringBuilder();
        for (String line; (line = r.readLine()) != null; ) {
            for (String key : data.keySet()) {
                String val = data.get(key);
                line = line.replace("{{" + key + "}}", val != null ? val : "Error");
            }
            total.append(line).append('\n');
        }
        return total.toString();

    }
}
