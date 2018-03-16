package bitwalking.bitwalking.server;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Pair;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import bitwalking.bitwalking.AppPreferences;
import bitwalking.bitwalking.BitwalkingApp;
import bitwalking.bitwalking.BuildConfig;
import bitwalking.bitwalking.R;
import bitwalking.bitwalking.registration_and_login.Utilities;
import bitwalking.bitwalking.server.requests.BasicServerRequest;
import bitwalking.bitwalking.server.responses.BasicServerResponse;
import bitwalking.bitwalking.util.Globals;
import bitwalking.bitwalking.util.Logger;
import bitwalking.bitwalking.util.RFC3339DateFormat;

/**
 * Created by Marcus on 6/20/16.
 */
public enum BitwalkingServer implements Runnable {

    INSTANCE;

    //region Constants

    public enum HttpMethod {
        GET,
        PUT,
        DELETE,
        POST
    }

    public class ContentType {
        public static final String JSON = "application/json;charset=UTF-8";
        public static final String DATA = "multipart/form-data;";
    }

    public static final String SERVER_URL = (BuildConfig.DEBUG && true) ? "http://api.dev.bitwalking.com" : "https://api.bitwalking.com";
    public static final String ACTION_PATH = "/action";
    public static final String REGISTRATION_PATH = "/action/register";
    public static final String AUTHENTICATION_PATH = "/action/auth";
    public static final String ME_PATH = "/model/me";
    public static final String STEPS_PATH = ME_PATH + "/activity/steps";
    public static final String INVITE_PATH = "/action/invite";
    public static final String STORE_PATH = "/model/store";
    public static final String SURVEY_PATH = "/model/store/survey";
    public static final String EVENTS_PATH = "/model/events";
    public static final String EVENTS_ACTION_PATH = "/action/events";
    public static final String PAYMENTS_REQUESTS_ACTION_PATH = "/action/transactions";
    public static final String WALLET_PATH = "";
    public static final String NOTIFICATION_PATH = "/model/notifications";

    private static final String API_VERSION = "/v1.0";

    //endregion

