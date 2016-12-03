package me.kudryavka.messagekr.MMS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.nokia.IMMConstants;
import com.nokia.MMContent;
import com.nokia.MMEncoder;
import com.nokia.MMMessage;
import com.nokia.MMResponse;
import com.nokia.MMSender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;

import me.kudryavka.messagekr.APNHelper;
import me.kudryavka.messagekr.MessageService;
import me.kudryavka.messagekr.PhoneEx;

/**
 * Created by seyriz on 2016. 12. 3..
 */

public class MMS {

    private static final String TAG = "MMS";

    private Context mContext;
    private String mReceiver;
    private String mMessageText;
    private Uri mImageUri;
    private PowerManager.WakeLock mWakeLock;
    private ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver;
    private ConnectivityManager mConnMgr;

    private MessageService.State mState;
    private boolean mListening;
    private boolean mSending;

    private NetworkInfo mNetworkInfo;
    private NetworkInfo mOtherNetworkInfo;

    private ConnectivityManager.NetworkCallback networkCallback;

    public MMS(Context mContext, String mMessageText, String mReceiver, Uri mImageUri, ConnectivityManager mConnMgr) {
        this.mContext = mContext;
        this.mMessageText = mMessageText;
        this.mReceiver = mReceiver;
        this.mImageUri = mImageUri;
        this.mConnMgr = mConnMgr;


        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mConnectivityBroadcastReceiver, filter);
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

    public int beginMmsConnectivity() throws IOException {
        // Take a wake lock so we don't fall asleep before the message is downloaded.
        createWakeLock();

        int result = -1;
        if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.LOLLIPOP) {
            result = mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, PhoneEx.FEATURE_ENABLE_MMS);


            switch (result) {
                case PhoneEx.APN_ALREADY_ACTIVE:
                case PhoneEx.APN_REQUEST_STARTED:
                    acquireWakeLock();
                    return result;
            }


            Log.v(TAG, "beginMmsConnectivity: result=" + result);

