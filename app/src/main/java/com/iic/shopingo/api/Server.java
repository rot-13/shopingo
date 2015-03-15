package com.iic.shopingo.api;

import android.net.Uri;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iic.shopingo.BuildConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

/**
 * Created by assafgelber on 3/10/15.
 */
public class Server {
  private static final String HEADER_AUTH_KEY = "X-WYAI-FBID";

  private static final Uri BASE_URI = Uri.parse(BuildConfig.SERVER_HOST);

  private final HttpClient client = new DefaultHttpClient();

  private String authToken;

  public Server() {}

  public Server(String authToken) {
    this.authToken = authToken;
  }

  public <T> T get(String path, Class<T> responseClass) throws IOException {
    return get(path, responseClass, new HashMap<String, Object>());
  }

  public <T> T get(String path, Class<T> responseClass, Map<String, Object> params) throws IOException {
    Uri.Builder requestUriBuilder = BASE_URI.buildUpon().encodedPath(path);
    for (String key : params.keySet()) {
      requestUriBuilder.appendQueryParameter(key, params.get(key).toString());
    }
    HttpGet request = new HttpGet(requestUriBuilder.build().toString());
    addAuthData(request);
    return executeRequest(request, responseClass);
  }

  public <T> T post(String path, Class<T> responseClass) throws IOException {
    return post(path, responseClass, new HashMap<String, Object>());
  }

  public <T> T post(String path, Class<T> responseClass, Map<String, Object> params) throws IOException {
    Uri.Builder requsetUriBuilder = BASE_URI.buildUpon().encodedPath(path);
    HttpPost request = new HttpPost(requsetUriBuilder.build().toString());
    request.setHeader("Content-Type", "application/json");
    request.setHeader("Accept", "application/json");
    addAuthData(request);
    JSONObject obj = new JSONObject(params);
    request.setEntity(new StringEntity(obj.toString()));
    return executeRequest(request, responseClass);
  }

  private void addAuthData(HttpUriRequest request) {
    if (authToken != null) {
      request.addHeader(HEADER_AUTH_KEY, authToken);
    }
  }

  private <T> T executeRequest(HttpUriRequest request, Class<T> responseClass) throws IOException {
    InputStream content = null;
    try {
      HttpResponse response = client.execute(request);
      content = response.getEntity().getContent();
      Reader reader = new InputStreamReader(content);
      Gson gson = new GsonBuilder().create();
      T result = gson.fromJson(reader, responseClass);
      content.close();
      return result;
    } catch (Exception e) {
      throw e;
    } finally {
      if (content != null) {
        content.close();
      }
    }
  }
}
