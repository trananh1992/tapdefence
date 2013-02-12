package com.kulinich.tapdefence;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.kulinich.tapdefence.R;
import com.swarmconnect.Swarm;
import com.swarmconnect.SwarmLeaderboard;

public class ScoreDialog extends DialogFragment {

	public long score = 0;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.game_over)
				.setMessage(
						getString(R.string.score_msg) + " "
								+ String.valueOf(score))
				.setPositiveButton(R.string.submit,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								SwarmLeaderboard.submitScoreAndShowLeaderboard(6237, score);
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

							}
						});
		// Create the AlertDialog object and return it
		return builder.create();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		Activity activity = (Activity) getActivity();
		activity.finish();
	}

}
