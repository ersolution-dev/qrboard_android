package kr.co.digigroove.qrboard_android

/**
 * 셋팅할 부분 모두 정의
 */
class SettingValue {
    // LOCAL TEST
    val SERVICE_URL = "http://192.168.0.63:8080"
    // TEST SERVER
//    public static final String SERVICE_URL          = "http://qrbdtool.ersolution.cc:8083/";

    val PREF_KEY_USER_IDX       = "USER_IDX"            // 사용자 인덱스
    val PREF_KEY_AUTO_LOGIN     = "AUTO_LOGIN"          // 자동로그인 여부
    val PREF_KEY_SCHEME_URL     = "SCHEME_URL"          // 스키마 PARAM URL

    val ISP = "ispmobile"
    val BANKPAY = "kftc-bankpay"
    val HYUNDAI_APPCARD = "hdcardappcardansimclick" //intent:hdcardappcardansimclick://appcard?acctid=201605092050048514902797477441#Intent;package=com.hyundaicard.appcard;end;
    val KB_APPCARD = "kb-acp" //intent://pay?srCode=5681318&kb-acp://#Intent;scheme=kb-acp;package=com.kbcard.cxh.appcard;end;
    val PACKAGE_ISP = "kvp.jjy.MispAndroid320"
    val PACKAGE_BANKPAY = "com.kftc.bankpay.android"

}
