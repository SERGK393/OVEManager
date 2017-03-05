package ru.ovecfo.ovemanager;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    String mLogin=null;
    String mAccessKey=null;
    String mPhoneCode=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TabHost tabHost = (TabHost) findViewById(R.id.tabHost);

        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec(getResources().getString(R.string.tab1_tag));
        tabSpec.setContent(R.id.tab1);
        ImageView imageView1=new ImageView(getApplicationContext());
        imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
        tabSpec.setIndicator(imageView1);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec(getResources().getString(R.string.tab2_tag));
        tabSpec.setContent(R.id.tab2);
        ImageView imageView2=new ImageView(getApplicationContext());
        imageView2.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
        tabSpec.setIndicator(imageView2);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec(getResources().getString(R.string.tab3_tag));
        tabSpec.setContent(R.id.tab3);
        ImageView imageView3=new ImageView(getApplicationContext());
        imageView3.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
        tabSpec.setIndicator(imageView3);
        tabHost.addTab(tabSpec);

        tabHost.setCurrentTab(0);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                try {
                    ActionBar actionBar = getSupportActionBar();
                    actionBar.setShowHideAnimationEnabled(false);
                    if (tabId.equals(getResources().getString(R.string.tab1_tag))) {
                        actionBar.hide();
                    }else actionBar.show();
                    if (tabId.equals(getResources().getString(R.string.tab2_tag))) {
                        actionBar.setTitle(R.string.tab2_title);
                    }else actionBar.setTitle(R.string.app_name);
                }catch (NullPointerException e){
                    Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        WebView webView=(WebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        WebViewClient client = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                view.loadUrl(url);
                return true;
            }
        };
        webView.setWebViewClient(client);
        webView.loadUrl("http://ove-cfo.ru/eshop/");



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


    private class ParseTask extends AsyncTask<String, Void, String> {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";

        @Override
        protected String doInBackground(String... params) {
            // получаем данные с внешнего ресурса
            try {
                URL url = new URL("http://ove-cfo.ru/remote-store/"+params[0]+"/query.json");

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                JSONObject json=new JSONObject();
                json.put("action","blablabla_дадада");

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
}
