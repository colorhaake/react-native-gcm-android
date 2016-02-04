package com.oney.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.Resources;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import java.lang.System;
import java.util.List;

import com.google.android.gms.gcm.GcmListenerService;

public class MYGcmListenerService extends GcmListenerService {

  private static final String TAG = "MYGcmListenerService";

  @Override
  public void onMessageReceived(String from, Bundle bundle) {
    sendNotification(bundle);
  }

  public Class getMainActivityClass() {
    try {
      String packageName = getApplication().getPackageName();
      return Class.forName(packageName + ".MainActivity");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }

  private String getApplicationName() {
    final PackageManager pm = getApplicationContext().getPackageManager();

    ApplicationInfo ai;
    try {
      ai = pm.getApplicationInfo( this.getPackageName(), 0);
    } catch (final NameNotFoundException e) {
      ai = null;
    }

    return (String) (ai != null ? pm.getApplicationLabel(ai) : "");
  }

  private boolean applicationIsRunning() {
    ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
    List<RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
    for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
      if (processInfo.processName.equals(getApplication().getPackageName())) {
        if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
          for (String d: processInfo.pkgList) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void sendNotification(Bundle bundle) {
    Resources resources = getApplication().getResources();
    String packageName = getApplication().getPackageName();

    Class intentClass = getMainActivityClass();
    if (intentClass == null) {
      return;
    }

    if (applicationIsRunning()) {
      Intent i = new Intent("RNGCMReceiveNotification");
      i.putExtra("bundle", bundle);
      sendBroadcast(i);
      return;
    }

    int resourceId = resources.getIdentifier("ic_launcher", "mipmap", packageName);

    Intent intent = new Intent(this, intentClass);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.putExtra("bundle", bundle);
    int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, uniqueInt, intent,
      PendingIntent.FLAG_UPDATE_CURRENT);

    Bitmap largeIcon = BitmapFactory.decodeResource(resources, resourceId);

    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    Notification.Builder notificationBuilder = new Notification.Builder(this)
      .setLargeIcon(largeIcon)
      .setSmallIcon(resourceId)
      .setContentTitle(getApplicationName())
      .setContentText(bundle.getString("notificationMessage"))
      .setAutoCancel(true)
      .setSound(defaultSoundUri)
      .setPriority(Notification.PRIORITY_HIGH)
      .setContentIntent(pendingIntent);

    NotificationManager notificationManager =
      (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    Notification notif = notificationBuilder.build();
    notif.defaults |= Notification.DEFAULT_VIBRATE;
    notif.defaults |= Notification.DEFAULT_SOUND;
    notif.defaults |= Notification.DEFAULT_LIGHTS;

    notificationManager.notify(uniqueInt, notif);
  }
}
