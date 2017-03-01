package me.kudryavka.messagekr.BroadcastReceiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * Created by seyriz on 2016. 12. 3..
 */

public class SmsSentBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsSentBroadcastReceiver";
    public static final int REQ_CODE = 122;

    public void onReceive(Context context, Intent intent) {
        switch (getResultCode())
        {
            case Activity.RESULT_OK:
                Log.e(TAG, "SMS sent");
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                Log.e(TAG, "Generic failure");
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                Log.e(TAG, "No service");
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                Log.e(TAG, "Null PDU");
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                Log.e(TAG, "Radio off");
                break;
        }
    }
}
