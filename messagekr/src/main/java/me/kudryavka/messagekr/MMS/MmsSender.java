package me.kudryavka.messagekr.MMS;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.nokia.IMMConstants;
import com.nokia.MMContent;
import com.nokia.MMEncoder;
import com.nokia.MMMessage;
import com.nokia.MMResponse;
import com.nokia.MMSender;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;

import me.kudryavka.messagekr.APNHelper;
import me.kudryavka.messagekr.MessageService;

/**
 * Created by seyriz on 2016. 12. 3..
 */

public class MmsSender {
    private static final String TAG = "MmsSender";

    static void sendMMSUsingNokiaAPI(Context mContext, String mReceiverNumber, String mMessageText, Uri mImageUri, MmsStatusListener mmsStatusListener)
    {
        // Magic happens here.


        MMMessage mm = new MMMessage();
        SetMessage(mContext, mm, mReceiverNumber);
        try {
            AddContents(mContext, mm, mMessageText, mImageUri);
        }
        catch (UnsupportedEncodingException e){
            Log.e(TAG, e.getMessage(), e);
            mmsStatusListener.onSendFailed();
            return;
        }

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
            final Boolean  isProxySet =   (MMSProxy != null) && (MMSProxy.trim().length() != 0);

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

                mmsStatusListener.onSendSuccess();
            }
            else
            {
                mmsStatusListener.onSendFailed();
                // kill dew :D hhaha
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static void SetMessage(Context mContext, MMMessage mm, String mReceiverNumber) {
        mm.setVersion(IMMConstants.MMS_VERSION_10);
        mm.setMessageType(IMMConstants.MESSAGE_TYPE_M_SEND_REQ);
        mm.setTransactionId("0000000066");
        mm.setDate(new Date(System.currentTimeMillis()));
        mm.setFrom(MessageService.getMyPhoneNumber(mContext)+"/TYPE=PLMN"); // doesnt work, i wish this worked as it should be
        mm.addToAddress(mReceiverNumber+"/TYPE=PLMN");
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

    static void AddContents(Context mContext, MMMessage mm, String mMessageText, Uri mImage) throws UnsupportedEncodingException{
	    /*Path where contents are stored*/
        Bitmap image = MessageService.getBitmapFromUri(mContext, mImage);
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
        buf2 = mMessageText.getBytes("euc-kr");
        part2.setContent(buf2, 0, buf2.length);
        part2.setContentId("<1>");
        part2.setType(IMMConstants.CT_TEXT_PLAIN+"; charset=\"euc-kr\";");
        mm.addContent(part2);
    }
}
