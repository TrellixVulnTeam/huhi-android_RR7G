/** Copyright (c) 2019 The Huhi Authors. All rights reserved.
  * This Source Code Form is subject to the terms of the Mozilla Public
  * License, v. 2.0. If a copy of the MPL was not distributed with this file,
  * You can obtain one at http://mozilla.org/MPL/2.0/.
  */

package org.chromium.chrome.browser.dialogs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.widget.ImageView;
import android.view.View;
import java.lang.System;

import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.HuhiAdsNativeHelper;
import org.chromium.chrome.browser.HuhiRewardsNativeWorker;
import org.chromium.chrome.browser.HuhiRewardsPanelPopup;
import org.chromium.chrome.browser.ChromeFeatureList;
import org.chromium.chrome.browser.notifications.HuhiOnboardingNotification;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.util.PackageUtils;
import org.chromium.chrome.browser.onboarding.OnboardingPrefManager;
import org.chromium.chrome.browser.preferences.HuhiRewardsPreferences;

public class HuhiAdsSignupDialog {

    private static String SHOULD_SHOW_ONBOARDING_DIALOG_VIEW_COUNTER = "should_show_onboarding_dialog_view_counter";
    private static String SHOULD_SHOW_ONBOARDING_DIALOG = "should_show_onboarding_dialog";

    private static final long TWENTY_FOUR_HOURS = 86_400_000;
    private static final long MOMENT_LATER = 2_500;

    public static boolean shouldShowNewUserDialog(Context context) {
        boolean shouldShow =
          shouldShowOnboardingDialog()
          && PackageUtils.isFirstInstall(context)
          && !HuhiAdsNativeHelper.nativeIsHuhiAdsEnabled(Profile.getLastUsedProfile())
          && !HuhiRewardsPanelPopup.isHuhiRewardsEnabled()
          && hasElapsed24Hours(context)
          && ChromeFeatureList.isEnabled(ChromeFeatureList.HUHI_REWARDS);

        boolean shouldShowForViewCount = shouldShowForViewCount();
        if (shouldShow) updateViewCount();

        return shouldShow && shouldShowForViewCount;
    }

    public static boolean shouldShowNewUserDialogIfRewardsIsSwitchedOff(Context context) {
        boolean shouldShow =
          shouldShowOnboardingDialog()
          && !PackageUtils.isFirstInstall(context)
          && !HuhiAdsNativeHelper.nativeIsHuhiAdsEnabled(Profile.getLastUsedProfile())
          && !HuhiRewardsPanelPopup.isHuhiRewardsEnabled()
          && ChromeFeatureList.isEnabled(ChromeFeatureList.HUHI_REWARDS);

        boolean shouldShowForViewCount = shouldShowForViewCount();
        if (shouldShow) updateViewCount();

        return shouldShow && shouldShowForViewCount;
    }

    public static boolean shouldShowExistingUserDialog(Context context) {
        boolean shouldShow =
          shouldShowOnboardingDialog()
          && !PackageUtils.isFirstInstall(context)
          && !HuhiAdsNativeHelper.nativeIsHuhiAdsEnabled(Profile.getLastUsedProfile())
          && HuhiRewardsPanelPopup.isHuhiRewardsEnabled()
          && HuhiAdsNativeHelper.nativeIsLocaleValid(Profile.getLastUsedProfile())
          && ChromeFeatureList.isEnabled(ChromeFeatureList.HUHI_REWARDS);

        boolean shouldShowForViewCount = shouldShowForViewCount();
        if (shouldShow) updateViewCount();

        return shouldShow && shouldShowForViewCount;
    }

    @CalledByNative
    public static void enqueueOobeNotificationNative() {
        enqueueOobeNotification(ContextUtils.getApplicationContext());
    }

    @CalledByNative
    public static boolean showAdsInBackground() {
        return HuhiRewardsPreferences.getPrefAdsInBackgroundEnabled();
    }

    private static void enqueueOobeNotification(Context context) {
        if(!OnboardingPrefManager.getInstance().isOnboardingNotificationShown()) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, HuhiOnboardingNotification.class);
            am.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + MOMENT_LATER,
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            );
        }
    }

    public static void showNewUserDialog(Context context) {
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.HuhiDialogTheme)
        .setView(R.layout.huhi_ads_new_user_dialog_layout)
        .setPositiveButton(R.string.huhi_ads_offer_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                OnboardingPrefManager.getInstance().setOnboardingNotificationShown(false);

                neverShowOnboardingDialogAgain();

                HuhiRewardsNativeWorker huhiRewardsNativeWorker = HuhiRewardsNativeWorker.getInstance();
                huhiRewardsNativeWorker.GetRewardsMainEnabled();
                huhiRewardsNativeWorker.CreateWallet();

                // Enable ads
                HuhiAdsNativeHelper.nativeSetAdsEnabled(Profile.getLastUsedProfile());
            }
        }).create();
        alertDialog.show();

        ImageView closeButton = alertDialog.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
    }

    public static void showExistingUserDialog(Context context) {
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.HuhiDialogTheme)
        .setView(R.layout.huhi_ads_existing_user_dialog_layout)
        .setPositiveButton(R.string.huhi_ads_offer_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Enable ads
                neverShowOnboardingDialogAgain();

                HuhiAdsNativeHelper.nativeSetAdsEnabled(Profile.getLastUsedProfile());
            }
        }).create();
        alertDialog.show();

        ImageView closeButton = alertDialog.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
    }

    private static boolean hasElapsed24Hours(Context context) {
        boolean result = false;
        try {
            result = System.currentTimeMillis() >= context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime + TWENTY_FOUR_HOURS;
        } catch (NameNotFoundException e) {}
        return result;
    }

    private static boolean shouldShowForViewCount() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();

        int viewCount = sharedPref.getInt(SHOULD_SHOW_ONBOARDING_DIALOG_VIEW_COUNTER, 0);
        return 0 == viewCount || 20 == viewCount || 40 == viewCount;
    }

    private static void updateViewCount() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt(SHOULD_SHOW_ONBOARDING_DIALOG_VIEW_COUNTER, sharedPref.getInt(SHOULD_SHOW_ONBOARDING_DIALOG_VIEW_COUNTER, 0) + 1);
        editor.apply();
    }

    private static void neverShowOnboardingDialogAgain() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putBoolean(SHOULD_SHOW_ONBOARDING_DIALOG, false);
        editor.apply();
    }

    private static boolean shouldShowOnboardingDialog() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
 
        return sharedPref.getBoolean(SHOULD_SHOW_ONBOARDING_DIALOG, true);
    }
}
