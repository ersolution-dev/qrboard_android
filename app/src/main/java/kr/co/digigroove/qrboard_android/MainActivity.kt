package kr.co.digigroove.qrboard_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.erst.android.api.util.PathUtilManager
import com.erst.android.api.util.SPUtilManager
import com.erst.android.api.webview.*
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.util.*

class MainActivity : AppCompatActivity(),
    AndroidAction.OnCallWebviewPageStartListener,
    AndroidAction.OnCallWebviewPageFinishListener
{

    private val TAG = "MainActivity"

    /** 공용 데이터 처리 클래스 */
    private var spUtilManager = SPUtilManager(this)
    /** 공용 파일경로 클래스 */
    private var pathUtilManager = PathUtilManager(this)
    /** 웹뷰 처리 클래스 */
    private lateinit var androidBridgeFunction: AndroidBridgeFunction
    /** Webview 처리 클래스 */
    private lateinit var extWebViewClient: WebViewClient
//    private lateinit var extWebViewClient: ExtWebViewClient
    private lateinit var webChromeViewClient: ExtWebChromeViewClient
    /** ActivityResult 처리 클래스 */
    private lateinit var androidActivityResult: AndroidActivityResult

    //setting value
    private var sv = SettingValue()
    private var handler = Handler()
    private var URL: String? = ""

    // 로딩창
    var asyncDialog: ProgressDialog? = null

    var SHARE_URL = "shareUrl"
    private var webviewPageFinish = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // splash
        splashView.visibility = View.VISIBLE;

        Log.e(TAG, "onCreate");

        // 권한체크
        settingPermission()

        // 웹뷰 처리 클래스 정의
        androidBridgeFunction = AndroidBridgeFunction(webView, this, handler)

        // 웹뷰 네이티브 콜 리스너 정의
        androidBridgeFunction.onCallWebviewPageStartListener = this
        androidBridgeFunction.onCallWebviewPageFinishListener = this

        // ActivityResult 클래스 정의
        androidActivityResult = AndroidActivityResult(this, androidBridgeFunction)
    }

    // 로딩바 보임처리
    private fun showLoadingProgress() {
        asyncDialog = ProgressDialog(this, R.style.MyTheme)
        asyncDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        asyncDialog!!.show()
    }

    // 로딩바 숨김처리
    private fun hideLoadingPrgress() {
        asyncDialog!!.dismiss()
    }

    /**
     * @param scheme
     * @return 해당 scheme에 대해 처리를 직접 하는지 여부
     *
     * 결제를 위한 3rd-party 앱이 아직 설치되어있지 않아 ActivityNotFoundException이 발생하는 경우 처리합니다.
     * 여기서 handler되지않은 scheme에 대해서는 intent로부터 Package정보 추출이 가능하다면 다음에서 packageName으로 market이동합니다.
     */
    protected fun handleNotFoundPaymentScheme(scheme: String?): Boolean {
        //PG사에서 호출하는 url에 package정보가 없어 ActivityNotFoundException이 난 후 market 실행이 안되는 경우
        if (sv.ISP.equals(scheme, ignoreCase = true)) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + sv.PACKAGE_ISP)
                )
            )
            return true
        } else if (sv.BANKPAY.equals(scheme, ignoreCase = true)) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + sv.PACKAGE_BANKPAY)
                )
            )
            return true
        }
        return false
    }

    /**
     * 권한체크
     */
    private fun settingPermission(){
        var permis = object  : PermissionListener{
            // 권한 허용
            override fun onPermissionGranted() {
            }

            // 권한 거부
            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {}
        }

        TedPermission.with(this)
            .setPermissionListener(permis)
            .setPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }

    /**
     * 웹뷰 초기화
     */
    @SuppressLint("JavascriptInterface")
    private fun initWebView() {
        extWebViewClient = ExtWebViewClient(this, URL!!, 500000, androidBridgeFunction)
//        extWebViewClient = WebViewClient()
        webView.webViewClient = extWebViewClient
        webChromeViewClient = ExtWebChromeViewClient(this, webView)
        webView.webChromeClient = webChromeViewClient

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.domStorageEnabled = true
        webSettings.setSupportMultipleWindows(true)

        if (Build.VERSION.SDK_INT >= 16) {
            webSettings.allowFileAccessFromFileURLs = true
            webSettings.allowUniversalAccessFromFileURLs = true
        }

        val TwoAppSettingString = webSettings.userAgentString + " TwoApp"
        webSettings.userAgentString = TwoAppSettingString

        //웹뷰 디버깅
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        URL = sv.SERVICE_URL
        webView.addJavascriptInterface(androidBridgeFunction, "TWOAPP")
        webView.loadUrl(URL)
    }

    /**
     * 웹뷰 로딩 시작
     */
    override fun onCallWebviewPageStart() {
        showLoadingProgress()
        Log.e(TAG, "onCallWebviewPageStart : ${webView.getUrl()}")
    }

    /**
     * 웹뷰 로딩 완료
     */
    override fun onCallWebviewPageFinish() {
        Log.e(TAG, "onCallWebviewPageFinish : ${webView.getUrl()}")
        webviewPageFinish = true;
        hideLoadingPrgress()
        splashView.visibility = View.GONE;
        webView.visibility = View.VISIBLE;
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Log.e(TAG, "onActivityResult :: requestCode : ${requestCode} / resultCode : ${resultCode}")

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                //웹뷰에서 파일 선택시
                ExtWebChromeViewClient.FILECHOOSER_RESULTCODE ->
                    webChromeViewClient.cropImage(intent!!)

                //웹뷰에서 첨부파일 선택시
                ExtWebChromeViewClient.INPUT_FILE_REQUEST_CODE ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (webChromeViewClient.mFilePathCallback == null) {
                            super.onActivityResult(requestCode, resultCode, intent)
                            return
                        }

                        val results = arrayOf<Uri>(pathUtilManager.getResultUri(intent, ExtWebChromeViewClient.mCameraPhotoPath)!!)
                        webChromeViewClient.mFilePathCallback!!.onReceiveValue(results)

                    } else {
                        if (webChromeViewClient?.mUploadMessage == null) {
                            super.onActivityResult(requestCode, resultCode, intent)
                            return
                        }

                        val result = pathUtilManager.getResultUri(intent, ExtWebChromeViewClient.mCameraPhotoPath)

                        webChromeViewClient.mUploadMessage!!.onReceiveValue(result)
                        webChromeViewClient.mUploadMessage = null
                    }

                //갤러리 선택시
                AndroidAction.GALLERY_CODE ->
                    try {
                        Log.e(TAG,"GALLERY");
                        //이미지 데이터를 비트맵으로 받아온다.
                        var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, intent?.data)
                        //bitmap이미지를 String형태로 변환한다
                        val outputStream = ByteArrayOutputStream()
                        var width = bitmap.width
                        var height = bitmap.height
                        if (width > 1000) {
                            width = 1000
                            height = height * 1000 / width
                            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                            bitmap.compress(Bitmap.CompressFormat.PNG, 70, outputStream)
                        } else {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                        val bitmapToBase64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

                        // 웹뷰 콜백 처리
                        androidBridgeFunction.callbackGallery(bitmapToBase64String)

                    } catch (e: Exception) {
                        e.printStackTrace()

                        // 스크립트 콜백 - 에러 처리
                        androidBridgeFunction.callbackCommonError(e.message.toString())
                    }

            }

            return

        }

        super.onActivityResult(requestCode, resultCode, intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            ExtWebChromeViewClient.PERMISSIONS_REQUEST_CODE -> {

                val permissionResults = HashMap<String, Int>()
                var deniedCount = 0

                for (i in grantResults.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        permissionResults[permissions[i]] = grantResults[i]
                        deniedCount++
                    }
                }

                if (deniedCount == 0) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.addCategory("android.intent.category.OPENABLE")
                    intent.type = "image/*"
                    startActivityForResult(Intent.createChooser(intent, "사진 추가"), ExtWebChromeViewClient.INPUT_FILE_REQUEST_CODE)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        webChromeViewClient.mFilePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(RESULT_CANCELED, Intent(Intent.ACTION_GET_CONTENT)))
                    }

                    try {
                        webView.loadUrl("javascript:alert(\"권한 요청 실패\", \"권한을 승인하지 않으면 사진 관련 작업을 할 수 없습니다.\");")
                    } catch (e: Exception) {
                    }

                }
            }
        }
    }

    private fun goPageMove() {
        Log.e(TAG, "goPageMove ::" + intent.data)
        val data = intent.data
        if (data != null) {
            Log.e(TAG, "getQueryParameter PageURL : " + data.getQueryParameter("qrboardIdx"))
            Log.e(TAG, "getQueryParameter PageURL : " + data.getQueryParameter("qrboardAreaSeq"))
            spUtilManager.setValue(sv.PREF_KEY_SCHEME_URL, "/user/advert/create?qrboardIdx="+data.getQueryParameter("qrboardIdx")+"&qrboardAreaSeq="+data.getQueryParameter("qrboardAreaSeq"))
        }
        initWebView()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.e(TAG, "onNewIntent")
//        setIntent(intent)
//        val data = intent.data
//        if (data != null) {
//            Log.e(TAG, "getQueryParameter PageURL : " + data.getQueryParameter("qrboardIdx"))
//            Log.e(TAG, "getQueryParameter PageURL : " + data.getQueryParameter("qrboardAreaSeq"))
//            spUtilManager.setValue(sv.PREF_KEY_SCHEME_URL, "/user/advert/create?qrboardIdx="+data.getQueryParameter("qrboardIdx")+"&qrboardAreaSeq="+data.getQueryParameter("qrboardAreaSeq"))
//        }
    }

    override fun onResume() {
        super.onResume()
        // 웹뷰 로드가 끝났을때만 데이터 전달
//        if (webviewPageFinish) {
            goPageMove()
//        }
    }

}
