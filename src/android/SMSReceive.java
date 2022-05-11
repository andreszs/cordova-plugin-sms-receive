package com.andreszs.smsreceive;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.telephony.SmsMessage;
import android.provider.Telephony;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SMSReceive extends CordovaPlugin {

	private static final String LOG_TAG = "cordova-plugin-sms-receive";
	private static final String ACTION_START_WATCH = "startWatch";
	private static final String ACTION_STOP_WATCH = "stopWatch";
	private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	private static final int START_WATCH_REQ_CODE = 194;
	private static boolean STARTED = false;

	private BroadcastReceiver mReceiver = null;
	private CallbackContext callbackContext;

	public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
		PluginResult result = null;
		this.callbackContext = callbackContext;

		if (action.equals(ACTION_START_WATCH)) {
			if (!hasPermission()) {
				requestPermissions(START_WATCH_REQ_CODE);
			} else {
				this.startWatch();
			}
		} else if (action.equals(ACTION_STOP_WATCH)) {
			this.stopWatch();
		} else {
			Log.d(LOG_TAG, String.format("Invalid action passed: %s", action));
			result = new PluginResult(PluginResult.Status.INVALID_ACTION);
			callbackContext.sendPluginResult(result);
		}
		return true;
	}

	public void onDestroy() {
		this.stopWatch();
	}

	private void startWatch() {
		Log.d(LOG_TAG, ACTION_START_WATCH);
		if (this.STARTED) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, "SMS_WATCHING_ALREADY_STARTED");
			result.setKeepCallback(true);
			this.callbackContext.sendPluginResult(result);
		} else {
			this.createIncomingSMSReceiver();
		}
	}

	private void stopWatch() {
		Log.d(LOG_TAG, ACTION_STOP_WATCH);
		if (this.mReceiver != null) {
			try {
				webView.getContext().unregisterReceiver(this.mReceiver);
				this.STARTED = false;
				PluginResult result = new PluginResult(PluginResult.Status.OK, "SMS_WATCHING_STOPPED");
				result.setKeepCallback(false);
				this.callbackContext.sendPluginResult(result);
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage());
				PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
				this.callbackContext.sendPluginResult(result);
			} finally {
				this.mReceiver = null;
			}
		}
	}

	private void onSMSArrive(JSONObject json) {
		try {
			PluginResult result = new PluginResult(PluginResult.Status.OK, json);
			result.setKeepCallback(true);
			this.callbackContext.sendPluginResult(result);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
			this.callbackContext.sendPluginResult(result);
		}
	}

	protected void createIncomingSMSReceiver() {
		this.mReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {
					Log.d(LOG_TAG, "SMS_RECEIVED_ACTION");
					// Create SMS container
					SmsMessage[] sms = null;
					// Use API 19+ to read SMS
					try {
						sms = Telephony.Sms.Intents.getMessagesFromIntent(intent);
					} catch (Exception e) {
						Log.e(LOG_TAG, e.getMessage());
						PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
						callbackContext.sendPluginResult(result);
					}
					// Get SMS contents as JSON
					if (sms == null || sms.length == 0) {
						Log.e(LOG_TAG, "SMS_EQUALS_NULL");
						PluginResult result = new PluginResult(PluginResult.Status.ERROR, "SMS_EQUALS_NULL");
						result.setKeepCallback(true);
						callbackContext.sendPluginResult(result);
					} else {
						JSONObject json_sms = SMSReceive.this.getJsonFromSmsMessage(sms);
						SMSReceive.this.onSMSArrive(json_sms);
					}
				}
			}
		};
		IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);
		try {
			webView.getContext().registerReceiver(this.mReceiver, filter);
			this.STARTED = true;
			PluginResult result = new PluginResult(PluginResult.Status.OK, "SMS_WATCHING_STARTED");
			result.setKeepCallback(true);
			this.callbackContext.sendPluginResult(result);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
			this.callbackContext.sendPluginResult(result);
		}
	}

	/**
	 * Return received SMS as JSON object.
	 *
	 * @param SmsMessage[] sms
	 * @return JSONObject
	 */
	private JSONObject getJsonFromSmsMessage(SmsMessage[] sms) {
		JSONObject json_sms = new JSONObject();
		String messageBody = "";
		try {
			json_sms.put("address", sms[0].getOriginatingAddress());
			for (int i = 0; i < sms.length; i++) {
				messageBody += sms[i].getMessageBody().toString();
			}
			json_sms.put("body", messageBody);
			json_sms.put("date_sent", sms[0].getTimestampMillis());
			json_sms.put("date", System.currentTimeMillis());
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage());
			PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
			callbackContext.sendPluginResult(result);
		}
		return json_sms;
	}

	/**
	 * Check if we have been granted SMS receiving permission on Android 6+
	 *
	 * @return boolean
	 */
	private boolean hasPermission() {

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		} else if (cordova.getActivity().checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED) {
			return false;
		}

		return true;

	}

	/**
	 * We override this so that we can access the permissions variable, which no longer exists in the parent class, since we can't initialize it reliably in the constructor!
	 *
	 * @param requestCode The code to get request action
	 */
	public void requestPermissions(int requestCode) {

		cordova.requestPermission(this, requestCode, Manifest.permission.RECEIVE_SMS);

	}

	/**
	 * processes the result of permission request
	 *
	 * @param requestCode The code to get request action
	 * @param permissions The collection of permissions
	 * @param grantResults The result of grant
	 */
	public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {

		PluginResult result;
		for (int r : grantResults) {
			if (r == PackageManager.PERMISSION_DENIED) {
				result = new PluginResult(PluginResult.Status.ERROR, "PERMISSION_DENIED");
				this.callbackContext.sendPluginResult(result);
				return;
			}
		}
		if (requestCode == START_WATCH_REQ_CODE) {
			this.startWatch();
		}

	}

}
