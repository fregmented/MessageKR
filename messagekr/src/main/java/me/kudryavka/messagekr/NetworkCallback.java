package me.kudryavka.messagekr;

import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.support.annotation.Nullable;

/**
 * NetworkRequestListener
 *
 */

public interface NetworkCallback {
    void onAvailable(Network network);
    void onLost(Network network);
}