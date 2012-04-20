package com.isorg.magicpadexplorer.application;

import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.isorg.magicpadexplorer.R;

public class AppletAdapter extends BaseAdapter {
	
	
	
	//			ATTRIBUTES				//
	private ArrayList<Applet> appletArray= new ArrayList<Applet>();
	private LayoutInflater  inflater;
	
	
	//			CONSTRUCTOR				//
	public AppletAdapter (Context context) {
		inflater = LayoutInflater.from(context);
	}
	
	
	
	//		GETTERS AND SETTERS			//
	public int getCount() {
		return appletArray.size();
	}
	

	public Applet getItem(long id) {
		if (appletArray != null) 
			for (Applet a : appletArray)
				if (a.hashCode() == id)
					return a;
		return null;
	}
	
	public Applet getItem(int position) {
		//Si la liste existe et que la position est correcte, on renvoit le contact
		if(appletArray != null && position >= 0 && position < appletArray.size())
			return appletArray.get(position);
		return null;
	}
	

	public long getItemId(int position) {
		Applet a = (Applet) getItem(position);
		if (a == null)
			return -1;
		return a.hashCode();
	}
	
	
	
	//		ADD/REMOVE/MODIFY ITEM		//
	
	public void addItem(Applet a) {
		this.appletArray.add(a);
		notifyDataSetChanged();
	}
	
	public void deleteItem (long id) {
		for (Applet a : appletArray)
			if (a.hashCode() == id) {
				appletArray.remove(a);
				notifyDataSetChanged();
			}
	}
	
	public void modifyItem (Applet a, long id) {
		Applet tmp = getItem(id);
		if (tmp != null) {
			tmp.setIcon(a.getIcon());
			tmp.setName(a.getName());
			notifyDataSetChanged();
		}
	}
	
	
	//				VIEW				//
	private class ViewHolder {
		ImageView ivIcon;
		TextView tvName;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(R.layout.applet, null);
			holder.tvName = (TextView)convertView.findViewById(R.id.applet_name);
			holder.ivIcon = (ImageView)convertView.findViewById(R.id.applet_Image);
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.ivIcon.setImageDrawable((appletArray.get(position).getIcon()));
		holder.tvName.setText(appletArray.get(position).getName());
		
		return convertView;
	}



	
}




