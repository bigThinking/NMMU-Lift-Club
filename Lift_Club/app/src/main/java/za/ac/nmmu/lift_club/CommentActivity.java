package za.ac.nmmu.lift_club;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;

import com.philippheckel.service.AbstractService;
import com.philippheckel.service.ServiceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import za.ac.nmmu.lift_club.services.HttpCommService;
import za.ac.nmmu.lift_club.util.Activity;

public class CommentActivity extends AppCompatActivity {

    public ServiceManager http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_action_arrow_back);
        ab.setDisplayHomeAsUpEnabled(true);

        CircleImageView userPic = (CircleImageView)findViewById(R.id.user_pic);
        CircleImageView carPic = (CircleImageView)findViewById(R.id.car_pic1);
        Button btnSave = (Button)findViewById(R.id.btnSave3);
        final RatingBar ratingBar = (RatingBar)findViewById(R.id.rb_rideRating);
        final EditText edtComments =  (EditText)findViewById(R.id.edtComment);

        final Intent intent = getIntent();
        final Activity a = (Activity)intent.getSerializableExtra("za.ac.nmmu.lift_club.activity");
        final String token = intent.getStringExtra("za.ac.nmmu.lift_club.token");

        if(imageExist(a.userInfo.pic_id))
        {
            Bitmap bitmap = readImage(a.userInfo.pic_id);
            userPic.setImageBitmap(bitmap);
        }

        if(a.userInfo.isDriver && imageExist(a.carInfo.pic_id))
        {
            Bitmap bitmap = readImage(a.userInfo.pic_id);
            carPic.setImageBitmap(bitmap);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String comments = edtComments.getText().toString();
                int rating = (int)ratingBar.getRating();

                Message msg = new Message();
                msg.what = 17;
                msg.getData().putString("token", token);
                msg.getData().putString("rideId", a.match.getRide().getRideId());
                msg.getData().putString("comment", comments);
                msg.getData().putInt("rating", rating);
                msg.getData().putString("driverId", a.match.getRide().getDriverId());
                msg.getData().putString("driverId", a.match.getRide().getDriverId());
                msg.getData().putString("passengerId", a.match.getUserId());

                Intent intent1 = new Intent();

                try {
                    httpSend(msg);
                } catch (RemoteException e) {
                    setResult(36);
                    finish();
                }
                intent.putExtra("za.ac.nmmu.lift_club.activity", a);
                setResult(RESULT_OK, intent1);
                finish();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_comment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public Bitmap readImage(String id) {
        try {
            File file = new File(getApplicationContext().getCacheDir(), id + ".tmp");
            FileInputStream fis = new FileInputStream(file);

            byte[] data = new byte[(int) file.length()];
            int res = fis.read(data);
            fis.close();

            if (res > 0) {
               return BitmapFactory.decodeByteArray(data, 0, data.length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean imageExist(String id) {
        File file = new File(getApplicationContext().getCacheDir(), id + ".tmp");
        return file.exists();
    }

    private void initHttp() {
        if (http == null) {
            http = new ServiceManager(this.getApplicationContext(), HttpCommService.class, new HttpCommHandler());
            http.start();
        }
    }

    private void httpSend(Message msg) throws RemoteException {
        initHttp();
        if (isConnected()) {
            http.send(msg);
        } else {
            //doqueue
            Message msg1 = new Message();
            msg1.what = AbstractService.ADD_PROCESSING_QUEUE;
            msg1.getData().putParcelable("msg", msg);
            http.send(msg1);
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    class HttpCommHandler extends Handler
    {

    }
}
