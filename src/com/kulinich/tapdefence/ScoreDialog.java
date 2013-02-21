package com.kulinich.tapdefence;

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.swarmconnect.SwarmLeaderboard;


public class ScoreDialog extends DialogFragment {
	public long score = 0;
	
	public String formatScore(long score) {
		DecimalFormat formatter = new DecimalFormat("#,###");
		return formatter.format(score);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.game_over)
				.setMessage(
						getString(R.string.score_msg) + " "
								+ formatScore(score))
				.setPositiveButton(R.string.submit,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								SwarmLeaderboard.submitScoreAndShowLeaderboard(6237, score);
								try {
								    Thread.sleep(250);
								} catch(InterruptedException ex) {
								    Thread.currentThread().interrupt();
								}
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

							}
						});
		return builder.create();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		Activity activity = (Activity) getActivity();
		activity.finish();
	}
}
