package com.arianjalarang.overtime;

import android.app.Activity;
import android.app.PrintManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Context;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.database.Cursor;
import android.os.Build;
import java.io.OutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
  private static final int REQUEST_SAVE_FILE = 501;
  private static final int REQUEST_OPEN_FILE = 502;
  private WebView web;
  private ValueCallback<Uri[]> fileChooserCallback;
  private String pendingFileName = "پشتیبان-اضافه-کاری.json";
  private String pendingFileContent = "";

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    s.setSupportZoom(false);
    web.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        String u = r.getUrl().toString();
        if (u.startsWith("http://") || u.startsWith("https://")) {
          try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ignored) {}
          return true;
        }
        return false;
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
        fileChooserCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] accepts = params.getAcceptTypes();
        if (accepts != null) {
          for (String type : accepts) {
            if (type != null && type.startsWith("image/")) { intent.setType("image/*"); break; }
            if (type != null && (type.contains("json") || type.contains(".json"))) { intent.setType("application/json"); break; }
          }
        }
        try { startActivityForResult(intent, REQUEST_OPEN_FILE); }
        catch (Exception e) { fileChooserCallback = null; callback.onReceiveValue(null); return false; }
        return true;
      }
    });
    web.loadUrl("file:///android_asset/index.html");
  }

  private class AndroidBridge {
    @JavascriptInterface public void saveTextFile(String filename, String content) {
      runOnUiThread(() -> {
        pendingFileName = (filename == null || filename.trim().isEmpty()) ? "پشتیبان-اضافه-کاری.json" : filename;
        if (!pendingFileName.toLowerCase().endsWith(".json")) pendingFileName += ".json";
        pendingFileContent = content == null ? "{}" : content;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, pendingFileName);
        try { startActivityForResult(intent, REQUEST_SAVE_FILE); }
        catch (Exception e) { if (web != null) web.evaluateJavascript("toast('ذخیره فایل انجام نشد؛ دوباره تلاش کنید')", null); }
      });
    }
    @JavascriptInterface public void printPage() {
      runOnUiThread(() -> {
        try {
          PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
          PrintDocumentAdapter adapter = web.createPrintDocumentAdapter("گزارش اضافه‌کاری");
          pm.print("گزارش اضافه‌کاری", adapter, new PrintAttributes.Builder().build());
        } catch (Exception e) {
          if (web != null) web.evaluateJavascript("toast('امکان چاپ در این دستگاه فراهم نشد')", null);
        }
      });
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_SAVE_FILE) {
      if (resultCode == RESULT_OK && data != null && data.getData() != null) {
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
          if (out != null) { out.write(pendingFileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)); out.flush(); }
          if (web != null) web.evaluateJavascript("toast('فایل با موفقیت ذخیره شد ✓')", null);
        } catch (IOException e) { if (web != null) web.evaluateJavascript("toast('خطا در ذخیره فایل')", null); }
      }
      return;
    }
    if (requestCode == REQUEST_OPEN_FILE && fileChooserCallback != null) {
      Uri[] result = null;
      if (resultCode == RESULT_OK && data != null && data.getData() != null) result = new Uri[]{data.getData()};
      fileChooserCallback.onReceiveValue(result);
      fileChooserCallback = null;
    }
  }

  @Override public void onBackPressed() {
    if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
  }
}
