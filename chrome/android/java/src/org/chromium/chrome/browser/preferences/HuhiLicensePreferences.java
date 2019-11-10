/* Copyright (c) 2019 The Huhi Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.preferences;

import android.os.Bundle;
import org.chromium.base.Log;
import org.chromium.chrome.browser.HuhiRewardsHelper;
import org.chromium.chrome.browser.preferences.HuhiPreferenceFragment;
import org.chromium.chrome.browser.preferences.TextMessagePreference;
import org.chromium.chrome.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Fragment to display Huhi license information.
 */
public class HuhiLicensePreferences extends HuhiPreferenceFragment {
    private static final String TAG = "HuhiLicense";

    private static final String PREF_HUHI_LICENSE_TEXT = "huhi_license_text";
    private static final String ASSET_HUHI_LICENSE = "HUHI_LICENSE.html";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        PreferenceUtils.addPreferencesFromResource(this, R.xml.huhi_license_preferences);
        getActivity().setTitle(R.string.huhi_license_text);
        TextMessagePreference licenseText = (TextMessagePreference) findPreference(PREF_HUHI_LICENSE_TEXT);
        try {
            InputStream in = getActivity().getAssets().open(ASSET_HUHI_LICENSE);
            Scanner scanner = new Scanner(in).useDelimiter("\\A");
            String summary = scanner.hasNext() ? scanner.next() : "";
            in.close();
            licenseText.setSummary(HuhiRewardsHelper.spannedFromHtmlString(summary));
        } catch (IOException exc) {
            Log.e(TAG, "Could not load license text");
        }
    }
}
