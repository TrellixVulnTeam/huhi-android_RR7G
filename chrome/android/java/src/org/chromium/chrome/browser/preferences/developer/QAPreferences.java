// Copyright 2019 The Huhi Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.preferences.developer;

import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.chromium.base.ContextUtils;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.HuhiRewardsNativeWorker;
import org.chromium.chrome.browser.HuhiRewardsObserver;
import org.chromium.chrome.browser.HuhiRewardsPanelPopup;
import org.chromium.chrome.browser.ConfigAPIs;
import org.chromium.chrome.browser.preferences.ChromeSwitchPreference;
import org.chromium.chrome.browser.preferences.PreferenceUtils;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.RestartWorker;
import org.chromium.chrome.browser.preferences.HuhiPreferenceFragment;

import org.chromium.base.Log;
/**
 * Settings fragment containing preferences for QA team.
 */
public class QAPreferences extends HuhiPreferenceFragment
        implements OnPreferenceChangeListener, HuhiRewardsObserver {
    private static final String PREF_USE_REWARDS_STAGING_SERVER = "use_rewards_staging_server";
    private static final String PREF_QA_MAXIMIZE_INITIAL_ADS_NUMBER = "qa_maximize_initial_ads_number";

    private static final String QA_ADS_PER_HOUR = "qa_ads_per_hour";
    private static final String QA_ADS_PER_DAY = "qa_ads_per_day";

    private static final int MAX_ADS_NUMBER = 50;
    private static final int DEFAULT_ADS_PER_HOUR = 2;
    private static final int DEFAULT_ADS_PER_DAY = 20;

    ChromeSwitchPreference mIsStagingServer;
    ChromeSwitchPreference mMaximizeAdsNumber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceUtils.addPreferencesFromResource(this, R.xml.qa_preferences);

        mIsStagingServer =
                (ChromeSwitchPreference) findPreference(PREF_USE_REWARDS_STAGING_SERVER);
        if (mIsStagingServer != null) {
            mIsStagingServer.setOnPreferenceChangeListener(this);
        }

        mMaximizeAdsNumber =
                (ChromeSwitchPreference) findPreference(PREF_QA_MAXIMIZE_INITIAL_ADS_NUMBER);
        if (mMaximizeAdsNumber != null) {
            mMaximizeAdsNumber.setEnabled(mIsStagingServer.isChecked());
            mMaximizeAdsNumber.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onStart() {
        HuhiRewardsNativeWorker.getInstance().AddObserver(this);
        checkQACode();
        super.onStart();
    }

    @Override
    public void onStop() {
        HuhiRewardsNativeWorker.getInstance().RemoveObserver(this);
        super.onStop();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (PREF_USE_REWARDS_STAGING_SERVER.equals(preference.getKey())) {
            PrefServiceBridge.getInstance().setUseRewardsStagingServer((boolean) newValue);
            HuhiRewardsNativeWorker.getInstance().ResetTheWholeState();
            mMaximizeAdsNumber.setEnabled((boolean) newValue);
            enableMaximumAdsNumber(((boolean) newValue) && mMaximizeAdsNumber.isChecked());
            RestartWorker.AskForRelaunch(getActivity());
        } else if (PREF_QA_MAXIMIZE_INITIAL_ADS_NUMBER.equals(preference.getKey())) {
            enableMaximumAdsNumber((boolean) newValue);
        }
        return true;
    }

    private void checkQACode() {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.qa_code_check, null);
        final EditText input = (EditText) view.findViewById(R.id.qa_code);

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int button) {
                if (button != AlertDialog.BUTTON_POSITIVE ||
                    !input.getText().toString().equals(ConfigAPIs.QA_CODE)) {
                    getActivity().finish();
                }
            }
        };

        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.Theme_Chromium_AlertDialog);
        if (null == alert) {
            return;
        }
        AlertDialog.Builder alertDialog = alert
                .setTitle("Enter QA code")
                .setView(view)
                .setPositiveButton(R.string.ok, onClickListener)
                .setNegativeButton(R.string.cancel, onClickListener)
                .setCancelable(false);
        Dialog dialog = alertDialog.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void enableMaximumAdsNumber(boolean enable) {
        if (enable) {
            // Save current values
            int adsPerHour = HuhiRewardsNativeWorker.getInstance().GetAdsPerHour();
            int adsPerDay = HuhiRewardsNativeWorker.getInstance().GetAdsPerDay();
            SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putInt(QA_ADS_PER_HOUR, adsPerHour);
            sharedPreferencesEditor.putInt(QA_ADS_PER_DAY, adsPerDay);
            sharedPreferencesEditor.apply();
            // Set max values
            HuhiRewardsNativeWorker.getInstance().SetAdsPerHour(MAX_ADS_NUMBER);
            HuhiRewardsNativeWorker.getInstance().SetAdsPerDay(MAX_ADS_NUMBER);
            return;
        }
        // Set saved values
        int adsPerHour = ContextUtils.getAppSharedPreferences().getInt(QA_ADS_PER_HOUR, DEFAULT_ADS_PER_HOUR);
        int adsPerDay = ContextUtils.getAppSharedPreferences().getInt(QA_ADS_PER_DAY, DEFAULT_ADS_PER_DAY);
        HuhiRewardsNativeWorker.getInstance().SetAdsPerHour(adsPerHour);
        HuhiRewardsNativeWorker.getInstance().SetAdsPerDay(adsPerDay);
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
    public void OnRewardsMainEnabled(boolean enabled) {}

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }
}