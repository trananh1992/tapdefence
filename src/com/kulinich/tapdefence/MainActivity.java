package com.kulinich.tapdefence;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.swarmconnect.Swarm;
import com.swarmconnect.SwarmActivity;
import com.swarmconnect.SwarmLeaderboard;

@SuppressLint("NewApi")
public class MainActivity extends SwarmActivity {
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Swarm.init(this, 4047, "2c3b6aceb6389e3edeafe239ae9fcb65");
		
		setContentView(R.layout.activity_main);
	}

	public void gameStart(View v) {
		Intent myIntent = new Intent(this, GameActivity.class);
		startActivity(myIntent);
	}
	
	public void goLeaderboard(View v) {
		SwarmLeaderboard.showLeaderboard(6237);
	}
	
	public void goDashboard(View v) {
		Swarm.showDashboard();
	}
}
