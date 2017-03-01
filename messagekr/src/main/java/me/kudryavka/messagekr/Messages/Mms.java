package me.kudryavka.messagekr.Messages;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nokia.IMMConstants;
import com.nokia.MMContent;
import com.nokia.MMEncoder;
import com.nokia.MMEncoderException;
import com.nokia.MMMessage;
import com.nokia.MMResponse;
import com.nokia.MMSender;
import com.nokia.MMSenderException;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import me.kudryavka.messagekr.APNHelper;
import me.kudryavka.messagekr.PhoneEx;

/**
 * Created by hanwool on 2017. 2. 27..
 */

public class Mms {
    public static final String TAG = Mms.class.getSimpleName();

    enum STATE {
        NOT_CONNECTED,
        CONNECTED,
        SENDING,
        SENDED
    }

    public static final String ENCODING_EUC_KR = "euc-kr";
    public static final String ENCODING_UTF_8 = "utf-8";

    public String mCurrentEncoding = ENCODING_EUC_KR;

    private Context mContext;
    private ArrayList<String> mReceiveNumbers;
    private String mMessageSubject;
    private String mMessageString;
    private Bitmap mAttachedImage;

    private ConnectivityManager mConnectivityManager;
    private PowerManager.WakeLock mWakeLock;

    private MMMessage mMmMessage;

    private STATE state = STATE.NOT_CONNECTED;

    public Mms(Context mContext, String receiver, String message, Bitmap attachedImage) {
        this.mContext = mContext;

        this.mReceiveNumbers = new ArrayList<>();
        this.mReceiveNumbers.add(receiver);

        this.mMessageSubject = "";
        this.mMessageString = message;
        this.mAttachedImage = attachedImage;
    }

    public Mms(Context mContext, ArrayList<String> receivers, String message, Bitmap attachedImage) {
        this.mContext = mContext;
        this.mReceiveNumbers = receivers;
        this.mMessageSubject = "";
        this.mMessageString = message;
        this.mAttachedImage = attachedImage;
    }

    public Mms(Context mContext, String receiver, String subject, String message, Bitmap attachedImage) {
        this.mContext = mContext;

        this.mReceiveNumbers = new ArrayList<>();
        this.mReceiveNumbers.add(receiver);

        this.mMessageSubject = subject;
        this.mMessageString = message;
        this.mAttachedImage = attachedImage;
    }

    public Mms(Context mContext, ArrayList<String> receivers, String subject, String message, Bitmap attachedImage) {
        this.mContext = mContext;
        this.mReceiveNumbers = receivers;
        this.mMessageSubject = subject;
        this.mMessageString = message;
        this.mAttachedImage = attachedImage;
    }

    public ArrayList<String> getmReceiveNumbers() {
        return mReceiveNumbers;
    }

    public void setmReceiveNumbers(ArrayList<String> mReceiveNumbers) {
        this.mReceiveNumbers = mReceiveNumbers;
    }

    public String getMessage() {
        return mMessageString;
    }

    public void setMessage(String message) {
        this.mMessageString = message;
    }

    public Bitmap getAttachedImage() {
        return mAttachedImage;
    }

    public void setAttachedImage(Bitmap attachedImage) {
        this.mAttachedImage = attachedImage;
    }

    public void send() {
        requestNetwork();
    }

