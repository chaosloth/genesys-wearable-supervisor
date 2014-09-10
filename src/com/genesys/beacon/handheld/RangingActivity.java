package com.genesys.beacon.handheld;

import java.util.Collection;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.EditText;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import com.genesys.beacon.R;
import com.genesys.beacon.R.id;
import com.genesys.beacon.R.layout;

public class RangingActivity extends Activity implements BeaconConsumer {
	protected static final String	TAG				= "RangingActivity";
	private BeaconManager			iBeaconManager	= BeaconManager.getInstanceForApplication(this);
	
	// My Region Name
	public static final String REGION_NAME = "myRangingUniqueId";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ranging);
		iBeaconManager.bind(this);
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
				if (iBeacons.size() > 0) {
					EditText editText = (EditText) RangingActivity.this.findViewById(R.id.rangingText);
					Beacon aBeacon = iBeacons.iterator().next();
					logToDisplay("Num Beacons:" + iBeacons.size() + " The first iBeacon I see is about " + aBeacon.getDistance() + " meters away. " + aBeacon.getId1());
				}
			}
		});

		try {
			iBeaconManager.startRangingBeaconsInRegion(new Region(REGION_NAME, null, null, null));
		} catch (RemoteException e) {
		}
	}

	private void logToDisplay(final String line) {
		runOnUiThread(new Runnable() {
			public void run() {
				EditText editText = (EditText) RangingActivity.this.findViewById(R.id.rangingText);
				editText.append(line + "\n");
			}
		});
	}
}
