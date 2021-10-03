//Blob download implementation
package com.dktechhub.mnnit.ee.whatsweb;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.MissingResourceException;

public class MainFragment extends AppCompatActivity {


    //ProgressBar pbar;
    WebView webView;
    boolean requestDesktopSite;
    String url;
    ProgressBar progressBar;
    ActionBar actionBar;
    protected static final int REQUEST_CODE_FILE_PICKER = 51426;
    protected static final String LANGUAGE_DEFAULT_ISO3 = "eng";
    protected static final String CHARSET_DEFAULT = "UTF-8";
    //protected Listener mListener;
   // protected final List<String> mPermittedHostnames = new LinkedList<String>();
    /** File upload callback for platform versions prior to Android 5.0 */
    protected ValueCallback<Uri> mFileUploadCallbackFirst;
    /** File upload callback for Android 5.0+ */
    protected ValueCallback<Uri[]> mFileUploadCallbackSecond;
    protected String mLanguageIso3;
    protected int mRequestCodeFilePicker = REQUEST_CODE_FILE_PICKER;
    protected String mUploadableFileTypes = "*/*";
    private MyApplication myApplication;
    //protected final Map<String, String> mHttpHeaders = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_main);
        myApplication=(MyApplication)this.getApplication();
        myApplication.showInterstitial(this);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            //actionBar.hide();
        }

        ConnectionDetector.checkInternet(this);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M&&checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},WRITE_EXTERNAL_STORAGE_CODE);
        }


       requestDesktopSite= getIntent().getBooleanExtra("requestDesktopSite",false);
        url=getIntent().getStringExtra("url");

        this.webView = findViewById(R.id.webView);

        progressBar=findViewById(R.id.progressBar);
        InitSettings();
        webView.loadUrl(url);



    }




    long time = 0;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - time < 1000) {
            finish();
        } else {
            time = System.currentTimeMillis();
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null&&!actionBar.isShowing()) {
                actionBar.show();
            }
            else if (webView.canGoBack())
                webView.goBack();

            Toast.makeText(this, getString(R.string.pressAgaintoexit), Toast.LENGTH_SHORT).show();
        }

    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.storagepermdenined), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private static final int WRITE_EXTERNAL_STORAGE_CODE = 257;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_basic,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.fullscreen)
        {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }else if(item.getItemId()==R.id.refresh)
        {
            this.webView.reload();
        }
        return true;
    }


    public class webClient extends WebViewClient {
        private final boolean requestDesktopSite;
        public webClient(boolean requestDesktopSite)
        {
            this.requestDesktopSite=requestDesktopSite;
        }
        public boolean shouldOverrideUrlLoading(WebView webView, String url)
        {
            webView.loadUrl(url);
            return true;
        }
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onLoadResource(WebView view, String url) {
            if(this.requestDesktopSite)
                view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }

    }

    public class WebChromeClient extends android.webkit.WebChromeClient
    {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if(MainFragment.this.actionBar!=null)
            {
                actionBar.setTitle(title);
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {

            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
        }


        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, null);
        }

        // file upload callback (Android 3.0 (API level 11) -- Android 4.0 (API level 15)) (hidden method)
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            openFileChooser(uploadMsg, acceptType, null);
        }

        // file upload callback (Android 4.1 (API level 16) -- Android 4.3 (API level 18)) (hidden method)
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            openFileInput(uploadMsg, null, false);
        }

        // file upload callback (Android 5.0 (API level 21) -- current) (public method)
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            if (Build.VERSION.SDK_INT >= 21) {
                final boolean allowMultiple = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;

                openFileInput(null, filePathCallback, allowMultiple);

                return true;
            }
            else {
                return false;
            }
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    public void InitSettings() {
        //this.requestDesktopSite = requestDesktopSite;
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.setWebViewClient(new webClient(requestDesktopSite));
        this.webView.getSettings().setSaveFormData(true);
        this.webView.getSettings().setLoadsImagesAutomatically(true);
        this.webView.getSettings().setAllowFileAccessFromFileURLs(true);
        if (requestDesktopSite) {

            this.webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            this.webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Win64; x64; rv:46.0) Gecko/20100101 Firefox/60.0");
            this.webView.setInitialScale(25);
            webView.getSettings().setSupportZoom(true);
            webView.getSettings().setBuiltInZoomControls(true);
        }
        this.webView.getSettings().setUseWideViewPort(true);
        this.webView.getSettings().setBlockNetworkImage(false);
        this.webView.getSettings().setBlockNetworkLoads(false);
        this.webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        this.webView.getSettings().setSupportMultipleWindows(true);
        this.webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.webView.getSettings().setLoadWithOverviewMode(true);
        this.webView.getSettings().setNeedInitialFocus(false);
        this.webView.getSettings().setAppCacheEnabled(true);
        this.webView.getSettings().setDatabaseEnabled(true);
        this.webView.getSettings().setDomStorageEnabled(true);
        this.webView.getSettings().setGeolocationEnabled(true);
        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
        this.webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        this.webView.setWebChromeClient(new WebChromeClient());
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if(url.startsWith("blob"))
                webView.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url,URLUtil.guessFileName(url,contentDisposition,mimetype),mimetype));
                else {




                    final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,contentDisposition,mimetype));
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    try {
                        try {
                            dm.enqueue(request);
                        }
                        catch (SecurityException e) {
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                            dm.enqueue(request);
                        }

                    }
                    // if the download manager app has been disabled on the device
                    catch (IllegalArgumentException e) {
                        // show the settings screen where the user can enable the download manager app again
                        Toast.makeText(MainFragment.this, getString(R.string.downloadfaild), Toast.LENGTH_SHORT).show();
                    }





                }
            }
        });
    }















    //Upload


    protected static String getLanguageIso3() {
        try {
            return Locale.getDefault().getISO3Language().toLowerCase(Locale.US);
        }
        catch (MissingResourceException e) {
            return LANGUAGE_DEFAULT_ISO3;
        }
    }

    /**
     * Provides localizations for the 25 most widely spoken languages that have a ISO 639-2/T code
     *
     * @return the label for the file upload prompts as a string
     */
    protected String getFileUploadPromptLabel() {
        try {
            if (mLanguageIso3.equals("zho")) return decodeBase64("6YCJ5oup5LiA5Liq5paH5Lu2");
            else if (mLanguageIso3.equals("spa")) return decodeBase64("RWxpamEgdW4gYXJjaGl2bw==");
            else if (mLanguageIso3.equals("hin")) return decodeBase64("4KSP4KSVIOCkq+CkvOCkvuCkh+CksiDgpJrgpYHgpKjgpYfgpII=");
            else if (mLanguageIso3.equals("ben")) return decodeBase64("4KaP4KaV4Kaf4Ka/IOCmq+CmvuCmh+CmsiDgpqjgpr/gprDgp43gpqzgpr7gpprgpqg=");
            else if (mLanguageIso3.equals("ara")) return decodeBase64("2KfYrtiq2YrYp9ixINmF2YTZgSDZiNin2K3Yrw==");
            else if (mLanguageIso3.equals("por")) return decodeBase64("RXNjb2xoYSB1bSBhcnF1aXZv");
            else if (mLanguageIso3.equals("rus")) return decodeBase64("0JLRi9Cx0LXRgNC40YLQtSDQvtC00LjQvSDRhNCw0LnQuw==");
            else if (mLanguageIso3.equals("jpn")) return decodeBase64("MeODleOCoeOCpOODq+OCkumBuOaKnuOBl+OBpuOBj+OBoOOBleOBhA==");
            else if (mLanguageIso3.equals("pan")) return decodeBase64("4KiH4Kmx4KiVIOCoq+CovuCoh+CosiDgqJrgqYHgqKPgqYs=");
            else if (mLanguageIso3.equals("deu")) return decodeBase64("V8OkaGxlIGVpbmUgRGF0ZWk=");
            else if (mLanguageIso3.equals("jav")) return decodeBase64("UGlsaWggc2lqaSBiZXJrYXM=");
            else if (mLanguageIso3.equals("msa")) return decodeBase64("UGlsaWggc2F0dSBmYWls");
            else if (mLanguageIso3.equals("tel")) return decodeBase64("4LCS4LCVIOCwq+CxhuCxluCwsuCxjeCwqOCxgSDgsI7gsILgsJrgsYHgsJXgsYvgsILgsKHgsL8=");
            else if (mLanguageIso3.equals("vie")) return decodeBase64("Q2jhu41uIG3hu5l0IHThuq1wIHRpbg==");
            else if (mLanguageIso3.equals("kor")) return decodeBase64("7ZWY64KY7J2YIO2MjOydvOydhCDshKDtg50=");
            else if (mLanguageIso3.equals("fra")) return decodeBase64("Q2hvaXNpc3NleiB1biBmaWNoaWVy");
            else if (mLanguageIso3.equals("mar")) return decodeBase64("4KSr4KS+4KSH4KSyIOCkqOCkv+CkteCkoeCkvg==");
            else if (mLanguageIso3.equals("tam")) return decodeBase64("4K6S4K6w4K+BIOCuleCvh+CuvuCuquCvjeCuquCviCDgrqTgr4fgrrDgr43grrXgr4E=");
            else if (mLanguageIso3.equals("urd")) return decodeBase64("2KfbjNqpINmB2KfYptmEINmF24zauiDYs9uSINin2YbYqtiu2KfYqCDaqdix24zaug==");
            else if (mLanguageIso3.equals("fas")) return decodeBase64("2LHYpyDYp9mG2KrYrtin2Kgg2qnZhtuM2K8g24zaqSDZgdin24zZhA==");
            else if (mLanguageIso3.equals("tur")) return decodeBase64("QmlyIGRvc3lhIHNlw6dpbg==");
            else if (mLanguageIso3.equals("ita")) return decodeBase64("U2NlZ2xpIHVuIGZpbGU=");
            else if (mLanguageIso3.equals("tha")) return decodeBase64("4LmA4Lil4Li34Lit4LiB4LmE4Lif4Lil4LmM4Lir4LiZ4Li24LmI4LiH");
            else if (mLanguageIso3.equals("guj")) return decodeBase64("4KqP4KqVIOCqq+CqvuCqh+CqsuCqqOCrhyDgqqrgqrjgqoLgqqY=");
        }
        catch (Exception ignored) { }

        // return English translation by default
        return "Choose a file";
    }

    protected static String decodeBase64(final String base64) throws IllegalArgumentException, UnsupportedEncodingException {
        final byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return new String(bytes, CHARSET_DEFAULT);
    }

    @SuppressLint("NewApi")
    protected void openFileInput(final ValueCallback<Uri> fileUploadCallbackFirst, final ValueCallback<Uri[]> fileUploadCallbackSecond, final boolean allowMultiple) {
        if (mFileUploadCallbackFirst != null) {
            mFileUploadCallbackFirst.onReceiveValue(null);
        }
        mFileUploadCallbackFirst = fileUploadCallbackFirst;

        if (mFileUploadCallbackSecond != null) {
            mFileUploadCallbackSecond.onReceiveValue(null);
        }
        mFileUploadCallbackSecond = fileUploadCallbackSecond;

        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);

        if (allowMultiple) {
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        i.setType(mUploadableFileTypes);
        //startActivityForResult(Intent.createChooser(i, getFileUploadPromptLabel()), mRequestCodeFilePicker);
        Intent shareIntent = Intent.createChooser(i, getFileUploadPromptLabel());
        startActivityForResult(shareIntent,mRequestCodeFilePicker);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == mRequestCodeFilePicker) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    if (mFileUploadCallbackFirst != null) {
                        mFileUploadCallbackFirst.onReceiveValue(intent.getData());
                        mFileUploadCallbackFirst = null;
                    } else if (mFileUploadCallbackSecond != null) {
                        Uri[] dataUris = null;

                        try {
                            if (intent.getDataString() != null) {
                                dataUris = new Uri[]{Uri.parse(intent.getDataString())};
                            } else {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();

                                    dataUris = new Uri[numSelectedFiles];

                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        dataUris[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }

                        mFileUploadCallbackSecond.onReceiveValue(dataUris);
                        mFileUploadCallbackSecond = null;
                    }
                }
            } else {
                if (mFileUploadCallbackFirst != null) {
                    mFileUploadCallbackFirst.onReceiveValue(null);
                    mFileUploadCallbackFirst = null;
                } else if (mFileUploadCallbackSecond != null) {
                    mFileUploadCallbackSecond.onReceiveValue(null);
                    mFileUploadCallbackSecond = null;
                }
            }
        }
    }




    public static class JavaScriptInterface {
        private final Context context;
        public JavaScriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void getBase64FromBlobData(String base64Data,String fileName,String mime) throws IOException {
            convertBase64StringToPdfAndStoreIt(base64Data,fileName,mime);
        }
        public static String getBase64StringFromBlobUrl(String blobUrl,String fileName,String mime) {
            if(blobUrl.startsWith("blob")){
                return "javascript: var xhr = new XMLHttpRequest();" +
                        "xhr.open('GET', '"+ blobUrl +"', true);" +
                        "xhr.setRequestHeader('Content-type','application/pdf');" +
                        "xhr.responseType = 'blob';" +
                        "xhr.onload = function(e) {" +
                        "    if (this.status == 200) {" +
                        "        var blobPdf = this.response;" +
                        "        var reader = new FileReader();" +
                        "        reader.readAsDataURL(blobPdf);" +
                        "        reader.onloadend = function() {" +
                        "            base64data = reader.result;" +
                        "            Android.getBase64FromBlobData(base64data,\""+fileName+"\",\""+mime+"\");" +
                        "        }" +
                        "    }" +
                        "};" +
                        "xhr.send();";
            }
            return "javascript: console.log('It is not a Blob URL');";
        }
        private void convertBase64StringToPdfAndStoreIt(String base64Data,String fileName,String mime) throws IOException {
            final int notificationId = 1;
            //String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
            final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/"+fileName);
           // Log.d("blob download",fileName+'\t'+mime);
           // Log.d("blob download",base64Data+"\n\n\n\n");

            String s = base64Data.substring(0, base64Data.indexOf("base64,") + 7);
           // Log.d("blob download", s);
            byte[] pdfAsBytes = Base64.decode(base64Data.replaceFirst(s, ""), 0);


            FileOutputStream os;
            os = new FileOutputStream(dwldsPath, false);
            os.write(pdfAsBytes);
            os.flush();

            if (dwldsPath.exists()) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                Uri apkURI = FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
                intent.setDataAndType(apkURI,mime);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getActivity(context,1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                String CHANNEL_ID = "MYCHANNEL";
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ID,"name", NotificationManager.IMPORTANCE_LOW);
                    Notification notification = new Notification.Builder(context,CHANNEL_ID)
                            .setContentText(fileName+" Downloaded")
                            .setContentTitle("File downloaded")
                            .setContentIntent(pendingIntent)
                            .setChannelId(CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.sym_action_chat)
                            .build();
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(notificationChannel);
                        notificationManager.notify(notificationId, notification);
                    }

                } else {
                    NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(android.R.drawable.sym_action_chat)
                            //.setContentIntent(pendingIntent)
                            .setContentText(fileName+" Downloaded")
                            .setContentTitle("File downloaded");

                    if (notificationManager != null) {
                        notificationManager.notify(notificationId, b.build());
                        Handler h = new Handler();
                        long delayInMilliseconds = 1000;
                        h.postDelayed(new Runnable() {
                            public void run() {
                                notificationManager.cancel(notificationId);
                            }
                        }, delayInMilliseconds);
                    }
                }
            }
            Toast.makeText(context, "FILE DOWNLOADED!", Toast.LENGTH_SHORT).show();
        }
    }







}
