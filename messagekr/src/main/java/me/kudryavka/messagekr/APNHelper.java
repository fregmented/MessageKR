package me.kudryavka.messagekr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class APNHelper {

    private static APN currentAPN;

    private static APNHelper instance;

    /**
     * APN정보를 담고 있습니다.
     * 통신사에 따라 달라집니다.
     */
    public class APN {
        private String APN = "";
        private String MMSCenterUrl = "";
        private int MMSPort = 0;
        private String MMSProxy = "";

        private APN(String Apn, String MMSCenterUrl, int MMSPort, String MMSProxy) {
            this.APN = Apn;
            this.MMSCenterUrl = MMSCenterUrl;
            this.MMSPort = MMSPort;
            this.MMSProxy = MMSProxy;
        }

        @Override
        public String toString() {
            return String.format(Locale.KOREAN,
                    "APN: %s\nMMSCenterUrl: %s\nMMSPort: %d\nMMSProxy: %s ",
                    APN,
                    MMSCenterUrl,
                    MMSPort,
                    MMSProxy);
        }

        public String getAPN() {
            return APN;
        }

        public String getMMSCenterUrl() {
            return MMSCenterUrl;
        }

        public int getMMSPort() {
            return MMSPort;
        }

        public String getMMSProxy() {
            return MMSProxy;
        }
    }

    public static APNHelper getInstance(Context ctx) {
        if(instance == null) {
            instance = new APNHelper(ctx);
        }
        return instance;
    }

    private APNHelper(final Context context) {
        this.context = context;
        getMMSApns();
    }

    /**
     * APN정보를 생성하여서 반환합니다.
     * @see APN
     * @return APN information
     */
    public APN getMMSApns() {
        if(currentAPN==null){
            checkCurrentApn();
        }
        return currentAPN;
    }

    /**
     * 통신사와 통신방식에 따른 APN을 생성합니다.
     * SK와 KT는 3G와 LTE망을 동시에 지원하고,
     * U+는 LTE망만 지원합니다.
     */
    private void checkCurrentApn(){
        String[] network = getCarrierAndNetworkType();

        switch (network[0].toUpperCase(Locale.ENGLISH)){
            case "OLLEH":
            case "KT":
                switch (network[1]){
                    case "lte":
                        currentAPN = new APN("lte.ktfwing.com",
                                "http://mmsc.ktfwing.com:9082",
                                9093,
                                "");
                        break;
                    case "wcdma":
                        currentAPN = new APN("alwayson.ktfwing.com",
                                "http://mmsc.ktfwing.com:9082",
                                9093,
                                "");
                        break;
                }
                break;
            case "SKTelecom":
            case "SKT":
                switch (network[1]){
                    case "lte":
                        currentAPN = new APN("lte.sktelecom.com",
                                "http://omms.nate.com:9082/oma_mms",
                                9093,
                                "lteoma.nate.com");
                        break;
                    case "wcdma":
                        currentAPN = new APN("web.sktelecom.com",
                                "http://omms.nate.com:9082/oma_mms",
                                9093,
                                "smart.nate.com");
                        break;
                }
                break;
            case "LG U+":
                switch (network[1]){
                    case "lte":
                        currentAPN = new APN("internet.lguplus.co.kr",
                                "http://omammsc.uplus.co.kr:9084",
                                9084,
                                "");
                        break;
                }
                break;
            default:
                currentAPN = new APN("", "", 0, "");
                break;
        }
    }

    /**
     * 통신사 이름과 네트워크 방식을 리턴합니다.
     * @return {통신사 이름, 네트워크 방식(wcdma, lte)}
     */
    public String[] getCarrierAndNetworkType(){

        TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String carrierName = manager.getNetworkOperatorName();
        int networkType = manager.getNetworkType();
        switch (networkType){
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return new String[]{carrierName, "wcdma"};
            case TelephonyManager.NETWORK_TYPE_LTE:
                return new String[]{carrierName, "lte"};
            default:
                return new String[]{"", ""};
        }
    }

    private Context context;
}

final class Carriers implements BaseColumns {
    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI =
            Uri.parse("content://telephony/carriers");

    /**
     * The default sort order for this table
     */
    public static final String DEFAULT_SORT_ORDER = "name ASC";

    public static final String NAME = "name";

    public static final String APN = "apn";

    public static final String PROXY = "proxy";

    public static final String PORT = "port";

    public static final String MMSPROXY = "mmsproxy";

    public static final String MMSPORT = "mmsport";

    public static final String SERVER = "server";

    public static final String USER = "user";

    public static final String PASSWORD = "password";

    public static final String MMSC = "mmsc";

    public static final String MCC = "mcc";

    public static final String MNC = "mnc";

    public static final String NUMERIC = "numeric";

    public static final String AUTH_TYPE = "authtype";

    public static final String TYPE = "type";

    public static final String CURRENT = "current";
}