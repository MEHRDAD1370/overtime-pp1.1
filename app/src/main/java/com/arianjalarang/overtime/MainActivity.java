package com.arianjalarang.overtime;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.print.PrintManager;
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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends Activity {
  private static final int REQUEST_SAVE_FILE = 501;
  private static final int REQUEST_OPEN_FILE = 502;
  private static final int REQUEST_POST_NOTIFICATIONS = 503;
  private static final String REMINDER_CHANNEL = "overtime_reminders";
  private WebView web;
  private ValueCallback<Uri[]> fileChooserCallback;
  private String pendingFileName = "پشتیبان-اضافه-کاری.json";
  private String pendingFileContent = "";

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    createReminderChannel();
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    // Required for the offline HTML app to fetch the official calendar over HTTPS.
    s.setAllowUniversalAccessFromFileURLs(true);
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

  private void createReminderChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(REMINDER_CHANNEL, "یادآورهای پنل اضافه‌کاری", NotificationManager.IMPORTANCE_HIGH);
      channel.setDescription("اعلان یادآورهای زمان‌بندی‌شده");
      channel.enableVibration(true);
      ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    }
  }

  private PendingIntent reminderPendingIntent(String id, String title, String body, int flags) {
    Intent intent = new Intent(this, ReminderReceiver.class);
    intent.setAction("com.arianjalarang.overtime.REMINDER." + id);
    intent.putExtra("reminder_id", id); intent.putExtra("reminder_title", title); intent.putExtra("reminder_body", body);
    int requestCode = id == null ? 0 : id.hashCode();
    return PendingIntent.getBroadcast(this, requestCode, intent, flags | PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
  }

  private void scheduleNativeReminder(String id, String title, long atMillis) {
    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQUEST_POST_NOTIFICATIONS);
    }
    if (atMillis <= System.currentTimeMillis()) return;
    AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
    PendingIntent pi = reminderPendingIntent(id, "یادآور اضافه‌کاری", title, 0);
    if (Build.VERSION.SDK_INT >= 23) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
    else manager.set(AlarmManager.RTC_WAKEUP, atMillis, pi);
  }

  private void cancelNativeReminder(String id) {
    AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
    if (manager != null) manager.cancel(reminderPendingIntent(id, "یادآور اضافه‌کاری", "", 0));
  }

  public static class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
      String id = intent.getStringExtra("reminder_id");
      String body = intent.getStringExtra("reminder_body");
      NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel channel = new NotificationChannel("overtime_reminders", "یادآورهای پنل اضافه‌کاری", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("اعلان یادآورهای زمان‌بندی‌شده"); channel.enableVibration(true); manager.createNotificationChannel(channel);
      }
      if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) return;
      Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, "overtime_reminders") : new Notification.Builder(context);
      builder.setSmallIcon(context.getApplicationInfo().icon).setContentTitle("یادآور اضافه‌کاری").setContentText(body == null ? "زمان یادآوری فرا رسیده است" : body).setStyle(new Notification.BigTextStyle().bigText(body)).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).setDefaults(Notification.DEFAULT_ALL);
      manager.notify(id == null ? (int)System.currentTimeMillis() : id.hashCode(), builder.build());
    }
  }

  private class AndroidBridge {
    @JavascriptInterface public void updateOvertimeWidget(String overtime, String amount, String accent, String background) {
      try {
        android.content.SharedPreferences prefs = getSharedPreferences("overtime_widget", MODE_PRIVATE);
        prefs.edit().putString("overtime", overtime == null ? "۰:۰۰" : overtime)
          .putString("amount", amount == null ? "۰ ریال" : amount)
          .putString("accent", accent == null ? "#39d0b4" : accent)
          .putString("background", background == null ? "#101c2f" : background).apply();
        Intent update = new Intent(MainActivity.this, OvertimeWidgetProvider.class);
        update.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = android.appwidget.AppWidgetManager.getInstance(MainActivity.this).getAppWidgetIds(new android.content.ComponentName(MainActivity.this, OvertimeWidgetProvider.class));
        update.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(update);
      } catch (Exception ignored) {}
    }
    @JavascriptInterface public void scheduleReminder(String id, String text, String timestamp) {
      try { long at = Long.parseLong(timestamp); runOnUiThread(() -> scheduleNativeReminder(id, text, at)); } catch (Exception ignored) {}
    }
    @JavascriptInterface public void cancelReminder(String id) { runOnUiThread(() -> cancelNativeReminder(id)); }
    @JavascriptInterface public void requestNotificationPermission() {
      runOnUiThread(() -> {
        createReminderChannel();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQUEST_POST_NOTIFICATIONS);
        else if (web != null) web.evaluateJavascript("toast('اعلان‌های گوشی فعال هستند')", null);
      });
    }
    @JavascriptInterface public void requestHolidayYear(String year, String requestId) {
      // Fetch one JSON file per Jalali year from GitHub, then return it to the WebView.
      new Thread(() -> {
        HttpURLConnection connection = null;
        boolean ok = false;
        String body = "";
        try {
          if (year == null || !year.matches("\\d{4}")) throw new IllegalArgumentException("سال شمسی نامعتبر است");
          URL url = new URL(("1405".equals(year)) ? "https://raw.githubusercontent.com/rzpoker/Iran-Calendar-1405/master/IranCalendar1405.json" : "https://raw.githubusercontent.com/iyazdanicharati/IranHollidaysJSON/main/" + year + ".json");
          connection = (HttpURLConnection) url.openConnection();
          connection.setRequestMethod("GET");
          connection.setConnectTimeout(15000);
          connection.setReadTimeout(20000);
          connection.setRequestProperty("Accept", "application/json");
          connection.setRequestProperty("User-Agent", "OvertimeAndroid/2.4");
          int status = connection.getResponseCode();
          InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
          if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
              StringBuilder response = new StringBuilder(); String line;
              while ((line = reader.readLine()) != null) response.append(line);
              body = response.toString();
            }
          }
          ok = status >= 200 && status < 300;
          if (!ok && body.isEmpty()) body = "GitHub JSON HTTP " + status;
        } catch (Exception e) {
          body = e.getMessage() == null ? "خطای شبکه در دریافت فایل تعطیلات از GitHub" : e.getMessage();
        } finally { if (connection != null) connection.disconnect(); }
        final boolean resultOk = ok; final String resultBody = body; final String resultId = requestId == null ? "" : requestId;
        runOnUiThread(() -> { if (web != null) web.evaluateJavascript("window.__holidayApiNativeResult(" + JSONObject.quote(resultId) + "," + resultOk + "," + JSONObject.quote(resultBody) + ")", null); });
      }).start();
    }
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
