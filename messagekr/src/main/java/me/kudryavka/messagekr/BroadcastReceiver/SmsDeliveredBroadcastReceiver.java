package me.kudryavka.messagekr.BroadcastReceiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by seyriz on 2016. 12. 3..
 */

public class SmsDeliveredBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsDeliveredBroadcastReceiver";
    public static final int REQ_CODE = 123;
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode())
            {
                case Activity.RESULT_OK:
                    Log.e(TAG, "SMS delivered");
                    break;
                case Activity.RESULT_CANCELED:
                    Log.e(TAG, "SMS not delivered");
                    break;
            }
    }
}
