package com.sensomatecompassredis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import redis.clients.jedis.Jedis;

import java.util.concurrent.CountDownLatch;


public class MainActivity extends ActionBarActivity implements SensorEventListener {
	String IP="";
	String CHANNEL="";
    private ImageView mPointer;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mPointer = (ImageView) findViewById(R.id.pointer);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        	 ddemo();
             return true;
        }
        if (id == R.id.action_channel) {
       	 ddemo1();
            return true;
       }


        return super.onOptionsItemSelected(item);
    }
    public void ddemo() {
       
       
                final EditText e = new EditText(MainActivity.this);

                AlertDialog.Builder alert = new AlertDialog.Builder(
                        MainActivity.this);
                alert.setTitle("Set Server");
                alert.setView(e);
                e.setText(PreferenceManager
                        .getDefaultSharedPreferences(MainActivity.this)
                        .getString("MYIP",
                                IP));
                alert.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                PreferenceManager
                                        .getDefaultSharedPreferences(
                                                MainActivity.this)
                                        .edit()
                                        .putString("MYIP",
                                                e.getText().toString())
                                        .commit();
                                dialog.cancel();
                                
                            }
                        });
                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // Canceled.
                            }
                        });
                alert.show();

            
       
    }
    public void ddemo1() {
        
        
        final EditText e = new EditText(MainActivity.this);

        AlertDialog.Builder alert = new AlertDialog.Builder(
                MainActivity.this);
        alert.setTitle("Set Channel");
        alert.setView(e);
        e.setText(PreferenceManager
                .getDefaultSharedPreferences(MainActivity.this)
                .getString("MYCHANNEL",
                        CHANNEL));
        alert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        PreferenceManager
                                .getDefaultSharedPreferences(
                                        MainActivity.this)
                                .edit()
                                .putString("MYCHANNEL",
                                        e.getText().toString())
                                .commit();
                        dialog.cancel();
                       
                    }
                });
        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        // Canceled.
                    }
                });
        alert.show();

    

}


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            RotateAnimation ra = new RotateAnimation(
                    mCurrentDegree,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);
           
            ra.setDuration(250);

            ra.setFillAfter(true);

            mPointer.startAnimation(ra);
            mCurrentDegree = -azimuthInDegress;
            if(checkInternetConnection()) {
                new JedisThread().execute();
            }
          
            
        }
    }
    private boolean checkInternetConnection() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
        if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isAvailable() &&    conMgr.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            System.out.println("Internet Connection Not Present");
            return false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    class JedisThread extends AsyncTask<Void, Void, Void> {

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog



        }

        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            JedisTrial j = new JedisTrial();

            j.setupPublisher();

            return null;
        }

    }

    public class JedisTrial {
        // private ArrayList<String> messageContainer = new ArrayList<String>();
        private CountDownLatch messageReceivedLatch = new CountDownLatch(1);
        private CountDownLatch publishLatch = new CountDownLatch(1);
        int l = (PreferenceManager
                .getDefaultSharedPreferences(MainActivity.this)
                .getString("MYIP", IP).toString().length());
        // no http://

        String JEDIS_SERVER = PreferenceManager
                .getDefaultSharedPreferences(MainActivity.this)
                .getString("MYIP", IP).toString();
    
       
        String JEDIS_CHANNEL = PreferenceManager
                .getDefaultSharedPreferences(MainActivity.this)
                .getString("MYCHANNEL", CHANNEL).toString();

        private void setupPublisher() {
            try {
            	 
                if(JEDIS_SERVER.contains(":")){
                    String s[]=new String[2];
                    s=JEDIS_SERVER.split(":");
                    JEDIS_SERVER=s[0];
                    //  Toast.makeText(getApplicationContext(),JEDIS_SERVER,Toast.LENGTH_SHORT).show();
                   
                }

                System.out.println("Connecting");
                System.out.println(JEDIS_SERVER);
                Jedis jedis = new Jedis(JEDIS_SERVER,6379);

                //jedis.auth("sensomate_123#");

                TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                JSONObject location = new JSONObject();
                location.put("degree_from_north ", mCurrentDegree);
               
                location.put("capturedAt", System.currentTimeMillis());
                location.put("deviceid", mngr.getDeviceId());
               

                String json = "";
                json = location.toString();
                System.out.println("Waiting to publish");
                // publishLatch.await();
                System.out.println("Ready to publish, waiting one sec");
                // Thread.sleep(1000);
                System.out.println("publishing");

                // jsonstring here...
                jedis.publish(JEDIS_CHANNEL, mCurrentDegree+"");
                //jedis.auth("sensomate_123#");
                System.out.println("published, closing publishing connection");
                jedis.quit();
                System.out.println("publishing connection closed");
              
            } catch (Exception e) {
                System.out.println(">>> OH NOES Pub, " + e.getMessage());
               
                e.printStackTrace();
            }
        }
    }
    public void start(View v){
    	startService(new Intent(this, MyService.class));
    }
}
