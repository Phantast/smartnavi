package com.ilm.sandwich.tools;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Markus Kniep | 2imba
 *         <p/>
 *         This class is the basis for HTTP requests within an asynchroneous task. UI changes resulting from the AsyncTask have to be made on the UI Thread.
 *         Create a intern class which extends AsyncTask and do UI changes in the onPostExecute()
 * @usage public class SendData extends AsyncTask <Void, Void, String> { ... }
 */
public class HttpRequests {

    private List<BasicNameValuePair> postingData;
    private String response = null;
    private URI uri;
    private String header = null;
    private String requestMethod;

    public HttpRequests() {
        postingData = new ArrayList<BasicNameValuePair>(1);
        // standard method post
        requestMethod = "POST";
    }

    /**
     * specifies the adress of a server/webpage/script where the request will be executed
     *
     * @param _uri URL adress
     */
    public void setURL(String _uri) {

        try {
            uri = new URI(_uri);
        } catch (URISyntaxException e) {
            //e.printStackTrace();
        }
    }

    /**
     * set the user agent header for the HTTP request
     *
     * @param _header User Agent
     */
    public void setHeader(String _header) {
        header = _header;
    }

    /**
     * set the request method to "GET" or "POST"
     *
     * @param _requestMethod Request Type
     */
    public void setMethod(String _requestMethod) {
        requestMethod = _requestMethod;
    }

    /**
     * set post variables and values
     *
     * @param varName name of variable
     * @param value   value of variable
     */
    public void addValue(String varName, String value) {
        postingData.add(new BasicNameValuePair(varName, value));
    }

    public void setBasicNameValuePairs(List<BasicNameValuePair> _data) {
        postingData = _data;
    }

    /**
     * execute the http server request
     *
     * @return response contains the server message
     */
    public String doRequest() {

        DefaultHttpClient httpclient = new DefaultHttpClient();

        response = null;

        if (requestMethod.equals("GET")) {

            // build typical GET url from postingData
            StringBuilder urlString = new StringBuilder();

            for (int i = 0; i < postingData.size(); i++) {
                urlString.append("" + postingData.get(i).getName() + "=" + postingData.get(i).getValue());
                if (i < postingData.size() - 1)
                    urlString.append("&");
            }

            String getURI = uri + "?" + urlString;

            HttpGet httpReq = new HttpGet(getURI);

            try {

                if (header != null)
                    httpReq.setHeader("User-Agent", header);

                // execute HTTP GET request
                HttpResponse httpResponse = httpclient.execute(httpReq);
                HttpEntity httpEntity = httpResponse.getEntity();
                response = EntityUtils.toString(httpEntity);

            } catch (Exception e) {
                // e.printStackTrace();
                response = "Error: " + e;
            }

        } else if (requestMethod.equals("POST")) {
            HttpPost httpReq = new HttpPost(uri);

            try {

                httpReq.setEntity(new UrlEncodedFormEntity(postingData));
                if (header != null)
                    httpReq.setHeader("User-Agent", header);

                // execute HTTP POST request
                HttpResponse httpResponse = httpclient.execute(httpReq);
                HttpEntity httpEntity = httpResponse.getEntity();
                response = EntityUtils.toString(httpEntity);

            } catch (Exception e) {
                // e.printStackTrace();
                response = "Error: " + e;
            }
        }

        return response;
    }
}