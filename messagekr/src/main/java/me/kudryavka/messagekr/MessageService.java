package me.kudryavka.messagekr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

import me.kudryavka.messagekr.Messages.Mms;
import me.kudryavka.messagekr.Messages.Sms;

/**
 * Created by seyriz on 2016. 12. 3..
 */

public class MessageService{

    private static final String TAG = "MessageService";

    private Context mContext;
    private ArrayList<String> mReceiveNumbers;
    private String mMessageText;
    private Uri mImageUri;
    private String mMessageSubject;

    public MessageService(Context ctx, String receiveNumber, String messageText, Uri imageUri) {
        this.mContext = ctx;

        this.mReceiveNumbers = new ArrayList<>();
        this.mReceiveNumbers.add(receiveNumber);

        this.mMessageSubject = "";
        this.mMessageText = messageText;
        this.mImageUri = imageUri;
    }

    public MessageService(Context ctx, ArrayList<String> receiveNumbers, String messageText, Uri imageUri) {
        this.mContext = ctx;

        this.mReceiveNumbers = receiveNumbers;

        this.mMessageSubject = "";
        this.mMessageText = messageText;
        this.mImageUri = imageUri;
    }

    public MessageService(Context ctx, String receiveNumber, String messageSubject, String messageText, Uri imageUri) {
        this.mContext = ctx;

        this.mReceiveNumbers = new ArrayList<>();
        this.mReceiveNumbers.add(receiveNumber);

        this.mMessageSubject = messageSubject;
        this.mMessageText = messageText;
        this.mImageUri = imageUri;
    }

    public MessageService(Context ctx, ArrayList<String> receiveNumbers, String messageSubject, String messageText, Uri imageUri) {
        this.mContext = ctx;

        this.mReceiveNumbers = receiveNumbers;

        this.mMessageSubject = messageSubject;
        this.mMessageText = messageText;
        this.mImageUri = imageUri;
    }

    public void send(){
        Log.d(TAG, "mImageUri:"+mImageUri);
        if(mImageUri!=null && !mImageUri.equals(Uri.EMPTY)) {
            Log.w(TAG, String.format("MMS [%s] %s: %s %s", mReceiveNumbers, mMessageSubject, mMessageText, mImageUri));
            if(mMessageSubject == null || mMessageSubject.isEmpty()) {
                new Mms(mContext, mReceiveNumbers, mMessageText, getBitmapFromUri(mImageUri)).send();
            }
            else {
                new Mms(mContext, mReceiveNumbers, mMessageSubject, mMessageText, getBitmapFromUri(mImageUri)).send();
            }
        }
        else {
            Log.w(TAG, String.format("SMS [%s] %s: %s", mReceiveNumbers, mMessageSubject, mMessageText));

            if(mMessageSubject == null || mMessageSubject.isEmpty()) {
                new Sms(mContext, mReceiveNumbers, mMessageText).send();
            }
            else {
                new Sms(mContext, mReceiveNumbers, mMessageSubject, mMessageText).send();
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {
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
