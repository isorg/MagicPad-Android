package com.isorg.magicpadexplorer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

import com.isorg.magicpadexplorer.application.Applet;
import com.isorg.magicpadexplorer.application.AppletAdapter;
import com.isorg.magicpadexplorer.application.Application1;
import com.isorg.magicpadexplorer.application.OnOffApplication;
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
		
	

	// For the debugging
	private String TAG = "magicExplorerActivity";
	private Boolean D = true;	
    
	
	
	//  
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
                
        // Fill the grid
        mAdapter = new AppletAdapter(this);
        for (int i = 0; i< 40; i++)
        	mAdapter.addItem(new Applet(getResources().getDrawable(R.drawable.ic_launcher),"L'application " + (i + 1)));
        mGrid = (GridView) findViewById(R.id.home_page);
        mGrid.setAdapter(mAdapter);

        mAdapter.getItem(0).setName("Connexion Test");
        mAdapter.getItem(0).setIcon(getResources().getDrawable(R.drawable.btlogo2));
        mAdapter.getItem(1).setName("On Off application");
        mAdapter.getItem(1).setIcon(getResources().getDrawable(R.drawable.onbutton));
        mAdapter.getItem(2).setName("Vumeter application");
        mAdapter.getItem(2).setIcon(getResources().getDrawable(R.drawable.vumeterapplication));


        
        // The listener for items
        mGrid.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {

			    if (mAdapter.getItem(position).getName() == "Connexion Test") {
					Intent intent = new Intent(MagicPadExplorerActivity.this, Application1.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }
			    else if (mAdapter.getItem(position).getName() == "On Off application") {
					Intent intent = new Intent(MagicPadExplorerActivity.this, OnOffApplication.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }else if (mAdapter.getItem(position).getName() == "Vumeter application") {
					Intent intent = new Intent(MagicPadExplorerActivity.this, VumeterApplication.class);
					if (D && magicPadAddress == null) Log.d(TAG, "address is null"); 
					intent.putExtra("address", magicPadAddress);
					startActivity(intent);
			    }			    
			    else {
	        	    Applet a = (Applet)mAdapter.getItem(position);
					a.setName("Ca a été appuyé !");
					mAdapter.notifyDataSetChanged();
			    }
        	}
        });
    }
    
    
    @Override
	protected void onResume() {
        if (!BTParameters) {
	        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); 
	        if (bluetoothAdapter == null)
	        	Toast.makeText(this, "No Bluetooth on this device", Toast.LENGTH_LONG);
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
	
	
	
	/*******             THE MENU              *******/
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return super.onCreateOptionsMenu(menu);
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
    	{case R.menu.exit :
    		finish();
    		break;
    	case R.menu.site :
    		Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.isorg.fr/fr/"));
    		startActivity(viewIntent);  
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
}


