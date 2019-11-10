/** Copyright (c) 2019 The Huhi Authors. All rights reserved.
  * This Source Code Form is subject to the terms of the Mozilla Public
  * License, v. 2.0. If a copy of the MPL was not distributed with this file,
  * You can obtain one at http://mozilla.org/MPL/2.0/.
  */

package org.chromium.chrome.browser.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Locale;
import android.net.Uri;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.notifications.HuhiAdsNotificationBuilder;
import org.chromium.chrome.browser.notifications.ChromeNotification;
import org.chromium.chrome.browser.notifications.NotificationBuilderBase;
import org.chromium.chrome.browser.notifications.NotificationManagerProxyImpl;
import org.chromium.chrome.browser.notifications.NotificationMetadata;
import org.chromium.chrome.browser.notifications.NotificationUmaTracker;
import org.chromium.chrome.browser.onboarding.OnboardingPrefManager;

public class HuhiOnboardingNotification extends BroadcastReceiver {
    public Context mContext;
    private Intent mIntent;

    private static final int HUHI_ONBOARDING_NOTIFICATION_ID = -2;
    private static String HUHI_ONBOARDING_NOTIFICATION_TAG = "huhi_onboarding_notification_tag";
    private static String HUHI_ONBOARDING_ORIGIN_EN = "https://huhisoft.com/my-first-ad/";
    private static String HUHI_ONBOARDING_ORIGIN_DE = "https://huhisoft.com/de/my-first-ad/";
    private static String HUHI_ONBOARDING_ORIGIN_FR = "https://huhisoft.com/fr/my-first-ad/";
    private static final String DEEP_LINK = "deep_link";

    private static final String COUNTRY_CODE_DE = "de_DE";
    private static final String COUNTRY_CODE_FR = "fr_FR";


    public static void showOnboardingNotification(Context context) {
        NotificationManagerProxyImpl notificationManager = new NotificationManagerProxyImpl(context);

        NotificationBuilderBase notificationBuilder =
          new HuhiAdsNotificationBuilder(context)
              .setTitle(context.getString(R.string.huhi_ui_huhi_rewards))
              .setBody(context.getString(R.string.this_is_your_first_ad))
              .setSmallIconId(R.drawable.ic_chrome)
              .setPriority(Notification.PRIORITY_HIGH)
              .setDefaults(Notification.DEFAULT_ALL)
              .setContentIntent(getDeepLinkIntent(context))
              .setOrigin(getNotificationUrl());

        ChromeNotification notification = notificationBuilder.build(
            new NotificationMetadata(
                NotificationUmaTracker.SystemNotificationType.UNKNOWN /* Underlying code doesn't track UNKNOWN */,
                HUHI_ONBOARDING_NOTIFICATION_TAG /* notificationTag */,
                HUHI_ONBOARDING_NOTIFICATION_ID /* notificationId */
            )
        );
        notificationManager.notify(notification);
    }

    private static PendingIntentProvider getDeepLinkIntent(Context context) {
        Intent intent = new Intent(context, HuhiOnboardingNotification.class);
        intent.setAction(DEEP_LINK);
        return new PendingIntentProvider(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(DEEP_LINK)) {
            OnboardingPrefManager.getInstance().setPrefOnboardingEnabled(false);

            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getNotificationUrl()));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            webIntent.setPackage(context.getPackageName());
            context.startActivity(webIntent);
        } else {
            showOnboardingNotification(context);
        }
    }

    private static String getNotificationUrl() {
      
      Locale locale = Locale.getDefault();
      switch (locale.toString()){
          case COUNTRY_CODE_DE :
            return HUHI_ONBOARDING_ORIGIN_DE;
          case COUNTRY_CODE_FR :
            return HUHI_ONBOARDING_ORIGIN_FR;
          default :
            return HUHI_ONBOARDING_ORIGIN_EN;
      }
    }

    public static void cancelOnboardingNotification(Context context) {
        NotificationManagerProxyImpl notificationManager = new NotificationManagerProxyImpl(context);
        notificationManager.cancel(HUHI_ONBOARDING_NOTIFICATION_TAG, HUHI_ONBOARDING_NOTIFICATION_ID);
    }
}
