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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> items;
    ArrayAdapter<String> adapter;
    HashMap <String,Integer> grupos;
    JSONObject jsonObject;
    JSONArray jsonArray;
    String JSON_STRING;
    ListView listView;
    Context context;
    Spinner spinner;
    Iterator it;
    BackgroundTask backgroundTask;
    TextView tvGrupo;
    TextView tvCantidad;
    TextView tvAutores;
    //El spinner presenta problemas con el hilo
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        grupos = new HashMap<>();
        listView = (ListView) findViewById(R.id.listView);
        tvGrupo = (TextView) findViewById(R.id.textView3);
        tvCantidad = (TextView) findViewById(R.id.textView4);
        tvAutores = (TextView) findViewById(R.id.textView5);
        tvAutores.setText("AUTORES:"+"\r\n"+"Víctor Fonseca"+"\r\n"+"Diego Sierra");
        items = new ArrayList<>();
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items);
        listView.setClickable(true);
        listView.setAdapter(adapter);
        poblarSpinner();
        verPrueba();
    }

    public void verPrueba() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                view.setSelected(true); //****new
                String nombre = (String) listView.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, EnviarPrueba.class);
                intent.putExtra("idprueba", nombre);
                startActivity(intent);
            }
        });
    }

    public void poblarSpinner(){
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        items.clear();
                        grupos.clear();
                        backgroundTask =  new BackgroundTask(context);
                        backgroundTask.execute();
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void actualizarTabla(){
        //String ids = "Grupo"+"\r\n"+"\r\n"+"---------------"+"\r\n";
        //String objetivos = "Objetivos cumplidos"+"\r\n"+"-----------------"+"\r\n";
        String ids = "Grupo"+"\r\n"+"\r\n";
        String objetivos = "Objetivos cumplidos"+"\r\n";
        it = grupos.keySet().iterator();
        while(it.hasNext()){
            String key = (String) it.next();
            ids += key+"\r\n";
            objetivos += grupos.get(key)+"\r\n";
        }
        tvGrupo.setText(ids);
        tvCantidad.setText(objetivos);
    }

    class BackgroundTask extends AsyncTask<Void,Void,String> {

        Context ctx;
        String URLconsulta="";

        BackgroundTask(Context ctx){
            this.ctx=ctx;
        }

        @Override
        protected void onPreExecute() {
            URLconsulta="http://tecmmas.com/reto/index.php/prueba/getpruebas";
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
                jsonObject = new JSONObject(result);
                jsonArray = jsonObject.getJSONArray("pruebas");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject JSO = jsonArray.getJSONObject(i);
                    if(JSO.getString("estado").equals("0")){
                        items.add(JSO.getString("idprueba"));
                    }else{
                        it = grupos.keySet().iterator();
                        boolean repetido = false;
                        while(it.hasNext()){
                            String key = (String) it.next();
                            if(JSO.getString("competidor").equals(key)){
                                grupos.put(key,grupos.get(key)+1);
                                repetido = true;
                            }
                        }
                        if(repetido==false){
                            grupos.put(JSO.getString("competidor"),1);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                actualizarTabla();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}