package ru.ovecfo.ovemanager;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    WebView webView = null;
    WebView localWebView = null;
    ProgressBar progressBar = null;
    LinearLayout noNet = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTaskBarColored();

        webView=(WebView)findViewById(R.id.webView);
        localWebView=(WebView)findViewById(R.id.localWebView);
        progressBar=(ProgressBar)findViewById(R.id.progressBar);
        String uri = "https://www.ove-cfo.ru/mobile";
        String uri_local = "file:///android_asset/index.html";

        noNet=(LinearLayout)findViewById(R.id.noNet);
        Button noNetClose = (Button)findViewById(R.id.noNet_close);
        noNetClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        //webView.getSettings().setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
        webView.getSettings().setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
        webView.getSettings().setAllowFileAccess( true );
        webView.getSettings().setAppCacheEnabled( true );
        webView.getSettings().setCacheMode( WebSettings.LOAD_DEFAULT ); // load online by default

        if ( !isNetworkAvailable() ) { // loading offline
            webView.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
        }

        localWebView.getSettings().setJavaScriptEnabled(true);
        localWebView.loadUrl(uri_local);
        WebViewClient client = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                if(isNetworkAvailable()) {
                    view.loadUrl(url);
                    //else showErrorSplash();
                    new SaveHTMLTask().execute(url);
                }else new SaveHTMLTask().execute(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon){
                //viewFadeOut(view);
                viewShow(localWebView);
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView view, String url){
                //viewFadeIn(view);
                viewFadeOut(localWebView);
                progressBar.setVisibility(View.GONE);
            }
        };
        webView.setWebViewClient(client);
        if(isNetworkAvailable()) {
            webView.loadUrl(uri);
            //else showErrorSplash();
            new SaveHTMLTask().execute(uri);
        }else new SaveHTMLTask().execute(uri);

        try {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setShowHideAnimationEnabled(false);
            actionBar.hide();
            actionBar.setElevation(0);
        }catch (NullPointerException e){
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    public final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showErrorSplash(){
        progressBar.setVisibility(View.GONE);
        localWebView.setVisibility(View.VISIBLE);
        viewFadeIn(noNet);
    }

    public void setTaskBarColored() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            try {
                View view = findViewById(R.id.status_bar);
                view.setVisibility(View.VISIBLE);
                view.getLayoutParams().height=getStatusBarHeight();
                Window w = getWindow();
                w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    public void viewShow(View view)
    {
        view.clearAnimation();
        view.setAlpha(1);
        view.setVisibility(View.VISIBLE);
    }
    public void viewFadeIn(View view)
    {
        view.clearAnimation();
        view.setAlpha(1);
        AlphaAnimation animate = new AlphaAnimation(0,1);
        animate.setDuration(500);
        //animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.VISIBLE);
    }
    public void viewFadeOut(View view)
    {
        view.clearAnimation();
        view.setAlpha(1);
        view.setVisibility(View.VISIBLE);
        AlphaAnimation animate = new AlphaAnimation(1,0);
        animate.setDuration(500);
        //animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.GONE);
    }

    private class ParseTask extends AsyncTask<JSONObject, Void, String> {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";

        @Override
        protected String doInBackground(JSONObject... params) {
            // получаем данные с внешнего ресурса
            try {
                JSONObject json=params[0];

                URL url = new URL("http://ove-cfo.ru/remote-store/"+json.get("func")+"/query.json");

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("json", json.toString());
                String query = builder.build().getEncodedQuery();

                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();

                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                resultJson = buffer.toString();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultJson;
        }

        @Override
        protected void onPostExecute(String strJson) {
            super.onPostExecute(strJson);
            // выводим целиком полученную json-строку
            //Log.d(LOG_TAG, strJson);
            //TextView textView=(TextView)findViewById(R.id.textview);
            //textView.setText(strJson.concat(" is JSON."));

            JSONObject dataJsonObj = null;
            String secondName = "";

            try {
                dataJsonObj = new JSONObject(strJson);
                Toast.makeText(MainActivity.this, dataJsonObj.toString().concat(" is JSON."), Toast.LENGTH_SHORT).show();
                //textView.setText(dataJsonObj.toString().concat(" is JSON."));
                /*JSONArray friends = dataJsonObj.getJSONArray("friends");

                // 1. достаем инфо о втором друге - индекс 1
                JSONObject secondFriend = friends.getJSONObject(1);
                secondName = secondFriend.getString("name");
                Log.d(LOG_TAG, "Второе имя: " + secondName);

                // 2. перебираем и выводим контакты каждого друга
                for (int i = 0; i < friends.length(); i++) {
                    JSONObject friend = friends.getJSONObject(i);

                    JSONObject contacts = friend.getJSONObject("contacts");

                    String phone = contacts.getString("mobile");
                    String email = contacts.getString("email");
                    String skype = contacts.getString("skype");

                    Log.d(LOG_TAG, "phone: " + phone);
                    Log.d(LOG_TAG, "email: " + email);
                    Log.d(LOG_TAG, "skype: " + skype);
                }*/

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private class SaveHTMLTask extends AsyncTask<String, Void, String> {

        HttpsURLConnection urlConnection = null;
        BufferedReader reader = null;
        String urlString = "";

        @Override
        protected String doInBackground(String... params) {
            // получаем данные с внешнего ресурса
            String result = "";
            urlString=params[0];
            if(isNetworkAvailable()){
                try {

                    URL url = new URL(urlString);

                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("GET");

                    urlConnection.connect();

                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();

                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    result = buffer.toString();

                    int oveIndex = urlString.indexOf("ove-cfo.ru/mobile");
                    if(oveIndex>1) {
                        // отрываем поток для записи
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                                openFileOutput(md5(urlString)+"_cache.html", MODE_PRIVATE)));
                        // пишем данные
                        bw.write(result);
                        // закрываем поток
                        bw.close();
                    }

                } catch (Exception e) {
                    Log.e("OVE_ERROR_TAG----------",e.toString());
                    result = e.toString();
                    e.printStackTrace();
                }
            }else{
                int oveIndex = urlString.indexOf("ove-cfo.ru/mobile");
                if(oveIndex>1) {
                    try {
                        // открываем поток для чтения
                        BufferedReader br = new BufferedReader(new InputStreamReader(
                                openFileInput(md5(urlString)+"_cache.html")));
                        String str = "";
                        // читаем содержимое
                        while ((str = br.readLine()) != null) {
                            result += str;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        result = "Network is not avaliable " + e.getMessage();
                    }
                }else result = "Network is not avaliable 11 "+urlString;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String strResponse) {
            super.onPostExecute(strResponse);

            if(!isNetworkAvailable())
                if(strResponse.contains("Network is not avaliable"))
                    showErrorSplash();
                else webView.loadDataWithBaseURL(urlString, strResponse, "text/html", "ru_RU", null);
        }
    }
}
