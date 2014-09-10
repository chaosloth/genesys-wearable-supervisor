package com.genesys.beacon.handheld;

import java.util.ArrayList;
import java.util.Collection;

import android.app.Activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;

import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import com.genesys.beacon.R;
import com.genesys.beacon.R.drawable;
import com.genesys.beacon.R.layout;
import com.genesys.beacon.R.string;
import com.genesys.beacon.glass.GlassService;

public class RangingDemoActivity extends Activity implements BeaconConsumer {
	public static final String		TAG				= "RangingDemoActivity";

	// My Beacon
	public static final Identifier	Beacon1_UUID	= Identifier.parse("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");

	// My Region Name
	public static final String		REGION_NAME		= "myRangingUniqueId";

	private static final String		EXTRA_EVENT_ID	= "EXTRA_EVENT_ID";

	private ArrayList<Double>		range			= new ArrayList<Double>();
	private BeaconManager			iBeaconManager	= BeaconManager.getInstanceForApplication(this);
	int								previousColor	= 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rangingdemo);
		iBeaconManager.bind(this);
		stopService(new Intent(this, GlassService.class));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		iBeaconManager.unbind(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (iBeaconManager.isBound(this))
			iBeaconManager.setBackgroundMode(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (iBeaconManager.isBound(this))
			iBeaconManager.setBackgroundMode(false);
	}

	@Override
	public void onBeaconServiceConnect() {
		iBeaconManager.setRangeNotifier(new RangeNotifier() {
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> iBeacons, Region region) {
				if (iBeacons != null) {
					if (iBeacons.size() > 0) {
						// iterate through each beacon found
						range.clear();
						for (Beacon i : iBeacons) {
							Log.d(TAG, "UUID:" + i.getId1() + " dist " + i.getDistance());
							if (i.getId1().equals(Beacon1_UUID)) {
								range.add(i.getDistance());
							}
						}

						if (range.size() > 0) {
							onDetectedBeaconInRange(range);
						}
					}
				}
			}
		});

		try {
			iBeaconManager.startRangingBeaconsInRegion(new Region(REGION_NAME, null, null, null));
		} catch (RemoteException e) {
		}
	}
	
	private void onDetectedBeaconInRange(ArrayList<Double> range) {
		setDisplay(range);
	}
	
	private void setDisplay(ArrayList<Double> range) {
		if (range != null) {
			double distance;
			distance = range.get(0);
			Log.d(TAG, "distance " + distance);
			if (distance <= 1.0f) {
				if (previousColor != Color.RED) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							View v = RangingDemoActivity.this.findViewById(android.R.id.content);
							v.setBackgroundColor(Color.RED);
							v.invalidate();
							sendNotification(Color.RED);
						}
					});

				}
				previousColor = Color.RED;

			} else if (distance > 1.0f && distance <= 3.0f) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						View v = RangingDemoActivity.this.findViewById(android.R.id.content);
						v.setBackgroundColor(Color.MAGENTA);
						v.invalidate();
						sendNotification(Color.MAGENTA);
					}

				});
			} else {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						View v = RangingDemoActivity.this.findViewById(android.R.id.content);
						v.setBackgroundColor(Color.BLUE);
						v.invalidate();
						sendNotification(Color.BLUE);
					}
				});
			}
		}
	}

	private void sendNotification(int color) {
		final int notificationId = 1;
		int resourceid = R.drawable.ic_marker;
		if (color == Color.MAGENTA) {
			resourceid = R.drawable.ic_marker2;
		} else if (color == Color.BLUE) {
			resourceid = R.drawable.ic_marker3;
		}
		final Bitmap bitmapIcon = BitmapFactory.decodeResource(this.getResources(), resourceid);

		Intent viewIntent = new Intent(this, RangingDemoActivity.class);
		viewIntent.putExtra(EXTRA_EVENT_ID, notificationId);
		PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, viewIntent, 0);

		String eventTitle = getString(R.string.app_name);
		String eventLocation = "- beacon in the area -";
		if (color == Color.RED) {
			eventLocation = "Beacon Found!";
		} else if (color == Color.MAGENTA) {
			eventLocation = "getting closer...";
		}
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this).setContentTitle(eventTitle).setContentText(eventLocation).setSmallIcon(R.drawable.ic_launcher)
				.setLargeIcon(bitmapIcon).setContentIntent(viewPendingIntent);

		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
		notificationManager.cancelAll();
		notificationManager.notify(notificationId, notificationBuilder.build());

	}
}