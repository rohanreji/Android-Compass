package com.sensomatecompassredis;

import java.util.concurrent.CountDownLatch;

import org.json.JSONObject;

import redis.clients.jedis.Jedis;

import com.sensomatecompassredis.MainActivity.JedisThread;
import com.sensomatecompassredis.MainActivity.JedisTrial;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

public class MyService extends Service implements SensorEventListener{

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
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
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
	 	@Override
	    public int onStartCommand(Intent intent, int flags, int startId) {
	        // Let it continue running until it is stopped.
	        Toast.makeText(this, "Service started", Toast.LENGTH_LONG).show();
	        Log.e("sfsdf","asfc");
	        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		     mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		     mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		     mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	         mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
	        return START_STICKY;
	    }
	 	@Override
	 	public void onDestroy() {//here u should unregister sensor
	 	    Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
	 	   mSensorManager.unregisterListener(this, mAccelerometer);
	        mSensorManager.unregisterListener(this, mMagnetometer);
	 	}
	    public class JedisTrial {
	        // private ArrayList<String> messageContainer = new ArrayList<String>();
	        private CountDownLatch messageReceivedLatch = new CountDownLatch(1);
	        private CountDownLatch publishLatch = new CountDownLatch(1);
	        int l = (PreferenceManager
	                .getDefaultSharedPreferences(getApplicationContext())
	                .getString("MYIP", IP).toString().length());
	        // no http://

	        String JEDIS_SERVER = PreferenceManager
	                .getDefaultSharedPreferences(getApplicationContext())
	                .getString("MYIP", IP).toString();
	    
	       
	        String JEDIS_CHANNEL = PreferenceManager
	                .getDefaultSharedPreferences(getApplicationContext())
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
	

}
