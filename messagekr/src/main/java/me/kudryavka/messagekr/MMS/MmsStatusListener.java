package me.kudryavka.messagekr.MMS;

/**
 * Created by seyriz on 2016. 12. 3..
 */

public interface MmsStatusListener {
    void onSendSuccess();
    void onSendFailed();
    void onSendReady();
}
