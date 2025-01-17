// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.incognito;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import org.chromium.base.ContextUtils;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.notifications.ChromeNotification;
import org.chromium.chrome.browser.notifications.ChromeNotificationBuilder;
import org.chromium.chrome.browser.notifications.NotificationBuilderFactory;
import org.chromium.chrome.browser.notifications.NotificationConstants;
import org.chromium.chrome.browser.notifications.NotificationManagerProxy;
import org.chromium.chrome.browser.notifications.NotificationManagerProxyImpl;
import org.chromium.chrome.browser.notifications.NotificationMetadata;
import org.chromium.chrome.browser.notifications.NotificationUmaTracker;
import org.chromium.chrome.browser.notifications.channels.ChannelDefinitions;

/**
 * Manages the notification indicating that there are incognito tabs opened in Document mode.
 */
public class IncognitoNotificationManager {
    private static final String INCOGNITO_TABS_OPEN_TAG = "incognito_tabs_open";
    private static final int INCOGNITO_TABS_OPEN_ID = 100;

    /**
     * Shows the close all incognito notification.
     */
    public static void showIncognitoNotification() {
        Context context = ContextUtils.getApplicationContext();
        String actionMessage =
                context.getResources().getString(R.string.huhi_close_all_private_tabs);
        String title = context.getResources().getString(R.string.app_name);

        ChromeNotificationBuilder builder =
                NotificationBuilderFactory
                        .createChromeNotificationBuilder(true /* preferCompat */,
                                ChannelDefinitions.ChannelId.INCOGNITO,
                                null /* remoteAppPackageName */,
                                new NotificationMetadata(
                                        NotificationUmaTracker.SystemNotificationType
                                                .CLOSE_INCOGNITO,
                                        INCOGNITO_TABS_OPEN_TAG, INCOGNITO_TABS_OPEN_ID))
                        .setContentTitle(title)
                        .setContentIntent(
                                IncognitoNotificationService.getRemoveAllIncognitoTabsIntent(
                                        context))
                        .setContentText(actionMessage)
                        .setOngoing(true)
                        .setVisibility(Notification.VISIBILITY_SECRET)
                        .setSmallIcon(R.drawable.incognito_simple)
                        .setShowWhen(false)
                        .setLocalOnly(true)
                        .setGroup(NotificationConstants.GROUP_INCOGNITO);
        NotificationManagerProxy nm = new NotificationManagerProxyImpl(context);
        ChromeNotification notification = builder.buildChromeNotification();
        nm.notify(notification);
        NotificationUmaTracker.getInstance().onNotificationShown(
                NotificationUmaTracker.SystemNotificationType.CLOSE_INCOGNITO,
                notification.getNotification());
    }

    /**
     * Dismisses the incognito notification.
     */
    public static void dismissIncognitoNotification() {
        Context context = ContextUtils.getApplicationContext();
        NotificationManager nm =
                  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(INCOGNITO_TABS_OPEN_TAG, INCOGNITO_TABS_OPEN_ID);
    }
}