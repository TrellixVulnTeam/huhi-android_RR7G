/* Copyright (c) 2019 The Huhi Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.preferences.HuhiPreferenceFragment;
import org.chromium.chrome.browser.HuhiRewardsNativeWorker;
import org.chromium.chrome.browser.HuhiRewardsObserver;
import org.chromium.chrome.browser.HuhiRewardsPanelPopup;
import org.chromium.chrome.browser.accessibility.FontSizePrefs;
import org.chromium.chrome.browser.accessibility.FontSizePrefs.FontSizePrefsObserver;
import org.chromium.chrome.browser.preferences.website.SingleCategoryPreferences;
import org.chromium.chrome.browser.RestartWorker;
import org.chromium.chrome.browser.util.AccessibilityUtil;

/**
 * Fragment to keep track of all Huhi Rewards related preferences.
 */
public class HuhiRewardsPreferences extends HuhiPreferenceFragment
        implements OnPreferenceChangeListener, HuhiRewardsObserver {

    static final String PREF_RESET_REWARDS = "reset_rewards";
    private static final String PREF_ADS_SWITCH = "ads_switch";

    private ChromeSwitchPreference mAdsSwitch;

    private HuhiRewardsNativeWorker mHuhiRewardsNativeWorker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.huhi_ui_huhi_rewards);
        PreferenceUtils.addPreferencesFromResource(this, R.xml.huhi_rewards_preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdsSwitch = (ChromeSwitchPreference) findPreference(PREF_ADS_SWITCH);
        boolean isAdsInBackgroundEnabled = getPrefAdsInBackgroundEnabled();
        mAdsSwitch.setChecked(isAdsInBackgroundEnabled);
        mAdsSwitch.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setPrefAdsInBackgroundEnabled((boolean) newValue);
                return true;
            }
        });
    }

    @Override
    public void onStart() {
        mHuhiRewardsNativeWorker = HuhiRewardsNativeWorker.getInstance();
        if (mHuhiRewardsNativeWorker != null) {
          mHuhiRewardsNativeWorker.AddObserver(this);
        }
        mHuhiRewardsNativeWorker.GetRewardsMainEnabled();
        super.onStart();
    }

    @Override
    public void onStop() {
        if (mHuhiRewardsNativeWorker != null) {
          mHuhiRewardsNativeWorker.RemoveObserver(this);
        }
        super.onStop();
    }

    /**
     * Returns the user preference for whether the huhi ads in background is enabled.
     *
     */
    public static boolean getPrefAdsInBackgroundEnabled() {
        SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
        return sharedPreferences.getBoolean(PREF_ADS_SWITCH, true);
    }

    /**
     * Sets the user preference for whether the huhi ads in background is enabled.
     */
    public void setPrefAdsInBackgroundEnabled(boolean enabled) {
        SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean(PREF_ADS_SWITCH, enabled);
        sharedPreferencesEditor.apply();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof HuhiRewardsResetPreference) {
            HuhiRewardsResetPreferenceDialog dialogFragment =
                    HuhiRewardsResetPreferenceDialog.newInstance(
                            (HuhiRewardsResetPreference) preference);
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), HuhiRewardsResetPreferenceDialog.TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public void OnWalletInitialized(int error_code) {}

    @Override
    public void OnWalletProperties(int error_code) {}

    @Override
    public void OnPublisherInfo(int tabId) {}

    @Override
    public void OnGetCurrentBalanceReport(String[] report) {}

    @Override
    public void OnNotificationAdded(String id, int type, long timestamp,
          String[] args) {}

    @Override
    public void OnNotificationsCount(int count) {}

    @Override
    public void OnGetLatestNotification(String id, int type, long timestamp,
              String[] args) {}

    @Override
    public void OnNotificationDeleted(String id) {}

    @Override
    public void OnIsWalletCreated(boolean created) {}

    @Override
    public void OnGetPendingContributionsTotal(double amount) {}

    @Override
    public void OnGetRewardsMainEnabled(boolean enabled) {
    }

    @Override
    public void OnGetAutoContributeProps() {}

    @Override
    public void OnGetReconcileStamp(long timestamp) {}

    @Override
    public void OnRecurringDonationUpdated() {}

    @Override
    public void OnResetTheWholeState(boolean success) {
        if (success) {
            SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putBoolean(HuhiRewardsPanelPopup.PREF_GRANTS_NOTIFICATION_RECEIVED, false);
            sharedPreferencesEditor.putBoolean(HuhiRewardsPanelPopup.PREF_WAS_HUHI_REWARDS_TURNED_ON, false);
            sharedPreferencesEditor.apply();
            PrefServiceBridge.getInstance().setSafetynetCheckFailed(false);
            RestartWorker.AskForRelaunch(getActivity());
        } else {
            RestartWorker.AskForRelaunchCustom(getActivity());
        }
    }

    @Override
    public void OnRewardsMainEnabled(boolean enabled) {
        if (!enabled) {
            mHuhiRewardsNativeWorker.ResetTheWholeState();
        } else {
            RestartWorker.AskForRelaunchCustom(getActivity());
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }
}
