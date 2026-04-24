package phaser.spider;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient; // Tambahan import

import androidx.annotation.RequiresApi;
import androidx.webkit.WebViewAssetLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private static ValueCallback<Uri> mUploadMessage;
    private static ValueCallback<Uri[]> mUploadMessage5;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private final static int PERMISSION_REQUEST = 123;
    private WebViewAssetLoader assetLoader;
    private WebView webView;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        hideNavigationBar();

        assetLoader = new WebViewAssetLoader.Builder().addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();

        webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setJavaScriptEnabled(true); // Penting untuk Google Script
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDomStorageEnabled(true); // Penting untuk Google Script
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setTextZoom(100);

        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
        webView.setDownloadListener(new DownloadListener(){
            @Override public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                webView.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url));
            }
        });
        
        // Background putih agar cocok dengan tampilan web pada umumnya
        webView.setBackgroundColor(Color.WHITE);

        webView.setWebViewClient(new WebViewClient() { // Menggunakan WebViewClient standar agar URL eksternal terbuka
            @Override @RequiresApi(21) public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override @SuppressWarnings("deprecation") public WebResourceResponse shouldInterceptRequest(WebView view, String request) {
                return assetLoader.shouldInterceptRequest(Uri.parse(request));
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // Setup untuk upload file di webview
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg, FileChooserParams fileChooserParams) {
                mUploadMessage5 = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        // MASUKKAN URL GOOGLE SCRIPT DI SINI
        webView.loadUrl("https://google.com");

        if (Build.VERSION.SDK_INT>=23) {
            try { checkAppPermissions(); } catch (Exception e) {}
        }
    }

    // Fungsi pembantu untuk Fullscreen
    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override public void onResume() {
        super.onResume();
        hideNavigationBar();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode==FILECHOOSER_RESULTCODE) {
            if (mUploadMessage5 == null) return;
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            if (result != null) {
                mUploadMessage5.onReceiveValue(new Uri[]{result});
            } else {
                mUploadMessage5.onReceiveValue(null);
            }
            mUploadMessage5 = null;
        }
    }

    // Pastikan metode checkAppPermissions ada di class ini atau sesuaikan jika menggunakan library lain
    private void checkAppPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
            }
        }
    }
}
