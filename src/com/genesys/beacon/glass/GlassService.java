package com.genesys.beacon.glass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Patterns;
import android.widget.RemoteViews;

import com.genesys.beacon.BeaconScanConsumer;
import com.genesys.beacon.ConnollyBeaconParser;
import com.genesys.beacon.R;
import com.google.android.glass.timeline.LiveCard;


/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class GlassService extends Service implements BeaconConsumer {
	protected static final String TAG = "GlassService";
    private static final String LIVE_CARD_TAG = "beaconscan";
    // My Region
    public static final String REGION_NAME = "myRangingUniqueId";
    
    // My Beacon
    public static final Identifier Beacon1_UUID = Identifier.parse("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
    
    // Battery Saver
    private BackgroundPowerSaver backgroundPowerSaver;
    
	private ArrayList<Double> range = new ArrayList<Double>();
	private RemoteViews mLiveCardView;
    private TextToSpeech mSpeech;
    private LiveCard mLiveCard;
    int previousColor = 0;

    //    private final UpdateLiveCardRunnable mUpdateLiveCardRunnable = new UpdateLiveCardRunnable();
//    private static final long DELAY_MILLIS = 1000;

    private final BeaconScanBinder mBinder = new BeaconScanBinder();
//	protected BeaconScanConsumer beaconConsumer;
    private BeaconManager iBeaconManager = BeaconManager.getInstanceForApplication(this);

    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class BeaconScanBinder extends Binder {
        /**
         * Read the current heading aloud using the text-to-speech engine.
         */
        public void readAloud() {

            Resources res = getResources();
            String[] spokenDirections = res.getStringArray(R.array.spoken_beacon_names);
            String beaconName = "one"; //TODO:.....

            String headingText = "Welcome to Genesys Pulse for Glass. As you get close to an agent we will display their metrics. To cool your Glass and stop scanning, select stop in the menu.";
            mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        // Even though the text-to-speech engine is only used in response to a menu action, we
        // initialize it when the application starts so that we avoid delays that could occur
        // if we waited until it was needed to start it up.
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            	Log.d(TAG, "Initialised TTS Engine");
            }
        });
        
        iBeaconManager.bind(this);
        
        backgroundPowerSaver = new BackgroundPowerSaver(this);
        
        // set the duration of the scan to be 1.1 seconds
        iBeaconManager.setBackgroundScanPeriod(1100l);
        
        // set the time between each scan to be 30 
        iBeaconManager.setBackgroundBetweenScanPeriod(30);
        
        // TODO: CJC - Change the beacon parser to Radius beacon types
        iBeaconManager.getBeaconParsers().clear();
        iBeaconManager.getBeaconParsers().add(new ConnollyBeaconParser());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    
	@Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }

        mSpeech.shutdown();
        mSpeech = null;
        
        //onPause():
        unregisterReceiver(intentReceiver);
    	//if (iBeaconManager.isBound(beaconConsumer)) {
    	//	iBeaconManager.setBackgroundMode(beaconConsumer, true); 		
    	//}    	
        iBeaconManager.unbind(this);      
        
        super.onDestroy();
    }
    
	private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG,intent.getExtras().getString(BeaconScanConsumer.EXTRA_DATA));			
            mLiveCard.navigate();
		}
	};
	
	
	@Override
	public void onBeaconServiceConnect() {
		
        iBeaconManager.setRangeNotifier(new RangeNotifier() {
	        @Override 
	        public void didRangeBeaconsInRegion(Collection<Beacon> iBeacons, Region region) {
	        	if(iBeacons != null) {
	        		if (iBeacons.size() > 0) {
	        			// iterate through each beacon found
	        			range.clear();
	        			for (Beacon i : iBeacons) {
	        				Log.d(TAG,"**CJC: UUID:" + i.getId1() + " dist " + i.getDistance());
	        				if(i.getId1().equals(Beacon1_UUID)) {
	        					Log.d(TAG,"**CJC: Beacon I: " + i.getId1() + " == " + Beacon1_UUID);
	        					range.add(i.getDistance());
	        				} else {
	        					Log.d(TAG,"**CJC: Beacon I: " + i.getId1() + " != " + Beacon1_UUID);
	        				}
	        			}	
	        			if(range.size() > 0) {
	        				Log.d(TAG,"**CJC: Calling setDisplay() for region " + range);
	        				onDetectedBeaconInRange(range);
	        			}
	        		}
	        	}
	        }
        });

        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
	        @Override
	        public void didEnterRegion(Region region) {
	          String data = "I just saw an iBeacon for the first time!";
	          Log.e(TAG,data);
	          mLiveCard.navigate();
	        }
	
	        @Override
	        public void didExitRegion(Region region) {
	        	Log.e(TAG,"I no longer see an iBeacon");
    	        //TODO: send intent with data
	        }
	
	        @Override
	        public void didDetermineStateForRegion(int state, Region region) {
	        	Log.e(TAG,"I have just switched from seeing/not seeing iBeacons: "+state);     
    	        //TODO: send intent with data
	        }
        });
        
        // ************
        // Here we start ranging - NOTE: High battery usage
        // ************
        try {
            iBeaconManager.startRangingBeaconsInRegion(new Region(REGION_NAME, null, null, null));
        } catch (RemoteException e) {   }		
	}

    
	private void onDetectedBeaconInRange(ArrayList<Double> range) {
		setDisplay(range);
	}
	
	private void setDisplay(ArrayList<Double> range) {
    	if(range != null) {
    		double distance;
    		distance = range.get(0);
    		Log.d(TAG,"distance " + distance );
    		if(distance <= 1.0f) {    			
    			setColor(Color.RED);
    			if(previousColor!=Color.RED) {
    				mSpeech.speak("found the beacon!", TextToSpeech.QUEUE_FLUSH, null);
    	            mLiveCardView.setTextViewText(R.id.beacpn_text, "found the beacon!");
    	            mLiveCard.setViews(mLiveCardView);
    			}
    			previousColor=Color.RED;
    		} else if (distance > 1.0f && distance <= 3.0f) {
				setColor(Color.MAGENTA);
    			if(previousColor!=Color.MAGENTA) {
    				mSpeech.speak("getting closer", TextToSpeech.QUEUE_FLUSH, null);
    	            mLiveCardView.setTextViewText(R.id.beacpn_text, "getting closer");
    	            mLiveCard.setViews(mLiveCardView);
    			}
    			previousColor=Color.MAGENTA;
    		} else {
    			setColor(Color.BLUE);
    			if(previousColor!=Color.BLUE) {
    				mSpeech.speak("beacon in the area", TextToSpeech.QUEUE_FLUSH, null);
    	            mLiveCardView.setTextViewText(R.id.beacpn_text, "beacon in the area");
    	            mLiveCard.setViews(mLiveCardView);
    			}
    			previousColor=Color.BLUE;
    		}
    	}
    }
    private void setColor(final int color) {
		Runnable task = new Runnable() {
			public void run() {
				Log.e(TAG,"set color =" + color);
				//View v = mRenderer.getViewById(android.R.id.content);
				//v.setBackgroundColor(Color.RED);
				//v.invalidate();
			}
		};							
	    new Handler(Looper.getMainLooper()).post(task);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mLiveCardView = new RemoteViews(getPackageName(), R.layout.beaconscan_glass);

            mLiveCardView.setTextViewText(R.id.beacpn_text, "scanning beacons...");
            mLiveCard.setViews(mLiveCardView);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            // Publish the live card
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);

            Log.d(TAG, "mLiveCard.publish " + mLiveCard.isPublished());
            
        } else if (!mLiveCard.isPublished()) {
            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
        } else {
            mLiveCard.navigate();
        }

        return START_STICKY;
    }

}
