package me.kudryavka.messagekr.Messages;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by seyriz on 2016. 12. 3..
 */

public class Sms {
    private static final String TAG = "SMS";
    private Context mContext;
    private ArrayList<String> mReceiveNumbers;
    private String mMessageText;
    private String mMessageSubject;

    public Sms(Context mContext, ArrayList<String> mReceiver, String mMessageText) {
        this.mContext = mContext;
        this.mReceiveNumbers = mReceiver;
        this.mMessageSubject = null;
        this.mMessageText = mMessageText;
    }

    public Sms(Context mContext, ArrayList<String> mReceiver, String mMessageSubject, String mMessageText) {
        this.mContext = mContext;
        this.mReceiveNumbers = mReceiver;
        this.mMessageSubject = mMessageSubject;
        this.mMessageText = mMessageText;
    }

    public void send(){
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(mContext, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(mContext, 0,
                new Intent(DELIVERED), 0);


        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> msgs = sms.divideMessage(mMessageText);
        if(msgs.size()>1){
            ArrayList<PendingIntent> sentPIs = new ArrayList<>();
            ArrayList<PendingIntent> deliveredPIs = new ArrayList<>();
            for(String s : msgs){
                sentPIs.add(sentPI);
                deliveredPIs.add(deliveredPI);
            }
            for(String number : mReceiveNumbers) {
                sms.sendMultipartTextMessage(number, mMessageSubject, msgs, sentPIs, deliveredPIs);
            }
        }
        else {
            for(String number : mReceiveNumbers) {
                sms.sendTextMessage(number, mMessageSubject, mMessageText, sentPI, deliveredPI);
            }
        }
    }
}
