package me.kudryavka.messagingkr;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import me.kudryavka.messagekr.MessageService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQ_PERMISSIONS = 202;
    private static final int REQ_PERMISSIONS_WRITE_SETTING = 202;
    private static final int SAF_REQ_CODE = 123;


    MessageService messageService;

    private ArrayList<String> permissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.write_settings_permission)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.e("MainActivity", "error starting permission intent", e);
                            }
                        }
                    })
                    .show();
        }
        else {
            ArrayList<String> need_perm = getRequestNeededPermission();
            if (need_perm.size() == 0) {
                send();
            } else {
                ActivityCompat.requestPermissions(this, arrayListToStringArray(need_perm), REQ_PERMISSIONS);
            }
        }
    }

    public static String[] arrayListToStringArray(ArrayList<String> arrayList){
        String[] ret = new String[arrayList.size()];
        for(String s : arrayList){
            ret[arrayList.indexOf(s)] = s;
        }
        return ret;
    }

    private ArrayList<String> getRequestNeededPermission() {
        permissions = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        return permissions;
    }

    public String getMyPhoneNumber(Context context) {
        TelephonyManager mTelephonyMgr;
        mTelephonyMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getLine1Number();
    }

    void send(){
        messageService = new MessageService(this);
        messageService.sendMessage(getMyPhoneNumber(this), "THIS IS SMS TEST", null);
        messageService.sendMessage(getMyPhoneNumber(this), "THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;THIS IS LMS TEST;", null);
        getImage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for(String p : permissions){
            Log.d(TAG, "onRequestPermissionsResult::Permission :: "+p);
        }
        if(requestCode == REQ_PERMISSIONS) {
            if(permissions.length != grantResults.length) {
                Log.d(TAG, "onRequestPermissionsResult:: permissions and grantResults length not matched");
            }
            ArrayList<String> denied = new ArrayList<>();
            for(int i = 0; i < permissions.length; i++) {
                Log.e(TAG, "onRequestPermissionsResult::"+permissions[0]+" is "+(grantResults[i]==PackageManager.PERMISSION_GRANTED?"GRANTED":"DENIED"));
                if (grantResults[i]!=PackageManager.PERMISSION_GRANTED) {
                    denied.add(permissions[i]);
                }
            }
            if(denied.size()!=0) {
                Toast.makeText(this, "PERMISSION!!!", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, arrayListToStringArray(denied), REQ_PERMISSIONS);
            }
            else {
                send();
            }
        }
    }

    private void getImage(){
        Intent saf = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        saf.addCategory(Intent.CATEGORY_OPENABLE);
        saf.setType("image/*");
        startActivityForResult(saf, SAF_REQ_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("onActivityResult", requestCode + " " + resultCode);
        if(requestCode==SAF_REQ_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = null;
                if (data != null) {
                    uri = data.getData();
                    Log.d("GET_IMAGE", uri.toString());
                    messageService.sendMessage(getMyPhoneNumber(this), "THIS IS MMS TEST", uri);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
