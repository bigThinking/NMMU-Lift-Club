package za.ac.nmmu.pointcollector;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends ActionBarActivity implements LocationListener {

    Button btnOk = null;
    EditText edtName = null;
    LocationManager service = null;
    MainActivity parent = this;
    boolean buttonclicked = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOk = (Button)findViewById(R.id.btnOk);
        edtName = (EditText)findViewById(R.id.edtName);

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.requestSingleUpdate(LocationManager.GPS_PROVIDER, parent ,null);//request location
                buttonclicked = true;
            }
        });

        service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {

        if(buttonclicked == true) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            Toast.makeText(getApplicationContext(), String.valueOf(lat) +","+String.valueOf(lon)+", Accuracy:" + String.valueOf(location.getAccuracy()), Toast.LENGTH_LONG).show();

            HTTP con = new HTTP(edtName.getText().toString(), lat, lon);
            con.start();

            buttonclicked = false;
            edtName.setText("");
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public class HTTP extends Thread{
        String name = "";
        double lat, lon;

        public HTTP(String name, double lat, double lon)
        {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }

        public void run()
        {
            try {
                URL url = new URL("http://10.122.49.23:8090/Point_Collector/webresources/point?name=" + name + "&lat=" + String.valueOf(lat) + "&long=" + String.valueOf(lon));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                final String response = urlConnection.getResponseMessage() + "," +reader.readLine();

                if(response.compareTo("202") == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "successful", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                else runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),response,Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
//               runOnUiThread(new Runnable() {
//                   @Override
//                   public void run() {
//                       Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
//                   }
//               });
            }
        }

    }
}
