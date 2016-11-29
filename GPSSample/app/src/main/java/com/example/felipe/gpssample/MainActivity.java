package com.example.felipe.gpssample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> items;
    ArrayAdapter<String> adapter;
    JSONObject jsonObject;
    JSONArray jsonArray;
    String JSON_STRING;
    Spinner listView;
    Context context;
    BackgroundTask backgroundTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (Spinner) findViewById(R.id.listView);
        items = new ArrayList<>();

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
        backgroundTask =  new BackgroundTask(context);
        backgroundTask.execute();

        context = this;
    }

    class BackgroundTask extends AsyncTask<Void,Void,String> {

        Context ctx;
        String URLconsulta="";

        BackgroundTask(Context ctx){
            this.ctx=ctx;

        }

        @Override
        protected void onPreExecute() {
            URLconsulta="http://"+getString(R.string.ipBase)+"/reto/index.php/getpruebas";
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url=new URL(URLconsulta);
                HttpURLConnection httpURLConnection= (HttpURLConnection) url.openConnection();
                InputStream inputStream=httpURLConnection.getInputStream();
                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder=new StringBuilder();
                while((JSON_STRING=bufferedReader.readLine())!=null){
                    stringBuilder.append(JSON_STRING+"\n");
                }
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                if(!result.equalsIgnoreCase("FALSE")) {
                    jsonObject = new JSONObject(result);
                    jsonArray = jsonObject.getJSONArray("pruebas");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject JSO = jsonArray.getJSONObject(i);
                        items.add(JSO.getString("idprueba"));
                    }
                    adapter.notifyDataSetChanged();
                }

            } catch (JSONException e) {
                items.add("No hay pruebas" + "  ");
                adapter.notifyDataSetChanged();
                e.printStackTrace();
            }
        }
    }
}
