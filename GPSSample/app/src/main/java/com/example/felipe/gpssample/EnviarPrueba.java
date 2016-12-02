package com.example.felipe.gpssample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class EnviarPrueba extends AppCompatActivity implements LocationListener {

    public double latitude;
    public double longitude;
    String location;
    String prueba;
    String competidor;
    private String rutafoto;
    private static int TAKE_PICTURE = 1;
    int aleatorio = 0;
    Bitmap photobmp;
    ImageView iv;
    String encodedImage;
    //RequestQueue requestQueue;
    TextView textprueba;
    TextView textmensaje;
    String JSON_STRING;
    JSONObject jsonObject;
    JSONArray jsonArray;
    Context context;
    BackgroundTask backgroundTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prueba);
        context = this;
        competidor="1";
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            prueba = (String)extras.get("idprueba");
        }
        backgroundTask =  new BackgroundTask(context);
        backgroundTask.execute();
        //Para crear un nombre diferente para la foto
        aleatorio = new Double(Math.random() * 1000).intValue();
        rutafoto = Environment.getExternalStorageDirectory()+"/imagen"+aleatorio+".jpg";
        iv = (ImageView) findViewById(R.id.imageView);
        textmensaje = (TextView) findViewById(R.id.textView);
        textprueba = (TextView) findViewById(R.id.textView2);
        LocationManager mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener mlocListener = this;
        try {
            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
        } catch (SecurityException e) {
            Log.e("PERMISSION_EXCEPTION", "PERMISSION_NOT_GRANTED");
        }
    /**    Location loc = new Location(LocationManager.GPS_PROVIDER);
        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        location = "(" +  String.valueOf(latitude)  + "," +  String.valueOf(longitude) + ")";**/
    }

    public void capturar(View v) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri output = Uri.fromFile(new File(rutafoto));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
        startActivityForResult(intent, TAKE_PICTURE); // 1 para la camara, 2 para la galeria
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            //photobmp = BitmapFactory.decodeFile(rutafoto);
            try {
                photobmp = redimensionarImagenMaximo(rutafoto);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            photobmp.compress(Bitmap.CompressFormat.JPEG, 40, baos);
            byte[] imageBytes = baos.toByteArray();
            encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            //Se ejecuta en segundo plano para no colgar la aplicacion
            //Log.d("console2", encodedImage.toString());
            guardarDato();
            iv.setImageBitmap(photobmp);
        }
    }

    private void guardarDato(){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, String.valueOf("http://tecmmas.com/reto/index.php/prueba/guardarprueba"),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(EnviarPrueba.this, response, Toast.LENGTH_LONG).show();
                        textmensaje.setText("R/ "+response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(EnviarPrueba.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                textmensaje.setText("E/ "+error.getMessage());
            }
        })    {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> map = new HashMap<>();
                map.put("idprueba",prueba);
                map.put("competidor",competidor);
                map.put("gps",location);
                map.put("foto",encodedImage);
                return map;
            }

        };
        requestQueue.add(request);
    }

    class BackgroundTask extends AsyncTask<Void,Void,String> {

        Context ctx;
        String URLconsulta="";

        BackgroundTask(Context ctx){
            this.ctx=ctx;
        }
        @Override
        protected void onPreExecute() {
            URLconsulta="http://tecmmas.com/reto/index.php/prueba/getprueba?idprueba="+String.valueOf(prueba);
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
            textprueba.setText(result);
        }

    }

    @Override
    public void onLocationChanged(Location loc) {
        if (loc != null) {
            latitude = loc.getLatitude();
            longitude = loc.getLongitude();
            location = "(" +  String.valueOf(latitude)  + "," +  String.valueOf(longitude) + ")";
        }
        //Toast.makeText(getApplicationContext(), location, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText( getApplicationContext(),"Gps Disabled",Toast.LENGTH_SHORT ).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText( getApplicationContext(),"Gps Enabled",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void regresar(View view) {
        Intent intent = new Intent(EnviarPrueba.this, MainActivity.class );
        startActivity(intent);
    }
    
    public Bitmap redimensionarImagenMaximo(String ruta) throws IOException {
     BitmapFactory.Options options=new BitmapFactory.Options();
     InputStream is=new FileInputStream(ruta);
     BitmapFactory.decodeStream(is, null, options);
     is.close();
     is=new FileInputStream(ruta);
     // here w and h are the desired width and height
     options.inSampleSize=Math.max(options.outWidth/460, options.outHeight/288); //Max 460 x 288 is my desired...
     // bmp is the resized bitmap
     Bitmap bmp=BitmapFactory.decodeStream(is, null, options);
     is.close();
     Log.d("holamundo", "Scaled bitmap bytes, " + bmp.getRowBytes() + ", width:" + bmp.getWidth() + ", height:" + bmp.getHeight());
     return bmp;
    }
}
