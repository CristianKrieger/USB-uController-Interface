package com.kriegersoftware.demos.usbinterface.ui;

import com.kriegersoftware.demos.usbinterface.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class HomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}
}