    private Context _context;
    public static void setContext(Context context) {
        INSTANCE._context = context;

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            INSTANCE.appVersion = pInfo.versionName;
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to get app version");
        }
    }

    private static final String TAG = BitwalkingServer.class.getSimpleName();
    private String appVersion = "empty";
    private Gson _gson;

    // Server connection members
    private KeyStore _serverKeyStore;
    private KeyStore _clientKeyStore, _clientTrustStore;
    private SSLContext _sslContext;
    static private SecureRandom _secureRandom;

    // Request members
    private ArrayList<ServerHttpReqInfo> _pendingRequests = new ArrayList();

    BitwalkingServer() {
        _gson = new Gson();
        new Thread(this).start();
    }

    public BasicServerResponse sendRequestNow(ServerHttpReqInfo request) throws IOException {
        return handleHttpRequest(request);
    }

    public void addRequest(ServerHttpReqInfo request) {
        synchronized (_pendingRequests) {
            _pendingRequests.add(request);
        }
    }

    public String encodeUrlParameters(List<Pair<String, String>> params) {
        StringBuilder paramsString = new StringBuilder();

        try {
            boolean first = true;
            for (Pair<String, String> p : params) {
                if (!first)
                    paramsString.append("&");
                paramsString.append(p.first).append("=").append(URLEncoder.encode(p.second, "UTF-8"));
                first = false;
            }
        }
        catch (Exception e) {
            Logger.instance().Log(Logger.ERROR, TAG, "failed to encode url params");
            BitwalkingApp.getInstance().trackException(e);
        }

        return paramsString.toString();
    }

    private void handleNextRequest() {
        try {
            ServerHttpReqInfo nextRequest = null;

            synchronized (_pendingRequests) {
                if (_pendingRequests.size() > 0) {
                    nextRequest = _pendingRequests.remove(0);
                }
            }

            if (null != nextRequest) {
                BasicServerResponse res = handleHttpRequest(nextRequest);

                if (null != nextRequest.callback) {
                    nextRequest.callback.onBasicResponse(res, nextRequest.requestId);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Logger.instance().Log(Logger.ERROR, TAG, "failed to send request");
        }
    }

    private String getClientLanguage() {
        String lang = Locale.getDefault().getDisplayLanguage(Locale.US);
        return lang;
    }

    private HttpURLConnection openConnection(String path, String contentType, HttpMethod method, HashMap<String, String> extraHeaders, String userId, String userSecret)
            throws IOException {
        String dataUrl = SERVER_URL + API_VERSION + path;
        HttpURLConnection connection = (HttpURLConnection) new URL(dataUrl).openConnection();

        connection.setRequestMethod(method.toString());
        connection.setReadTimeout(60000 /* milliseconds */);
        connection.setConnectTimeout(20000 /* milliseconds */);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Device-ID", Utilities.getDeviceItentity(_context));
        connection.setRequestProperty("Client-Version", appVersion);
        connection.setRequestProperty("Client-Language", getClientLanguage());
        connection.setRequestProperty("Device-Model", Build.MODEL);
        connection.setRequestProperty("Platform", "Android");
        connection.setRequestProperty("Platform-Version", Globals.getOsVersion());
        if (null != userSecret)
            connection.setRequestProperty("User-Secret", userSecret);
        if (null != userId)
            connection.setRequestProperty("User-ID", userId);

        String strRFC3339 =new RFC3339DateFormat().format(new Date());
        connection.setRequestProperty("Client-Time", strRFC3339);

        // Add extra headers if there are...
        if (null != extraHeaders && extraHeaders.size() > 0) {
            for (Map.Entry<String, String> h : extraHeaders.entrySet()) {
                connection.setRequestProperty(h.getKey(), h.getValue());
            }
        }

        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.connect();

        return connection;
    }

    private BasicServerResponse handleHttpRequest(ServerHttpReqInfo req) throws IOException {
        BasicServerResponse res = null;
        InputStream is = null;
        String resJson = null;
        int responseCode = 444;
        try {
            // Create connection
            HttpURLConnection connection = openConnection(req.path, req.contentType, req.method, req.extraHeaders, req.userId, req.userSecret);
            // Send request
            if (null != req.body) {
                Logger.instance().Log(Logger.DEBUG, TAG, "request path: " + req.path);
                String bodyString = new String(req.body, "UTF-8");

                try {
                    if (bodyString.contains("password")) {
                        bodyString = bodyString.replaceFirst("\"password.+\"", "\"***\": ***,");
                    }
                } catch (Exception e) {
                }

                Logger.instance().Log(Logger.DEBUG, TAG, "server http request: " + bodyString);
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(req.body);
                wr.flush();
                wr.close();
            }

            responseCode = connection.getResponseCode();

            Logger.instance().Log(Logger.DEBUG, TAG, "server http post response code: " + responseCode);
            boolean isError = connection.getResponseCode() >= 400;
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    isError ? connection.getErrorStream() : connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            resJson = response.toString();
            Logger.instance().Log(Logger.VERB, TAG, "response: " + resJson);
            res = _gson.fromJson(resJson, BasicServerResponse.class);
        }
        catch (UnknownHostException e1) {
            // probably no internet connection
            Logger.instance().Log(Logger.ERROR, TAG, e1.getMessage());
        }
        catch (SocketTimeoutException e2) {
            Logger.instance().Log(Logger.ERROR, TAG, e2.getMessage());
            BitwalkingApp.getInstance().trackException(e2);
        }
        catch (Exception e) {
            if (null != resJson)
                Logger.instance().Log(Logger.ERROR, TAG, resJson);
            e.printStackTrace();
        }
        finally {

            if (null == res) {
                // Json parse failed. create only response code
                res = new BasicServerResponse(responseCode);
            }
            else {
                res.setResponseCode(responseCode);
            }

            if (is != null) {
                is.close();
            }
        }

        if (res.getResponseCode() == 401) {
            // check if there should be user valid session
            if (null != req.userSecret && !req.userSecret.isEmpty() &&
                null != req.userId && !req.userId.isEmpty()) {
                // Logout user :\
                Logger.instance().Log(Logger.INFO, TAG, "got 401 response: " + req.path);
                new AppPreferences(_context).setForceLogout();
            }
        }
        else if (res.getResponseCode() == 403 &&
                 res.getHeader().code.equalsIgnoreCase("error.client.upgrade-required")) {
            Logger.instance().Log(Logger.INFO, TAG, "got force update response");
            Globals.notifyForceUpdate(_context);
        }

        return res;
    }

//    private void setupServerKeystore() throws GeneralSecurityException, IOException {
//        _clientTrustStore = KeyStore.getInstance("BKS");
//        InputStream serverKeyInput = _context.getResources().openRawResource(R.raw.clienttruststore);
//        _clientTrustStore.load(serverKeyInput, "client_keystore".toCharArray());
//
//        _clientKeyStore = KeyStore.getInstance("BKS");
//        InputStream serverTrustInput = _context.getResources().openRawResource(R.raw.client);
//        _clientKeyStore.load(serverTrustInput, "client_private".toCharArray());
//    }
//
//    private void setupSSLContext() throws GeneralSecurityException, IOException {
//        TrustManagerFactory trustManagerFactory =
//                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//        trustManagerFactory.init(_clientTrustStore);
//
//        KeyManagerFactory keyManagerFactory =
//                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//        keyManagerFactory.init(_clientKeyStore, "bitwalking_client".toCharArray());
//
//        _sslContext = SSLContext.getInstance("TLS");
//        _sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), _secureRandom);
//    }

    private void setupServerKeystore() throws GeneralSecurityException, IOException {
        _serverKeyStore = KeyStore.getInstance("BKS");
        InputStream serverKeyInput = _context.getResources().openRawResource(R.raw.server_public_bks);
        _serverKeyStore.load(serverKeyInput, "public".toCharArray());
    }

    private void setupSSLContext() throws GeneralSecurityException, IOException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(_serverKeyStore);

        _sslContext = SSLContext.getInstance("TLS");
        _sslContext.init(null, trustManagerFactory.getTrustManagers(), _secureRandom);
    }

    @Override
    public void run() {
        while (true) {
            try {
                handleNextRequest();
                Thread.sleep(100);
            }
            catch (Exception e) {
                Logger.instance().Log(Logger.ERROR, TAG, "next request handle failed");
                e.printStackTrace();
            }
        }
    }

    public static class ServerHttpReqInfo {
        private OnServerResponse callback;
        private int requestId;
        private String path;
        private HttpMethod method;
        private String contentType;
        private String userSecret;
        private String userId;
        private HashMap<String, String> extraHeaders;
        private byte[] body;

        public ServerHttpReqInfo(OnServerResponse callback,
                                 int requestId,
                                 String path,
                                 HttpMethod method,
                                 String contentType,
                                 HashMap<String, String> extraHeaders,
                                 String userId,
                                 String userSecret,
                                 BasicServerRequest req) {
            this.callback = callback;
            this.requestId = requestId;
            this.path = path;
            this.method = method;
            this.contentType = contentType;
            this.extraHeaders = extraHeaders;
            this.userId = userId;
            this.userSecret = userSecret;
            this.body = null;

            if (null != req && null != req.getBody())
                this.body = req.getBody().clone();
        }

        public ServerHttpReqInfo(String path,
                                 HttpMethod method,
                                 String contentType,
                                 String userId,
                                 String userSecret,
                                 BasicServerRequest req) {
            this(null, 0, path, method, contentType, null, userId, userSecret, req);
        }

        public ServerHttpReqInfo(OnServerResponse callback,
                                 String path,
                                 HttpMethod method,
                                 String contentType,
                                 String userId,
                                 String userSecret,
                                 BasicServerRequest req) {
            this(callback, 0, path, method, contentType, null, userId, userSecret, req);
        }
    }
}