    private void requestNetwork(){
        acquireWakeLock();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            requestNetworkLegacy();
        } else {
            requestNetworkLollipop();
        }
    }

    @TargetApi(1)
    private void requestNetworkLegacy(){
        int result = getConnectivityManager().startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, PhoneEx.FEATURE_ENABLE_MMS);
        mContext.registerReceiver(mNetworkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        switch (result) {
            case PhoneEx.APN_REQUEST_FAILED:
            case PhoneEx.APN_TYPE_NOT_AVAILABLE:
            case PhoneEx.APN_START_FAILED:
                releaseWakeLock();
                releaseNetwork();
        }
    }

    @TargetApi(21)
    private void requestNetworkLollipop() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
        getConnectivityManager().requestNetwork(request, new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(Network network) {
                Log.i(TAG, "requestNetworkLollipop::onAvailable::"+network);
                state = STATE.CONNECTED;
                super.onAvailable(network);
                for(String receiveNumber : mReceiveNumbers) {
                    Log.i(TAG, "requestNetworkLollipop::receiveNumber:"+receiveNumber);
                    makeMmMessage(receiveNumber, network);
                }
                getConnectivityManager().unregisterNetworkCallback(this);
            }

            @Override
            public void onLost(Network network) {
                Log.i(TAG, "requestNetworkLollipop::onLost");
                super.onLost(network);
                getConnectivityManager().unregisterNetworkCallback(this);
            }
        });
    }

    private void releaseNetwork() {
        state = STATE.SENDED;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                releaseNetworkLegacy();
            } else {
                releaseNetworkLollipop();
            }
        }
        finally {
            releaseWakeLock();
        }
    }

    @TargetApi(1)
    private void releaseNetworkLegacy() throws IllegalStateException {
        getConnectivityManager().stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, PhoneEx.FEATURE_ENABLE_MMS);
        mContext.unregisterReceiver(mNetworkChangeReceiver);
    }

    @TargetApi(21)
    private void releaseNetworkLollipop() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build();
        getConnectivityManager().requestNetwork(request, new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(Network network) {
                Log.i(TAG, "releaseNetworkLollipop::onAvailable");
                super.onAvailable(network);
                getConnectivityManager().unregisterNetworkCallback(this);
            }

            @Override
            public void onLost(Network network) {
                Log.i(TAG, "releaseNetworkLollipop::onLost");
                super.onLost(network);
                getConnectivityManager().unregisterNetworkCallback(this);
            }
        });
    }

    /**
     * 한국 전화(+82)를 로컬전화번호(01x~)로 변화해서 리턴
     * @return 로컬 전화번호
     */
    private String getPhoneNumberLine1ForKorea() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getLine1Number().replace("+82", "0");
    }

    private void makeMmMessage(String receiveNumber, @Nullable Network network) {
        state = STATE.SENDING;
        mMmMessage = new MMMessage();

        setMessageHeader(receiveNumber);
        try {
            setMessageContents();
        }
        catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }
        finally {
            releaseNetwork();
        }

        MMEncoder encoder = new MMEncoder();
        encoder.setMessage(mMmMessage);
        try {
            encoder.encodeMessage();
            byte[] out = encoder.getMessage();
            APNHelper.APN currentApn = APNHelper.getInstance(mContext).getMMSApns();
            Log.e(TAG, "makeMmMessage::APN: " + currentApn);
            boolean isProxySetted = currentApn.getMMSProxy() != null || !currentApn.getMMSProxy().isEmpty();
            MMSender sender = new MMSender();
            sender.setMMSCURL(currentApn.getMMSCenterUrl());
            sender.addHeader("X-NOKIA-MMSC-Charging", "100");

            MMResponse response;
            if(network == null) {
                response = sender.send(out, isProxySetted, currentApn.getMMSProxy(), currentApn.getMMSPort());
            }
            else {
                response = sender.send(out, isProxySetted, currentApn.getMMSProxy(), currentApn.getMMSPort(), network);
            }
            Log.e(TAG, "makeMmMessage::resp\nstatus: " + response.getResponseCode() + "\nmessage: " + response.getResponseMessage());
        }
        catch (MMEncoderException | MMSenderException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        finally {
            releaseNetwork();
        }
    }

    /**
     * 메세지 헤더 작성
     */
    private void setMessageHeader(String receiveNumber) {
        mMmMessage.setVersion(IMMConstants.MMS_VERSION_10);
        mMmMessage.setMessageType(IMMConstants.MESSAGE_TYPE_M_SEND_REQ);
        mMmMessage.setTransactionId("0000000066");
        mMmMessage.setDate(new Date(System.currentTimeMillis()));

        // '-'와 공백 제거
        receiveNumber = receiveNumber.replaceAll("\\-", "").replaceAll(" ", "").replaceAll("\\+82", "0");
        mMmMessage.addToAddress(receiveNumber + "/TYPE=PLMN");
        mMmMessage.setDeliveryReport(true);
        mMmMessage.setReadReply(false);
        mMmMessage.setSenderVisibility(IMMConstants.SENDER_VISIBILITY_SHOW);

        mMmMessage.setSubject(mMessageSubject);

        mMmMessage.setMessageClass(IMMConstants.MESSAGE_CLASS_PERSONAL);
        mMmMessage.setPriority(IMMConstants.PRIORITY_LOW);
        mMmMessage.setContentType(IMMConstants.CT_APPLICATION_MULTIPART_MIXED);
    }

    private void setMessageContents() throws UnsupportedEncodingException{


        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        BitmapFactory.Options options = new BitmapFactory.Options();
        // 사진크기 고정
        options.inScaled = false;

        mAttachedImage.compress(Bitmap.CompressFormat.PNG, 90, byteArrayOutputStream);

        MMContent part1 = new MMContent();
        byte[] buffer1 = byteArrayOutputStream.toByteArray();

        part1.setContent(buffer1, 0, buffer1.length);
        part1.setContentId("<0>");

        // TODO : GIF 첨부 기능 추가
        part1.setType(IMMConstants.CT_IMAGE_PNG);

        MMContent part2 = new MMContent();
        if(APNHelper.getInstance(mContext).getCarrierAndNetworkType()[0].equalsIgnoreCase("OLLEH") ||
                APNHelper.getInstance(mContext).getCarrierAndNetworkType()[0].equalsIgnoreCase("KT")) {
            mMessageString = mMessageString.replaceAll("\n", "<br>");
            part2.setType(IMMConstants.CT_TEXT_HTML);
        }
        else {
            part2.setType(IMMConstants.CT_TEXT_PLAIN + "; charset=\"" + mCurrentEncoding + "\";");
        }

        byte[] buffer2 = mMessageString.getBytes(mCurrentEncoding);
        part2.setContent(buffer2, 0, buffer2.length);
        part2.setContentId("<1>");

        mMmMessage.addContent(part1);
        mMmMessage.addContent(part2);

    }

    private ConnectivityManager getConnectivityManager() {
        if(mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        return mConnectivityManager;
    }

    private PowerManager.WakeLock getWakeLock() {
        if(mWakeLock == null) {
            PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.setReferenceCounted(false);
        }
        return mWakeLock;
    }

    private void acquireWakeLock() {
        // It's okay to double-acquire this because we are not using it
        // in reference-counted mode.
        getWakeLock().acquire();
    }

    private void releaseWakeLock() {
        // Don't release the wake lock if it hasn't been created and acquired.
        if (getWakeLock().isHeld()) {
            getWakeLock().release();
        }
    }

    BroadcastReceiver mNetworkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if(intent.getExtras() != null) {
                    for (String key : intent.getExtras().keySet()) {
                        Log.d(TAG, "mNetworkChangeReceiver::" + key + ": " + intent.getExtras().get(key));
                    }

                    boolean hasConectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                    boolean mmsConnect = false;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        mmsConnect = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1) == ConnectivityManager.TYPE_MOBILE_MMS;
                    } else {
                        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                        mmsConnect = (networkInfo != null) && (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE_MMS) && networkInfo.isConnected();
                    }
                    if (hasConectivity && mmsConnect) {
                        Log.i(TAG, "mNetworkChangeReceiver::connected..");

                        for (String receiveNumber : mReceiveNumbers) {
                            makeMmMessage(receiveNumber, null);
                        }
                    }
                    else{
                        Log.i(TAG, "mNetworkChangeReceiver::not connected");
                    }
                }
                else {
                    Log.e(TAG, "EXTRAS is NULL!");
                }
            }
        }
    };

}
