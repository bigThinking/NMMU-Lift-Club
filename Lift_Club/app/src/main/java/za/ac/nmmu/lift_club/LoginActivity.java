package za.ac.nmmu.lift_club;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.philippheckel.service.ServiceManager;

import org.json.JSONException;

import za.ac.nmmu.lift_club.services.HttpCommService;
import za.ac.nmmu.lift_club.util.User;


public class LoginActivity extends AppCompatActivity {

    private ServiceManager http;
    private ProgressBar pbarLogin = null;
    private Button login;
    private int returnMarker = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        pbarLogin = (ProgressBar) findViewById(R.id.pbarLogin);
        login = (Button) findViewById(R.id.btnLogin);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin();
            }
        });
        pbarLogin.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.nmmu_blue), PorterDuff.Mode.MULTIPLY);

        http = new ServiceManager(getApplicationContext(), HttpCommService.class, new HttpCommHandler());
        http.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_log_in, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
      /*  if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    public void doLogin() {
        initHttp();
        EditText edtName = (EditText) findViewById(R.id.edtUsername);
        EditText edtPassword = (EditText) findViewById(R.id.edtPassword);

        String userName = edtName.getText().toString();
        String password = edtPassword.getText().toString();

        if (userName.trim().compareTo("") == 0) {
            edtName.setError("Please enter your username");
            return;
        }

        if (password.compareTo("") == 0) {
            edtPassword.setError("Please enter your password");
            return;
        }

        Log.i("1", "1");
        Message msg = new Message();
        msg.what = 0;
        msg.getData().putString("userName", userName);
        msg.getData().putString("password", password);

        Log.i("2", "2");

        returnMarker = 0;
        try {
            http.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.i("3", "3");
        pbarLogin.setVisibility(View.VISIBLE);
        login.setEnabled(false);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private void initHttp() {
        if (http == null) {
            http = new ServiceManager(this.getApplicationContext(), HttpCommService.class, new HttpCommHandler());
            http.start();
        }
    }

    private void showDialog(String title, String response) {
        String[] message = response.split(":");
        ErrorDialogFragment edf = new ErrorDialogFragment();
        edf.errorMessage = message[1];
        edf.title = title;
        edf.show(getSupportFragmentManager(), "");
    }

    class HttpCommHandler extends Handler {
        private String token;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: {//login
                    String response = msg.getData().getString("response", "");
                    if (!response.startsWith("error")) {
                        SharedPreferences preferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("token", response);
                        editor.apply();

                        token = response;

                        Message msg1 = new Message();
                        msg1.what = 2;// get user info
                        msg1.getData().putString("token", response);

                        Message msg2 = new Message();
                        msg2.what = 3;//get campuses
                        msg2.getData().putString("token", response);

                        try {
                            http.send(msg1);
                            returnMarker++;
                            http.send(msg2);
                            returnMarker++;
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (pbarLogin != null)
                            pbarLogin.setVisibility(View.INVISIBLE);

                        showDialog("Problem logging in", response);
                        login.setEnabled(true);
                    }
                    break;
                }
                case 2: {//user info
                    String response = msg.getData().getString("response", "");
                    if (!response.startsWith("error")) {
                        SharedPreferences preferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("user", response);
                        editor.apply();

                        try {
                            User currentUser = new User(response);

                            if (currentUser.nrPointsSaved != 0) {
                                Message msg1 = new Message();
                                msg1.what = 5;// get points
                                msg1.getData().putString("token", token);
                                http.send(msg1);
                                returnMarker++;
                            }

                            if (currentUser.isDriver && currentUser.nrRoutesRecorded > 0) {
                                Message msg1 = new Message();
                                msg1.what = 6;//get routes
                                msg1.getData().putString("token", token);
                                http.send(msg1);
                                returnMarker++;
                            }

                            if(currentUser.isDriver)
                            {
                                Message msg1 = new Message();
                                msg1.what = 15;
                                msg1.getData().putString("token", token);
                                msg1.getData().putString("vrn", currentUser.currentCarVRN);
                                http.send(msg1);
                                returnMarker++;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                        if (--returnMarker == 0) {
                            editor.putBoolean("justLoggedIn", true);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            http.unbind();
                            finish();
                            startActivity(intent);
                        }
                    } else {
                        if (pbarLogin != null)
                            pbarLogin.setVisibility(View.INVISIBLE);

                        showDialog("Problem obtaining user details", response);
                        login.setEnabled(true);
                        doLogout();
                    }
                    break;
                }
                case 3: {// campuses
                    String response = msg.getData().getString("response", "");
                    if (!response.startsWith("error")) {
                        SharedPreferences preferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("campus", response);
                        editor.apply();

                        if (--returnMarker == 0) {
                            editor.putBoolean("justLoggedIn", true);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            http.unbind();
                            finish();
                            startActivity(intent);
                        }
                    } else {
                        if (pbarLogin != null)
                            pbarLogin.setVisibility(View.INVISIBLE);

                        showDialog("Problem obtaining campus info", response);
                        login.setEnabled(true);
                        doLogout();
                    }
                    break;
                }
                case 5: {//positions
                    String response = msg.getData().getString("response", "");
                    if (!response.startsWith("error")) {
                        SharedPreferences preferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("points", response);
                        editor.apply();

                        if (--returnMarker == 0) {
                            editor.putBoolean("justLoggedIn", true);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            http.unbind();
                            finish();
                            startActivity(intent);
                        }
                    } else {
                        if (pbarLogin != null)
                            pbarLogin.setVisibility(View.INVISIBLE);

                        showDialog("Problem obtaining saved positions", response);
                        login.setEnabled(true);
                        doLogout();
                    }
                    break;
                }
                case 6: {//routes
                    String response = msg.getData().getString("response", "");
                    if (!response.startsWith("error")) {
                        SharedPreferences preferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("routes", response);
                        editor.apply();

                        if (--returnMarker == 0) {
                            editor.putBoolean("justLoggedIn", true);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            http.unbind();
                            finish();
                            startActivity(intent);
                        }
                    } else {
                        if (pbarLogin != null)
                            pbarLogin.setVisibility(View.INVISIBLE);

                        showDialog("Problem obtaining saved routes", response);
                        login.setEnabled(true);
                        doLogout();
                    }
                    break;
                }
                case  15:{//current car
                    String response = msg.getData().getString("response", "");
                    if (!response.startsWith("error")) {
                        SharedPreferences preferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("currentCar", response);
                        editor.apply();

                        if (--returnMarker == 0) {
                            editor.putBoolean("justLoggedIn", true);
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            http.unbind();
                            finish();
                            startActivity(intent);
                        }
                    } else {
                        if (pbarLogin != null)
                            pbarLogin.setVisibility(View.INVISIBLE);

                        showDialog("Problem obtaining current car info", response);
                        login.setEnabled(true);
                        doLogout();
                    }
                    break;
                }
            }
        }

        private void doLogout() {
            SharedPreferences preferences = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
            String token = preferences.getString("token", null);
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("token");
            editor.apply();

            Message msg = new Message();
            msg.what = 1;
            msg.getData().putString("token", token);
            try {
                http.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