            throw new IOException("Cannot establish MMS connectivity");
        }
        else {
            NetworkRequest request;
            if (!MessageService.useWifi(mContext)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    request = new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                            .setNetworkSpecifier(Integer.toString(MessageService.getDefaultSubscriptionId()))
                            .build();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    request = new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                            .build();
                } else {
                    throw new IllegalStateException("This clause never running");
                }
            } else {
                request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
            }
            networkCallback = new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    acquireWakeLock();
                    if(mSending == false)
                    {
                        mSending = true;
                        sendMMSUsingNokiaAPI();
                    }
                    Log.d(TAG, "NetworkCallbackListener.onAvailable: network=" + network);
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    releaseWakeLock();
                    Log.d(TAG, "NetworkCallbackListener.onLost: network=" + network);

                }
            };

            mConnMgr.requestNetwork(request, networkCallback);
        }
        return PhoneEx.APN_REQUEST_STARTED;
    }

    public void endMmsConnectivity() {
        // End the connectivity
        try {
            Log.v(TAG, "endMmsConnectivity");
            if (mConnMgr != null) {
                if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.LOLLIPOP) {
                    mConnMgr.stopUsingNetworkFeature(
                            ConnectivityManager.TYPE_MOBILE,
                            PhoneEx.FEATURE_ENABLE_MMS);
                }
                else {
                    if(networkCallback != null) {
                        mConnMgr.unregisterNetworkCallback(networkCallback);
                    }
                }
            }
        } finally {
            releaseWakeLock();
        }
    }

    private void sendMMSUsingNokiaAPI()
    {
        // Magic happens here.

        MMMessage mm = new MMMessage();
        SetMessage(mm, mReceiver);
        AddContents(mm, mImageUri, mMessageText);

        MMEncoder encoder=new MMEncoder();
        encoder.setMessage(mm);

        try {
            encoder.encodeMessage();
            byte[] out = encoder.getMessage();

            MMSender sender = new MMSender();
            APNHelper apnHelper = new APNHelper(mContext);
            APNHelper.APN results = apnHelper.getMMSApns();

            final String MMSCenterUrl = results.MMSCenterUrl;
            final String MMSProxy = results.MMSProxy;
            final int MMSPort = results.MMSPort;
            final Boolean  isProxySet = (MMSProxy != null) && (MMSProxy.trim().length() != 0);

            sender.setMMSCURL(MMSCenterUrl);
            sender.addHeader("X-NOKIA-MMSC-Charging", "100");

            MMResponse mmResponse = sender.send(out, isProxySet, MMSProxy, MMSPort);
            Log.d(TAG, "Message sent to " + sender.getMMSCURL());
            Log.d(TAG, "Response code: " + mmResponse.getResponseCode() + " " + mmResponse.getResponseMessage());

            Enumeration keys = mmResponse.getHeadersList();
            while (keys.hasMoreElements()){
                String key = (String) keys.nextElement();
                String value = (String) mmResponse.getHeaderValue(key);
                Log.d(TAG, (key + ": " + value));
            }

            if(mmResponse.getResponseCode() == 200)
            {
                // 200 Successful, disconnect and reset.
                endMmsConnectivity();
                mSending = false;
                mListening = false;

            }
            else
            {
                // kill dew :D hhaha
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }



    private void SetMessage(MMMessage mm, String receiver) {
        mm.setVersion(IMMConstants.MMS_VERSION_10);
        mm.setMessageType(IMMConstants.MESSAGE_TYPE_M_SEND_REQ);
        mm.setTransactionId("0000000066");
        mm.setDate(new Date(System.currentTimeMillis()));
        mm.setFrom(MessageService.getMyPhoneNumber(mContext) + "/TYPE=PLMN"); // doesnt work, i wish this worked as it should be
        mm.addToAddress(receiver + "/TYPE=PLMN");
        mm.setDeliveryReport(true);
        mm.setReadReply(false);
        mm.setSenderVisibility(IMMConstants.SENDER_VISIBILITY_SHOW);
        mm.setSubject("");
        mm.setMessageClass(IMMConstants.MESSAGE_CLASS_PERSONAL);
        mm.setPriority(IMMConstants.PRIORITY_LOW);
        mm.setContentType(IMMConstants.CT_APPLICATION_MULTIPART_MIXED);

//	    In case of multipart related message and a smil presentation available
//	    mm.setContentType(IMMConstants.CT_APPLICATION_MULTIPART_RELATED);
//	    mm.setMultipartRelatedType(IMMConstants.CT_APPLICATION_SMIL);
//	    mm.setPresentationId("<A0>"); // where <A0> is the id of the content containing the SMIL presentation

    }

    private void AddContents(MMMessage mm, Uri uri, String msg) {
	    /*Path where contents are stored*/
        Bitmap image = MessageService.getBitmapFromUri(mContext, uri);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bitmapdata = stream.toByteArray();

        // Adds text content
        MMContent part1 = new MMContent();
        byte[] buf1 = bitmapdata;
        part1.setContent(buf1, 0, buf1.length);
        part1.setContentId("<0>");
        part1.setType(IMMConstants.CT_IMAGE_PNG);
        mm.addContent(part1);

        MMContent part2 = new MMContent();
        byte[] buf2 = new byte[]{};
        try{
            buf2 = msg.getBytes("euc-kr");
        } catch (UnsupportedEncodingException e){
            Log.e("MMS", "FAILED", e);
        }
        part2.setContent(buf2, 0, buf2.length);
        part2.setContentId("<1>");
        part2.setType(IMMConstants.CT_TEXT_PLAIN+"; charset=\"euc-kr\";");
        mm.addContent(part2);

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
                mState = MessageService.State.NOT_CONNECTED;
            } else {
                mState = MessageService.State.CONNECTED;
            }

            mNetworkInfo = mConnMgr.getActiveNetworkInfo();
            mOtherNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            Log.v(TAG, "connected..");

            if(mSending == false)
            {
                mSending = true;
                sendMMSUsingNokiaAPI();
            }
//            }
        }
    }
}
