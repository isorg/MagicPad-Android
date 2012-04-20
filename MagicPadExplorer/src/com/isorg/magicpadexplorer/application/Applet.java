package com.isorg.magicpadexplorer.application;

import android.graphics.drawable.Drawable;

public class Applet {
	
	private Drawable mIcon;
	private String mName;
	
	
	public Applet(Drawable d, String n) {
		mIcon = d;
		mName = n;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mName == null) ? 0 : mName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Applet other = (Applet) obj;
		if (mName == null) {
			if (other.mName != null)
				return false;
		} else if (!mName.equals(other.mName))
			return false;
		return true;
	}
	
	
	
	//			GETTEURS AND SETTERS			//
	
	public void setIcon (Drawable i) {mIcon = i;}
	public void setName (String n) {mName = n;}
	
	public Drawable getIcon () {return mIcon;}
	public String getName () {return mName;}

	
}