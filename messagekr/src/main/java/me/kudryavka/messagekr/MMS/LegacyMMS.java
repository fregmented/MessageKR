package me.kudryavka.messagekr.MMS;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.nokia.IMMConstants;
import com.nokia.MMContent;
import com.nokia.MMEncoder;
import com.nokia.MMMessage;
import com.nokia.MMResponse;
import com.nokia.MMSender;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore.Images.Media;
import android.text.TextUtils;
import android.util.Log;

import me.kudryavka.messagekr.APNHelper;
import me.kudryavka.messagekr.MessageService;
import me.kudryavka.messagekr.PhoneEx;

public class LegacyMMS implements MmsStatusListener{

    private static final String TAG = "LegacyMMS";
    private ConnectivityManager mConnMgr;
    private PowerManager.WakeLock mWakeLock;
    private ConnectivityBroadcastReceiver mReceiver;
    private Context mContext;
    private String mReceiverNumber;
    private String mMessageText;
    private Uri mImage;

    private NetworkInfo mNetworkInfo;
    private NetworkInfo mOtherNetworkInfo;

    public enum State {
        UNKNOWN,
        CONNECTED,
        NOT_CONNECTED
    }

    private State mState;
    private boolean mListening;
    private boolean mSending;

    public LegacyMMS(Context context, String receiver, String message, Uri image){
        mContext = context;
        mListening = true;
        mSending = false;
        mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mReceiver = new ConnectivityBroadcastReceiver();
        mReceiverNumber = receiver;
        mMessageText = message;
        mImage = image;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(mReceiver, filter);

        try {

            // Ask to start the connection to the APN. Pulled from Android source code.
            int result = beginMmsConnectivity();

            if (result != PhoneEx.APN_ALREADY_ACTIVE) {
                Log.v(TAG, "Extending MMS connectivity returned " + result + " instead of APN_ALREADY_ACTIVE");
                // Just wait for connectivity startup without
                // any new request of APN switch.
                return;
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, e.getMessage(), e);
        }
    }

    protected void endMmsConnectivity() {
        // End the connectivity
        try {
            Log.v(TAG, "endMmsConnectivity");
            if (mConnMgr != null) {
                mConnMgr.stopUsingNetworkFeature(
                        ConnectivityManager.TYPE_MOBILE,
                        PhoneEx.FEATURE_ENABLE_MMS);
            }
        } finally {
            releaseWakeLock();
        }
    }

    protected int beginMmsConnectivity() throws IOException {
        // Take a wake lock so we don't fall asleep before the message is downloaded.
        createWakeLock();

        int result = mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, PhoneEx.FEATURE_ENABLE_MMS);

        Log.v(TAG, "beginMmsConnectivity: result=" + result);

        switch (result) {
            case PhoneEx.APN_ALREADY_ACTIVE:
            case PhoneEx.APN_REQUEST_STARTED:
                acquireWakeLock();
                return result;
        }

        throw new IOException("Cannot establish MMS connectivity");
    }

    private synchronized void createWakeLock() {
        // Create a new wake lock if we haven't made one yet.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS Connectivity");
            mWakeLock.setReferenceCounted(false);
        }
    }

    private void acquireWakeLock() {
        // It's okay to double-acquire this because we are not using it
        // in reference-counted mode.
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        // Don't release the wake lock if it hasn't been created and acquired.
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public void onSendFailed() {
        Log.i(TAG, "MMS Send Failed");

    }

    @Override
    public void onSendReady() {
        MmsSender.sendMMSUsingNokiaAPI(mContext, mReceiverNumber, mMessageText, mImage, this);
    }

    @Override
    public void onSendSuccess() {
        Log.i(TAG, "MMS Send Successful");
        mSending = false;
        mListening = false;
        endMmsConnectivity();
    }

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION) || mListening == false) {
                Log.w(TAG, "onReceived() called with " + mState.toString() + " and " + intent);
                return;
            }

            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

            if (noConnectivity) {
                mState = State.NOT_CONNECTED;
            } else {
                mState = State.CONNECTED;
            }

            mNetworkInfo = mConnMgr.getActiveNetworkInfo();
            mOtherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

//			mReason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
//			mIsFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);


            // Check availability of the mobile network.
            if ((mNetworkInfo == null) || (mNetworkInfo.getType() != ConnectivityManager.TYPE_MOBILE_MMS)) {
                Log.v(TAG, "   type is not TYPE_MOBILE_MMS, bail");
                return;
            }

            if (!mNetworkInfo.isConnected()) {
                Log.v(TAG, "   TYPE_MOBILE_MMS not connected, bail");
                return;
            }
            else
            {
                Log.v(TAG, "connected..");

                if(mSending == false)
                {
                    mSending = true;
                    onSendReady();
                }
            }
        }
    };
}
