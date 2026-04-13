package com.ilm.sandwich.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP request utility using HttpURLConnection (replaces deprecated Apache HttpClient).
 * Designed to be called from a background thread.
 */
public class HttpRequests {

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 15000;

    private final List<String[]> postingData;
    private String response = null;
    private String urlString;
    private String header = null;
    private String requestMethod;

    public HttpRequests() {
        postingData = new ArrayList<>();
        requestMethod = "POST";
    }

    public void setURL(String url) {
        this.urlString = url;
    }

    public void setHeader(String _header) {
        header = _header;
    }

    public void setMethod(String _requestMethod) {
        requestMethod = _requestMethod;
    }

    public void addValue(String varName, String value) {
        postingData.add(new String[]{varName, value});
    }

    public String doRequest() {
        response = null;
        try {
            if ("GET".equals(requestMethod)) {
                response = doGet();
            } else if ("POST".equals(requestMethod)) {
                response = doPost();
            }
        } catch (Exception e) {
            response = "Error: " + e;
        }
        return response;
    }

    private String doGet() throws IOException {
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < postingData.size(); i++) {
            String[] pair = postingData.get(i);
            query.append(URLEncoder.encode(pair[0], "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(pair[1], "UTF-8"));
            if (i < postingData.size() - 1) query.append("&");
        }
        String fullUrl = urlString + "?" + query;

        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        if (header != null) conn.setRequestProperty("User-Agent", header);

        try {
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    private String doPost() throws IOException {
        StringBuilder postData = new StringBuilder();
        for (int i = 0; i < postingData.size(); i++) {
            String[] pair = postingData.get(i);
            postData.append(URLEncoder.encode(pair[0], "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(pair[1], "UTF-8"));
            if (i < postingData.size() - 1) postData.append("&");
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setDoOutput(true);
        if (header != null) conn.setRequestProperty("User-Agent", header);

        try {
            byte[] postBytes = postData.toString().getBytes("UTF-8");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postBytes);
                os.flush();
            }
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
