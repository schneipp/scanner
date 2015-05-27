package com.moovaa.plugins.scanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;


public class MoovaaScanner extends CordovaPlugin {

    private CallbackContext callbackContext;

	@Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
	    if (!hasRearFacingCamera()) {
	        callbackContext.error("No rear camera detected");
	        return false;
	    }
	    this.callbackContext = callbackContext;
	    Context context = cordova.getActivity().getApplicationContext();
	    Intent intent = new Intent(context, MoovaaScanVideo.class);
	    intent.putExtra(MoovaaScanVideo.FILENAME, args.getString(0));
        intent.putExtra(MoovaaScanVideo.VOICEOVER, args.getString(2));
	    cordova.startActivityForResult(this, intent, 0);
        return true;
    }

	private boolean hasRearFacingCamera() {
	    Context context = cordova.getActivity().getApplicationContext();
	    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (resultCode == Activity.RESULT_OK) {
	        callbackContext.success(intent.getExtras().getString(MoovaaScanVideo.VIDEO_URI));
	    } else if (resultCode == MoovaaScanVideo.RESULT_ERROR) {
	        String errorMessage = intent.getExtras().getString(MoovaaScanVideo.ERROR_MESSAGE);
	        if (errorMessage != null) {
	            callbackContext.error(errorMessage);
	        } else {
	            callbackContext.error("Failed to take picture");
	        }
	    }
    }

}
