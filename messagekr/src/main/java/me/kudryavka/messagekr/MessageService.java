package me.kudryavka.messagekr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

import me.kudryavka.messagekr.MMS.MMS;

/**
 * Created by seyriz on 2016. 12. 3..
 */

public class MessageService{

    private static final String TAG = "MessageService";

    private Context mContext;
    private String mReceiver;
    private String mMessageText;
    private Uri mImageUri;
    private ConnectivityManager mConnMgr;

    private State mState;
    private boolean mListening;
    private boolean mSending;

    private NetworkInfo mNetworkInfo;
    private NetworkInfo mOtherNetworkInfo;

    private ConnectivityManager.NetworkCallback networkCallback;

    private MMS mms;

    public enum State {
        UNKNOWN,
        CONNECTED,
        NOT_CONNECTED
    }


    public MessageService(Context ctx) {
        this.mContext = ctx;
        mConnMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

    }

    public void sendMessage(String receiver, String messageText, @Nullable Uri imageUri){
        this.mReceiver = receiver;
        this.mMessageText = messageText;
        this.mImageUri = imageUri;
        send();
    }

    private void send(){
        try {
            if(mImageUri!=null) {
                new MMS(mContext, mMessageText, mReceiver, mImageUri, mConnMgr).beginMmsConnectivity();
            }
            else {
                new SMS(mContext, mReceiver, mMessageText).send();
            }
        }
        catch (IOException e){
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static int getDefaultSubscriptionId() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return SmsManager.getDefaultSmsSubscriptionId();
        } else {
            return 1;
        }
    }

    public static String getMyPhoneNumber(Context context) {
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getLine1Number();
    }



    /**
     * are we set up to use wifi? if so, send mms over it.
     */
    public static boolean useWifi(Context context) {
        if (isMmsOverWifiEnabled(context)) {
            ConnectivityManager mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (mConnMgr != null) {
                NetworkInfo niWF = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if ((niWF != null) && (niWF.isConnected())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determins whether or not the app has enabled MMS over WiFi
     * @param context
     * @return true if enabled
     */
    public static boolean isMmsOverWifiEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("mms_over_wifi", false);
    }

    public static Bitmap getBitmapFromUri(Context mContext, Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = mContext.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e("getBitmapFromUri", "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                Log.e("getBitmapFromUri", "Error closing ParcelFile Descriptor", e);
            }
        }
    }


}
