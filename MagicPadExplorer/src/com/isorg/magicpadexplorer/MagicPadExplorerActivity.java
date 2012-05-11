package com.isorg.magicpadexplorer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.application.Applet;
import com.isorg.magicpadexplorer.application.AppletAdapter;
import com.isorg.magicpadexplorer.application.ConnexionTest;
import com.isorg.magicpadexplorer.application.SmartSwitchApplication;
import com.isorg.magicpadexplorer.application.PhotosBrowserApplication;
import com.isorg.magicpadexplorer.application.TwistApplication;
import com.isorg.magicpadexplorer.application.VumeterApplication;

public class MagicPadExplorerActivity extends Activity {
    /** Called when the activity is first created. */
	
	// For the home page view
	private GridView mGrid = null;
	private AppletAdapter mAdapter= null;
	
	// For the BT
	private final static int REQUEST_CODE_ENABLE_BLUETOOTH = 0;
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private String magicPadAddress = null;
	private boolean BTParameters = false;
		
	//For keep the screen bright
    WakeLock mWakeLock;


	// For the debugging
	private String TAG = "MagicExplorerActivity";
	private Boolean D = true;	
    
	
	
	//  
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
                
        // Fill the grid
        mAdapter = new AppletAdapter(this);

        mGrid = (GridView) findViewById(R.id.home_page);
        mGrid.setAdapter(mAdapter);

        mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.logo_for_connexion_test), getResources().getString(R.string.connexion_name)));
        mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.on_button), getResources().getString(R.string.switch_name)));
        mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.logo_for_vumeter), getResources().getString(R.string.vumeter_name)));
        mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.logo_for_twist), getResources().getString(R.string.twist_name)));
        mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.logo_for_photos_browser), getResources().getString(R.string.photos_browser_name)));
        mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.logo_for_lemon), getResources().getString(R.string.lemon_name)));
        mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.logo_isorg_on_the_web), getResources().getString(R.string.web_site_name)));
        mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.logo_isorg_on_the_web), getResources().getString(R.string.videos_name)));


        try
		{
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
		}
		catch (Exception e){
		}
        
        // The listener for items
        mGrid.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {

			    if (mAdapter.getItem(position).getName() == getResources().getString(R.string.connexion_name)) {
					Intent intent = new Intent(MagicPadExplorerActivity.this, ConnexionTest.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }
			    else if (mAdapter.getItem(position).getName() == getResources().getString(R.string.switch_name)) {
					Intent intent = new Intent(MagicPadExplorerActivity.this, SmartSwitchApplication.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }else if (mAdapter.getItem(position).getName() == getResources().getString(R.string.vumeter_name)) {
					Intent intent = new Intent(MagicPadExplorerActivity.this, VumeterApplication.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }else if (mAdapter.getItem(position).getName() == getResources().getString(R.string.twist_name)) {
					Intent intent = new Intent(MagicPadExplorerActivity.this, TwistApplication.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }else if (mAdapter.getItem(position).getName() == getResources().getString(R.string.photos_browser_name)) {
					Intent intent = new Intent(MagicPadExplorerActivity.this, PhotosBrowserApplication.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }else if (mAdapter.getItem(position).getName() == getResources().getString(R.string.web_site_name)) {
			    	Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.isorg.fr/fr/"));
		    		startActivity(viewIntent);	
			    }else if (mAdapter.getItem(position).getName() == getResources().getString(R.string.videos_name)) {
			    	Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.youtube.com/user/ISORGorganicsensor?feature=watch"));
		    		startActivity(viewIntent);
			    }else if (mAdapter.getItem(position).getName() == getResources().getString(R.string.lemon_name)) {
					Intent intent = new Intent(MagicPadExplorerActivity.this, com.android.lemon.LemonViewerActivity.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }
        	}
        });
        
		
    }
    
    
    @Override
	protected void onResume() {
        mWakeLock.acquire();

        if (!BTParameters) {
	        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
	        if (bluetoothAdapter == null)
	        	Toast.makeText(this, R.string.no_bluetooth, Toast.LENGTH_LONG);
	        else {
	        	if (!bluetoothAdapter.isEnabled()) { 
	        		   Intent enableBlueTooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); 
	        		   startActivityForResult(enableBlueTooth, REQUEST_CODE_ENABLE_BLUETOOTH);
	        	}
	        	else connectToDevice();
	        	BTParameters = true;
	        }
        }
    	super.onResume();
	}
    
    protected void onPause() {
    	mWakeLock.release();
    	super.onPause();
    }
    
   

    
    
    /********				Save/restore the State				*******/
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	// Save UI state changes to the savedInstanceState.
    	// This bundle will be passed to onCreate if the process is
    	// killed and restarted.
    	savedInstanceState.putBoolean("BTParameters", BTParameters);
    	savedInstanceState.putString("address", magicPadAddress);
    	super.onSaveInstanceState(savedInstanceState);
    }
    
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	BTParameters = savedInstanceState.getBoolean("BTParameters");
    	magicPadAddress = savedInstanceState.getString("address");
    	super.onRestoreInstanceState(savedInstanceState);
    	// Restore UI state from the savedInstanceState.
    	// This bundle has also been passed to onCreate.
    }
    
    
    /********			Methods for BlueTooth				*********/
	
	// Connect to a device
	private void connectToDevice()
	{
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}
    
	// Called after activity requested something
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
         Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if(resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				if (D) Log.d(TAG, "BT list choose: " + address);
				//magicPadDevice.connect(address);
				magicPadAddress = address;
			}
			else
			{
				Toast.makeText(this, R.string.bt_no_device_found, Toast.LENGTH_SHORT).show();
	            finish();
			}
			break;

        case REQUEST_CODE_ENABLE_BLUETOOTH:
            // When the request to enable Bluetooth returns
            if(resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so connect to a MagicPAD           	
            	connectToDevice();
            	Log.d(TAG, "BT enabled: continue");
            } else {
                // User did not enable Bluetooth or an error occurred
            	Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }	
	

}


