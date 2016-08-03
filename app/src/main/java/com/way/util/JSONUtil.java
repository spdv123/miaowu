package com.way.util;

import android.os.Bundle;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BufferedHeader;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by deva on 16/7/30.
 */
public class JSONUtil {
    public static String getHtml(String URL) {
        HttpClient httpClient = new DefaultHttpClient();
        StringBuilder stringBuilder = new StringBuilder();
        HttpGet httpGet = new HttpGet(URL);
        try {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int resCode = httpResponse.getStatusLine().getStatusCode();
            if(resCode == 200) {
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(httpResponse.getEntity().getContent())
                );
                for(String s = bufferedReader.readLine(); s!=null; s=bufferedReader.readLine()) {
                    stringBuilder.append(s);
                }
                String html = new String(stringBuilder.toString().getBytes(), "UTF-8");
                return html;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getJSONFrom(String URL) {
        String html = getHtml(URL);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(html);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /** Convert a JSON object to a Bundle that can be passed as the extras of
     * an Intent. It passes each number as a double, and everything else as a
     * String, arrays of those two are also supported. */
    public static Bundle fromJson(JSONObject s) {
        Bundle bundle = new Bundle();

        for (Iterator<String> it = s.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONArray arr = s.optJSONArray(key);
            int num = s.optInt(key);
            String str = s.optString(key);

            if (arr != null && arr.length() <= 0)
                bundle.putStringArray(key, new String[]{});
            else if (arr != null && !Double.isNaN(arr.optDouble(0))) {
                double[] newarr = new double[arr.length()];
                for (int i=0; i<arr.length(); i++)
                    newarr[i] = arr.optDouble(i);
                bundle.putDoubleArray(key, newarr);
            }
            else if (arr != null && arr.optString(0) != null) {
                String[] newarr = new String[arr.length()];
                for (int i=0; i<arr.length(); i++)
                    newarr[i] = arr.optString(i);
                bundle.putStringArray(key, newarr);
            }
            else if (num != 0)
                bundle.putInt(key, num);
            else if (str != null)
                bundle.putString(key, str);
            else
                System.err.println("unable to transform json to bundle " + key);
        }
        return bundle;
    }
}
