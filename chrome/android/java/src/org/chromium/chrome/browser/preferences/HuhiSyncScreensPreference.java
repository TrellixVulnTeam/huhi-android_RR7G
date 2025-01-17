/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.preferences;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.SensorManager;
import org.chromium.base.task.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.text.Editable;
import android.text.format.DateUtils;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;

import org.chromium.base.Log;
import org.chromium.base.ThreadUtils;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.preferences.HuhiPreferenceFragment;
import org.chromium.ui.base.DeviceFormFactor;
import org.chromium.ui.KeyboardVisibilityDelegate;

import org.chromium.base.ApiCompatibilityUtils;
import org.chromium.base.ContextUtils;
import org.chromium.chrome.browser.HuhiSyncWorker;
import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.qrreader.BarcodeTracker;
import org.chromium.chrome.browser.qrreader.BarcodeTrackerFactory;
import org.chromium.chrome.browser.qrreader.CameraSource;
import org.chromium.chrome.browser.qrreader.CameraSourcePreview;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Settings fragment that allows to control Sync functionality.
 */
public class HuhiSyncScreensPreference extends HuhiPreferenceFragment
      implements View.OnClickListener, Preferences.OnBackPressedListener,
                 CompoundButton.OnCheckedChangeListener, BarcodeTracker.BarcodeGraphicTrackerCallback{

  private static final String TAG = "SYNC";
  // Permission request codes need to be < 256
  private static final int RC_HANDLE_CAMERA_PERM = 2;
  // Intent request code to handle updating play services if needed.
  private static final int RC_HANDLE_GMS = 9001;
  // For QR code generation
  private static final int WHITE = 0xFFFFFFFF;
  private static final int BLACK = 0xFF000000;
  private static final int WIDTH = 300;
  // For view sizes limit
  private static final int MAX_WIDTH = 512;
  private static final int MAX_HEIGHT = 1024;
  // Wait time out
  private static final int WAIT_TIMEOUT = 120000;
  // Timeout to show cancel button while loading devices on sync chain creation
  private static final int CANCEL_LOAD_BUTTON_TIMEOUT = 15*1000;

  private ChromeSwitchPreference mPrefSwitchTabs;
  private ChromeSwitchPreference mPrefSwitchHistory;
  private ChromeSwitchPreference mPrefSwitchAutofillPasswords;
  private ChromeSwitchPreference mPrefSwitchPaymentSettings;
  private Switch mSyncSwitchBookmarks;
  private Switch mSyncSwitchTabs;
  private Switch mSyncSwitchHistory;
  private Switch mSyncSwitchAutofillPasswords;
  private Switch mSyncSwitchPaymentSettings;
  // The have a sync code button displayed in the Sync view.
  private Button mScanChainCodeButton;
  private Button mStartNewChainButton;
  private Button mEnterCodeWordsButton;
  private Button mDoneButton;
  private Button mDoneLaptopButton;
  private Button mUseCameraButton;
  private Button mConfirmCodeWordsButton;
  private ImageButton mMobileButton;
  private ImageButton mLaptopButton;
  private ImageButton mPasteButton;
  private Button mCopyButton;
  private Button mAddDeviceButton;
  private Button mCancelLoadingButton;
  private Timer mCancelLoadingButtonUpdater;
  private Button mRemoveDeviceButton;
  private Button mQRCodeButton;
  private Button mCodeWordsButton;
  // Huhi Sync messaeg text view
  private TextView mHuhiSyncTextViewInitial;
  private TextView mHuhiSyncTextViewSyncChainCode;
  private TextView mHuhiSyncTextViewAddMobileDevice;
  private TextView mHuhiSyncTextViewAddLaptop;
  private TextView mHuhiSyncWarningTextViewAddMobileDevice;
  private TextView mHuhiSyncWarningTextViewAddLaptop;
  private TextView mHuhiSyncTextDevicesTitle;
  private TextView mHuhiSyncWordCountTitle;
  private TextView mHuhiSyncAddDeviceCodeWords;
  private CameraSource mCameraSource;
  private CameraSourcePreview mCameraSourcePreview;
  private HuhiSyncScreensObserver mSyncScreensObserver;
  private String mDeviceName = "";
  private ListView mDevicesListView;
  private ArrayAdapter<String> mDevicesAdapter;
  private List<String> mDevicesList;
  private ScrollView mScrollViewSyncInitial;
  private ScrollView mScrollViewSyncChainCode;
  private ScrollView mScrollViewSyncStartChain;
  private ScrollView mScrollViewAddMobileDevice;
  private ScrollView mScrollViewAddLaptop;
  private ScrollView mScrollViewEnterCodeWords;
  private ScrollView mScrollViewSyncDone;
  private LayoutInflater mInflater;
  private ImageView mQRCodeImage;
  private CountDownTimer mTimeoutTimer;
  private LinearLayout mLayoutSyncStartChain;
  private EditText mCodeWords;
  private FrameLayout mLayoutMobile;
  private FrameLayout mLayoutLaptop;

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);

      // Checks the orientation of the screen
      if (newConfig.orientation != Configuration.ORIENTATION_UNDEFINED
            && null != mCameraSourcePreview) {
          mCameraSourcePreview.stop();
          try {
              startCameraSource();
          } catch (SecurityException exc) {
          }
      }
      adjustImageButtons(newConfig.orientation);
  }

  @Override
  public View onCreateView(
          LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      if (ensureCameraPermission()) {
          createCameraSource(true, false);
      }
      mInflater = inflater;
      // Read which category we should be showing.
      return mInflater.inflate(R.layout.huhi_sync_layout, container, false);
  }

  private boolean ensureCameraPermission() {
      if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA)
              == PackageManager.PERMISSION_GRANTED){
          return true;
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          requestPermissions(
                  new String[]{Manifest.permission.CAMERA}, RC_HANDLE_CAMERA_PERM);
      }

      return false;
  }

  @Override
   public void onRequestPermissionsResult(int requestCode,
                                          String[] permissions,
                                          int[] grantResults) {
       if (requestCode != RC_HANDLE_CAMERA_PERM) {
           super.onRequestPermissionsResult(requestCode, permissions, grantResults);

           return;
       }

       if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
           // we have permission, so create the camerasource
           createCameraSource(true, false);

           return;
       }

       Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
               " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
       // We still allow to enter words
       //getActivity().onBackPressed();
   }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
      getActivity().setTitle(R.string.sign_in_sync);

      SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
      mDeviceName = sharedPref.getString(HuhiSyncWorker.PREF_SYNC_DEVICE_NAME, "");

      mPrefSwitchTabs = (ChromeSwitchPreference) findPreference(HuhiSyncWorker.PREF_SYNC_TABS);
      mPrefSwitchHistory = (ChromeSwitchPreference) findPreference(HuhiSyncWorker.PREF_SYNC_HISTORY);
      mPrefSwitchAutofillPasswords = (ChromeSwitchPreference) findPreference(HuhiSyncWorker.PREF_SYNC_AUTOFILL_PASSWORDS);
      mPrefSwitchPaymentSettings = (ChromeSwitchPreference) findPreference(HuhiSyncWorker.PREF_SYNC_PAYMENT_SETTINGS);

      // Initialize mSyncScreensObserver
      ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
      if (null != application && null != application.mHuhiSyncWorker) {
          if (null == mSyncScreensObserver) {
              mSyncScreensObserver = new HuhiSyncScreensObserver() {
                  @Override
                  public void onSyncError(String message) {
                      try {
                          if (null == getActivity()) {
                              return;
                          }
                          if (null != message && !message.isEmpty()) {
                              if (message.equals("Credential server response 400. Signed request body of the client timestamp is required.")) {
                                  message = getResources().getString(R.string.sync_requires_correct_time);
                              }
                              message = " [" + message + "]";
                          }
                          final String messageFinal = (null == message) ? "" : message;
                          getActivity().runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  cancelTimeoutTimer();
                                  showEndDialog(getResources().getString(R.string.sync_device_failure) + messageFinal);
                              }
                          });
                      } catch(Exception exc) {
                          Log.e(TAG, "onSyncError exception: " + exc);
                      }
                  }

                  @Override
                  public void onSeedReceived(String seed, boolean fromCodeWords, boolean afterInitialization) {
                      try {
                          if (fromCodeWords) {
                              assert !afterInitialization;
                              if (!isBarCodeValid(seed, false)) {
                                  showEndDialog(getResources().getString(R.string.sync_device_failure));
                              }
                              //Log.i(TAG, "!!!received seed == " + seed);
                              // Save seed and deviceId in preferences
                              SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
                              SharedPreferences.Editor editor = sharedPref.edit();
                              editor.putString(HuhiSyncWorker.PREF_SEED, seed);
                              editor.apply();
                              if (null == getActivity()) {
                                  return;
                              }
                              getActivity().runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      cancelTimeoutTimer();
                                      ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
                                      if (null != application && null != application.mHuhiSyncWorker) {
                                          application.mHuhiSyncWorker.SetSyncEnabled(true);
                                          application.mHuhiSyncWorker.InitSync(true, false);
                                      }
                                      setAppropriateView();
                                  }
                              });
                          } else if (afterInitialization) {
                              assert !fromCodeWords;
                              if (null != seed && !seed.isEmpty()) {
                                  if ((null != mScrollViewAddMobileDevice) && (View.VISIBLE == mScrollViewAddMobileDevice.getVisibility())) {
                                      String[] seeds = seed.split(",");
                                      if (seeds.length != 32) {
                                          Log.e(TAG, "Incorrect seed for QR code");
                                      }
                                      String qrData = "";
                                      for (String s : seeds) {
                                          String hex = Integer.toHexString(Integer.parseInt(s.trim(), 10));
                                          if (hex.length() == 1) {
                                              hex = "0" + hex;
                                          }
                                          qrData += hex;
                                      }
                                      final String qrDataFinal = qrData;
                                      //Log.i(TAG, "Generate QR with data: " + qrDataFinal);
                                      new Thread(new Runnable() {
                                          @Override
                                          public void run() {
                                              // Generate QR code
                                              BitMatrix result;
                                              try {
                                                  result = new MultiFormatWriter().encode(qrDataFinal, BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
                                              } catch (WriterException e) {
                                                  Log.e(TAG, "QR code unsupported format: " + e);
                                                  return;
                                              }
                                              int w = result.getWidth();
                                              int h = result.getHeight();
                                              int[] pixels = new int[w * h];
                                              for (int y = 0; y < h; y++) {
                                                  int offset = y * w;
                                                  for (int x = 0; x < w; x++) {
                                                      pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
                                                  }
                                              }
                                              Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                                              bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
                                              getActivity().runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      cancelTimeoutTimer();
                                                      application.mHuhiSyncWorker.SetSyncEnabled(true);
                                                      mQRCodeImage.setImageBitmap(bitmap);
                                                      mQRCodeImage.invalidate();
                                                  }
                                              });
                                          }
                                      }).start();
                                  } else if ((null != mScrollViewAddLaptop) && (View.VISIBLE == mScrollViewAddLaptop.getVisibility())) {
                                      if (null == getActivity()) {
                                          return;
                                      }
                                      getActivity().runOnUiThread(new Runnable() {
                                          @Override
                                          public void run() {
                                              application.mHuhiSyncWorker.GetCodeWords();
                                          }
                                      });
                                  }
                              }
                          } else {
                              Log.e(TAG, "Unknown flag on receiving seed");
                              assert false;
                          }
                      } catch(Exception exc) {
                          Log.e(TAG, "onSeedReceived exception: " + exc);
                      }
                  }

                  @Override
                  public void onCodeWordsReceived(String[] codeWords) {
                      try {
                          if (null == getActivity()) {
                              return;
                          }
                          getActivity().runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  cancelTimeoutTimer();
                                  application.mHuhiSyncWorker.SetSyncEnabled(true);
                                  String words = "";
                                  for (int i = 0; i < codeWords.length; i++) {
                                      words = words + " " + codeWords[i].trim();
                                  }
                                  mHuhiSyncAddDeviceCodeWords.setText(words.trim());
                              }
                          });
                      } catch(Exception exc) {
                          Log.e(TAG, "onCodeWordsReceived exception: " + exc);
                      }
                  }

                  @Override
                  public void onDevicesAvailable() {
                      try {
                          if (null == getActivity()) {
                              return;
                          }
                          getActivity().runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  if (View.VISIBLE != mScrollViewSyncDone.getVisibility()) {
                                      Log.w(TAG, "No need to load devices for other pages");
                                      return;
                                  }
                                  SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
                                  String currentDeviceId = sharedPref.getString(HuhiSyncWorker.PREF_DEVICE_ID, "");
                                  // Load other devices in chain
                                  ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
                                  if (null != application && null != application.mHuhiSyncWorker) {
                                      new Thread(new Runnable() {
                                          @Override
                                          public void run() {
                                              ArrayList<HuhiSyncWorker.ResolvedRecordToApply> devices = application.mHuhiSyncWorker.GetAllDevices();
                                              if (null == getActivity()) {
                                                  return;
                                              }
                                              getActivity().runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      ViewGroup insertPoint = (ViewGroup) getView().findViewById(R.id.huhi_sync_devices);
                                                      insertPoint.removeAllViews();
                                                      cancelTimeoutTimer();
                                                      int index = 0;
                                                      for (HuhiSyncWorker.ResolvedRecordToApply device : devices) {
                                                          View separator = (View) mInflater.inflate(R.layout.menu_separator, null);
                                                          View listItemView = (View) mInflater.inflate(R.layout.huhi_sync_device, null);
                                                          if (null != listItemView && null != separator && null != insertPoint) {
                                                              TextView textView = (TextView) listItemView.findViewById(R.id.huhi_sync_device_text);
                                                              if (null != textView) {
                                                                  textView.setText(device.mDeviceName);
                                                              }
                                                              AppCompatImageView deleteButton = (AppCompatImageView) listItemView.findViewById(R.id.huhi_sync_remove_device);
                                                              if (null != deleteButton) {
                                                                  if (currentDeviceId.equals(device.mDeviceId)) {
                                                                      // Current device is deleted by button on the bottom
                                                                      deleteButton.setVisibility(View.GONE);
                                                                      if (null != textView) {
                                                                          // Highlight curret device
                                                                          textView.setTextColor(ApiCompatibilityUtils.getColor(getActivity().getResources(), R.color.huhi_theme_color));
                                                                          String currentDevice = device.mDeviceName + " " + getResources().getString(R.string.huhi_sync_this_device_text);
                                                                          textView.setText(currentDevice);
                                                                      }
                                                                      if (null != mRemoveDeviceButton) {
                                                                          mRemoveDeviceButton.setTag(device);
                                                                          mRemoveDeviceButton.setVisibility(View.VISIBLE);
                                                                          mRemoveDeviceButton.setEnabled(true);
                                                                      }
                                                                  } else {
                                                                      deleteButton.setTag(device);
                                                                      deleteButton.setOnClickListener(v -> {
                                                                          HuhiSyncWorker.ResolvedRecordToApply deviceToDelete = (HuhiSyncWorker.ResolvedRecordToApply) v.getTag();
                                                                          deleteDeviceDialog(deviceToDelete.mDeviceName, deviceToDelete.mDeviceId, deviceToDelete.mObjectId, v);
                                                                      });
                                                                  }
                                                              }

                                                              insertPoint.addView(separator, index++);
                                                              insertPoint.addView(listItemView, index++);
                                                          }
                                                      }
                                                      if (index > 0) {
                                                          dismissCancelLoadingButton();
                                                          mHuhiSyncTextDevicesTitle.setText(getResources().getString(R.string.huhi_sync_devices_title));
                                                          View separator = (View) mInflater.inflate(R.layout.menu_separator, null);
                                                          if (null != insertPoint && null != separator) {
                                                              insertPoint.addView(separator, index++);
                                                          }
                                                      }
                                                  }
                                              });
                                          }
                                      }).start();
                                  }
                              }
                          });
                      } catch(Exception exc) {
                          Log.e(TAG, "onDevicesAvailable exception: " + exc);
                      }
                  }

                  @Override
                  public void onResetSync() {
                      try {
                          if (null == getActivity()) {
                              return;
                          }
                          getActivity().runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  cancelTimeoutTimer();
                                  setAppropriateView();
                              }
                          });
                      } catch(Exception exc) {
                          Log.e(TAG, "onResetSync exception: " + exc);
                      }
                  }

                  @Override
                  public boolean shouldLoadDevices() {
                      if (null == getActivity() || View.VISIBLE != mScrollViewSyncDone.getVisibility()) {
                          // No need to load devices for other pages
                          return false;
                      }
                      return true;
                  }
              };
          }
          application.mHuhiSyncWorker.InitJSWebView(mSyncScreensObserver);
      }

      mSyncSwitchBookmarks = (Switch) getView().findViewById(R.id.sync_bookmarks_switch);
      if (null != mSyncSwitchBookmarks) {
          mSyncSwitchBookmarks.setOnCheckedChangeListener(this);
      }
      //mSyncSwitchTabs = (Switch) getView().findViewById(R.id.huhi_sync_tabs_switch);
      if (null != mSyncSwitchTabs) {
          mSyncSwitchTabs.setOnCheckedChangeListener(this);
      }
      //mSyncSwitchHistory = (Switch) getView().findViewById(R.id.huhi_sync_history_switch);
      if (null != mSyncSwitchHistory) {
          mSyncSwitchHistory.setOnCheckedChangeListener(this);
      }
      //mSyncSwitchAutofillPasswords = (Switch) getView().findViewById(R.id.huhi_sync_autofill_passwords_switch);
      if (null != mSyncSwitchAutofillPasswords) {
          mSyncSwitchAutofillPasswords.setOnCheckedChangeListener(this);
      }
      //mSyncSwitchPaymentSettings = (Switch) getView().findViewById(R.id.huhi_sync_payment_settings_switch);
      if (null != mSyncSwitchPaymentSettings) {
          mSyncSwitchPaymentSettings.setOnCheckedChangeListener(this);
      }

      mScrollViewSyncInitial = (ScrollView) getView().findViewById(R.id.view_sync_initial);
      mScrollViewSyncChainCode = (ScrollView) getView().findViewById(R.id.view_sync_chain_code);
      mScrollViewSyncStartChain = (ScrollView) getView().findViewById(R.id.view_sync_start_chain);
      mScrollViewAddMobileDevice = (ScrollView) getView().findViewById(R.id.view_add_mobile_device);
      mScrollViewAddLaptop = (ScrollView) getView().findViewById(R.id.view_add_laptop);
      mScrollViewEnterCodeWords = (ScrollView) getView().findViewById(R.id.view_enter_code_words);
      mScrollViewSyncDone = (ScrollView) getView().findViewById(R.id.view_sync_done);

      if (!DeviceFormFactor.isTablet()) {
          clearBackground(mScrollViewSyncInitial);
          clearBackground(mScrollViewSyncChainCode);
          clearBackground(mScrollViewSyncStartChain);
          clearBackground(mScrollViewAddMobileDevice);
          clearBackground(mScrollViewAddLaptop);
          clearBackground(mScrollViewEnterCodeWords);
          clearBackground(mScrollViewSyncDone);
      }

      mLayoutSyncStartChain = (LinearLayout) getView().findViewById(R.id.view_sync_start_chain_layout);

      mScanChainCodeButton = (Button) getView().findViewById(R.id.huhi_sync_btn_scan_chain_code);
      if (mScanChainCodeButton != null) {
          mScanChainCodeButton.setOnClickListener(this);
      }

      mStartNewChainButton = (Button) getView().findViewById(R.id.huhi_sync_btn_start_new_chain);
      if (mStartNewChainButton != null) {
          mStartNewChainButton.setOnClickListener(this);
      }

      mEnterCodeWordsButton = (Button) getView().findViewById(R.id.huhi_sync_btn_enter_code_words);
      if (mEnterCodeWordsButton != null) {
          mEnterCodeWordsButton.setOnClickListener(this);
      }

      mQRCodeImage = (ImageView) getView().findViewById(R.id.huhi_sync_qr_code_image);

      mDoneButton = (Button) getView().findViewById(R.id.huhi_sync_btn_done);
      if (mDoneButton != null) {
          mDoneButton.setOnClickListener(this);
      }

      mDoneLaptopButton = (Button) getView().findViewById(R.id.huhi_sync_btn_add_laptop_done);
      if (mDoneLaptopButton != null) {
          mDoneLaptopButton.setOnClickListener(this);
      }

      mUseCameraButton = (Button) getView().findViewById(R.id.huhi_sync_btn_use_camera);
      if (mUseCameraButton != null) {
          mUseCameraButton.setOnClickListener(this);
      }

      mConfirmCodeWordsButton = (Button) getView().findViewById(R.id.huhi_sync_confirm_code_words);
      if (mConfirmCodeWordsButton != null) {
          mConfirmCodeWordsButton.setOnClickListener(this);
      }

      mMobileButton = (ImageButton) getView().findViewById(R.id.huhi_sync_btn_mobile);
      if (mMobileButton != null) {
          mMobileButton.setOnClickListener(this);
      }

      mLaptopButton = (ImageButton) getView().findViewById(R.id.huhi_sync_btn_laptop);
      if (mLaptopButton != null) {
          mLaptopButton.setOnClickListener(this);
      }

      mPasteButton = (ImageButton) getView().findViewById(R.id.huhi_sync_paste_button);
      if (mPasteButton != null) {
          mPasteButton.setOnClickListener(this);
      }

      mCopyButton = (Button) getView().findViewById(R.id.huhi_sync_copy_button);
      if (mCopyButton != null) {
          mCopyButton.setOnClickListener(this);
      }

      mHuhiSyncTextViewInitial = (TextView) getView().findViewById(R.id.huhi_sync_text_initial);
      mHuhiSyncTextViewSyncChainCode = (TextView) getView().findViewById(R.id.huhi_sync_text_sync_chain_code);
      mHuhiSyncTextViewAddMobileDevice = (TextView) getView().findViewById(R.id.huhi_sync_text_add_mobile_device);
      mHuhiSyncTextViewAddLaptop = (TextView) getView().findViewById(R.id.huhi_sync_text_add_laptop);
      mHuhiSyncWarningTextViewAddMobileDevice = (TextView) getView().findViewById(R.id.huhi_sync_warning_text_add_mobile_device);
      mHuhiSyncWarningTextViewAddLaptop = (TextView) getView().findViewById(R.id.huhi_sync_warning_text_add_laptop);
      mHuhiSyncTextDevicesTitle = (TextView) getView().findViewById(R.id.huhi_sync_devices_title);
      mHuhiSyncWordCountTitle = (TextView) getView().findViewById(R.id.huhi_sync_text_word_count);
      mHuhiSyncWordCountTitle.setText(getString(R.string.huhi_sync_word_count_text, 0));
      mHuhiSyncAddDeviceCodeWords = (TextView) getView().findViewById(R.id.huhi_sync_add_device_code_words);
      setMainSyncText();
      mCameraSourcePreview = (CameraSourcePreview) getView().findViewById(R.id.preview);

      mAddDeviceButton = (Button) getView().findViewById(R.id.huhi_sync_btn_add_device);
      if (null != mAddDeviceButton) {
          mAddDeviceButton.setOnClickListener(this);
      }

      mCancelLoadingButton = (Button) getView().findViewById(R.id.huhi_sync_btn_cancel_loading);
      if (null != mCancelLoadingButton) {
          mCancelLoadingButton.setOnClickListener(this);
      }

      mRemoveDeviceButton = (Button) getView().findViewById(R.id.huhi_sync_btn_remove_device);
      if (null != mRemoveDeviceButton) {
          mRemoveDeviceButton.setOnClickListener(this);
      }

      mQRCodeButton = (Button) getView().findViewById(R.id.huhi_sync_qr_code_off_btn);
      if (null != mQRCodeButton) {
          mQRCodeButton.setOnClickListener(this);
      }

      mCodeWordsButton = (Button) getView().findViewById(R.id.huhi_sync_code_words_off_btn);
      if (null != mCodeWordsButton) {
          mCodeWordsButton.setOnClickListener(this);
      }

      mCodeWords = (EditText) getView().findViewById(R.id.code_words);

      mLayoutMobile = (FrameLayout) getView().findViewById(R.id.huhi_sync_frame_mobile);
      mLayoutLaptop = (FrameLayout) getView().findViewById(R.id.huhi_sync_frame_laptop);

      mTimeoutTimer = new CountDownTimer(WAIT_TIMEOUT, WAIT_TIMEOUT) {
          @Override
          public void onTick(long millisUntilFinished) {
              // No action is required here
          }
          @Override
          public void onFinish() {
              if (null != mSyncScreensObserver) {
                  mSyncScreensObserver.onDevicesAvailable();
              }
              showEndDialog(getResources().getString(R.string.huhi_sync_time_out_message));
          }
      };

      setAppropriateView();

      super.onActivityCreated(savedInstanceState);
  }

  private void setAppropriateView() {
      getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
      getActivity().setTitle(R.string.prefs_sync_official);
      SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
      String seed = sharedPref.getString(HuhiSyncWorker.PREF_SEED, null);
      //Log.i(TAG, "setAppropriateView: seed == " + seed);
      if (null == seed || seed.isEmpty()) {
          if (null != mCameraSourcePreview) {
              mCameraSourcePreview.stop();
          }
          if (null != mScrollViewSyncInitial) {
              adjustWidth(mScrollViewSyncInitial, false);
              mScrollViewSyncInitial.setVisibility(View.VISIBLE);
          }
          if (null != mScrollViewSyncChainCode) {
              mScrollViewSyncChainCode.setVisibility(View.GONE);
          }
          if (null != mScrollViewEnterCodeWords) {
              mScrollViewEnterCodeWords.setVisibility(View.GONE);
          }
          if (null != mScrollViewAddMobileDevice) {
              mScrollViewAddMobileDevice.setVisibility(View.GONE);
          }
          if (null != mScrollViewAddLaptop) {
              mScrollViewAddLaptop.setVisibility(View.GONE);
          }
          if (null != mScrollViewSyncStartChain) {
              mScrollViewSyncStartChain.setVisibility(View.GONE);
          }
          if (null != mScrollViewSyncDone) {
              mScrollViewSyncDone.setVisibility(View.GONE);
          }
          if (null != mCodeWords) {
              mCodeWords.setText("");
          }
          return;
      }
      setSyncDoneLayout();
  }

  private void setMainSyncText() {
      setSyncText(getResources().getString(R.string.huhi_sync_official), getResources().getString(R.string.huhi_sync_description_page_1_part_1) + "\n\n" +
                    getResources().getString(R.string.huhi_sync_description_page_1_part_2), mHuhiSyncTextViewInitial);
  }

  private void setQRCodeText() {
      setSyncText("", getResources().getString(R.string.huhi_sync_qrcode_message_v2), mHuhiSyncTextViewSyncChainCode);
  }

  private void setSyncText(String title, String message, TextView textView) {
      String text = "";
      if (title.length() > 0) {
          text = title + "\n\n";
      }
      text += message;
      SpannableString formatedText =  new SpannableString(text);
      formatedText.setSpan(new RelativeSizeSpan(1.25f), 0, title.length(), 0);
      textView.setText(formatedText);
  }

  /** OnClickListener for the clear button. We show an alert dialog to confirm the action */
  @Override
  public void onClick(View v) {
      if ((getActivity() == null) || (v != mScanChainCodeButton && v != mStartNewChainButton
          && v != mEnterCodeWordsButton && v != mDoneButton && v != mDoneLaptopButton
          && v != mUseCameraButton && v != mConfirmCodeWordsButton && v != mMobileButton && v != mLaptopButton
          && v != mPasteButton && v != mCopyButton && v != mRemoveDeviceButton && v != mAddDeviceButton
          && v != mCancelLoadingButton && v != mQRCodeButton && v != mCodeWordsButton)) return;

      if (mScanChainCodeButton == v) {
          showAddDeviceNameDialog(false);
      } else if (mStartNewChainButton == v) {
          showAddDeviceNameDialog(true);
      } else if (mMobileButton == v) {
          setAddMobileDeviceLayout();
      } else if (mLaptopButton == v) {
          setAddLaptopLayout();
      } else if (mDoneButton == v) {
          setSyncDoneLayout();
      } else if (mDoneLaptopButton == v) {
          setSyncDoneLayout();
      } else if (mUseCameraButton == v) {
          setJoinExistingChainLayout();
      } else if (mQRCodeButton == v) {
          setAddMobileDeviceLayout();
      } else if (mCodeWordsButton == v) {
          setAddLaptopLayout();
      } else if (mPasteButton == v) {
          if (null != mCodeWords) {
              ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
              ClipData clipData = clipboard.getPrimaryClip();
              if (null != clipData && clipData.getItemCount() > 0) {
                  mCodeWords.setText(clipData.getItemAt(0).coerceToText(getActivity().getApplicationContext()));
              }
          }
      } else if (mCopyButton == v) {
          if (null != mHuhiSyncAddDeviceCodeWords) {
              ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
              ClipData clip = ClipData.newPlainText("", mHuhiSyncAddDeviceCodeWords.getText());
              clipboard.setPrimaryClip(clip);
              Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.huhi_sync_copied_text), Toast.LENGTH_LONG).show();
          }
      } else if (mConfirmCodeWordsButton == v) {
          ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
          String[] words = mCodeWords.getText().toString().trim().replace("   ", " ").replace("\n", " ").split(" ");
          if (HuhiSyncWorker.NICEWARE_WORD_COUNT != words.length && HuhiSyncWorker.BIP39_WORD_COUNT != words.length) {
              if (null != mSyncScreensObserver) {
                  mSyncScreensObserver.onSyncError(getResources().getString(R.string.huhi_sync_word_count_error));
              }
              return;
          }
          if (null != application && null != application.mHuhiSyncWorker && null != words) {
              for (int i = 0; i < words.length; i++) {
                  words[i] = words[i].trim();
              }
              application.mHuhiSyncWorker.GetNumber(words);
          }
      } else if (mEnterCodeWordsButton == v) {
          getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
          if (null != mScrollViewSyncInitial) {
              mScrollViewSyncInitial.setVisibility(View.GONE);
          }
          if (null != mScrollViewAddMobileDevice) {
              mScrollViewAddMobileDevice.setVisibility(View.GONE);
          }
          if (null != mScrollViewAddLaptop) {
              mScrollViewAddLaptop.setVisibility(View.GONE);
          }
          if (null != mScrollViewSyncStartChain) {
              mScrollViewSyncStartChain.setVisibility(View.GONE);
          }
          if (null != mCameraSourcePreview) {
              mCameraSourcePreview.stop();
          }
          if (null != mScrollViewSyncChainCode) {
              mScrollViewSyncChainCode.setVisibility(View.GONE);
          }
          if (null != mScrollViewEnterCodeWords) {
              adjustWidth(mScrollViewEnterCodeWords, false);
              mScrollViewEnterCodeWords.setVisibility(View.VISIBLE);
          }
          getActivity().setTitle(R.string.huhi_sync_code_words_title);
          if (null != mCodeWords && null != mHuhiSyncWordCountTitle) {
              mCodeWords.addTextChangedListener(new TextWatcher() {
                   @Override
                   public void afterTextChanged(Editable s) {}

                   @Override
                   public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                   @Override
                   public void onTextChanged(CharSequence s, int start, int before, int count) {
                       int wordCount = mCodeWords.getText().toString().length();
                       if (0 != wordCount) {
                           String[] words = mCodeWords.getText().toString().trim().replace("   ", " ").replace("\n", " ").split(" ");
                           wordCount = words.length;
                       }
                       mHuhiSyncWordCountTitle.setText(getString(R.string.huhi_sync_word_count_text, wordCount));
                       mHuhiSyncWordCountTitle.invalidate();
                   }
              });
          }
      } else if (mRemoveDeviceButton == v) {
          HuhiSyncWorker.ResolvedRecordToApply deviceToDelete = (HuhiSyncWorker.ResolvedRecordToApply) mRemoveDeviceButton.getTag();
          deleteDeviceDialog(deviceToDelete.mDeviceName, deviceToDelete.mDeviceId, deviceToDelete.mObjectId, mRemoveDeviceButton);
      } else if (mAddDeviceButton == v) {
          setNewChainLayout();
      } else if (mCancelLoadingButton == v) {
          cancelLoadingResetAndBack();
      }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
      if ((getActivity() == null) || (buttonView != mSyncSwitchBookmarks && buttonView != mSyncSwitchTabs &&
            buttonView != mSyncSwitchHistory && buttonView != mSyncSwitchAutofillPasswords && buttonView != mSyncSwitchPaymentSettings)) {
          Log.w(TAG, "Unknown button");
          return;
      }
      ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
      if (null != application && null != application.mHuhiSyncWorker) {
          if (buttonView == mSyncSwitchBookmarks) {
              application.mHuhiSyncWorker.SetSyncBookmarksEnabled(isChecked);
          } else if (buttonView == mSyncSwitchTabs && null != mPrefSwitchTabs) {
              mPrefSwitchTabs.setChecked(isChecked);
          } else if (buttonView == mSyncSwitchHistory && null != mPrefSwitchHistory) {
              mPrefSwitchHistory.setChecked(isChecked);
          } else if (buttonView == mSyncSwitchAutofillPasswords && null != mPrefSwitchAutofillPasswords) {
              mPrefSwitchAutofillPasswords.setChecked(isChecked);
          } else if (buttonView == mSyncSwitchPaymentSettings && null != mPrefSwitchPaymentSettings) {
              mPrefSwitchPaymentSettings.setChecked(isChecked);
          }
      }
  }

  private void showMainSyncScrypt() {
      if (null != mScrollViewSyncInitial) {
          adjustWidth(mScrollViewSyncInitial, false);
          mScrollViewSyncInitial.setVisibility(View.VISIBLE);
      }
      if (null != mScrollViewAddMobileDevice) {
          mScrollViewAddMobileDevice.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddLaptop) {
          mScrollViewAddLaptop.setVisibility(View.GONE);
      }
      if (null != mScrollViewSyncStartChain) {
          mScrollViewSyncStartChain.setVisibility(View.GONE);
      }
      if (null != mScrollViewSyncChainCode) {
          mScrollViewSyncChainCode.setVisibility(View.GONE);
      }
      if (null != mScrollViewEnterCodeWords) {
          mScrollViewEnterCodeWords.setVisibility(View.GONE);
      }
      setMainSyncText();
  }

  // Handles the requesting of the camera permission.
  private void requestCameraPermission() {
      Log.w(TAG, "Camera permission is not granted. Requesting permission");

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          final String[] permissions = new String[]{Manifest.permission.CAMERA};

          requestPermissions(permissions, RC_HANDLE_CAMERA_PERM);
      }
  }

  @SuppressLint("InlinedApi")
  private void createCameraSource(boolean autoFocus, boolean useFlash) {
      Context context = getActivity().getApplicationContext();

      // A barcode detector is created to track barcodes.  An associated multi-processor instance
      // is set to receive the barcode detection results, track the barcodes, and maintain
      // graphics for each barcode on screen.  The factory is used by the multi-processor to
      // create a separate tracker instance for each barcode.
      BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context)
              .setBarcodeFormats(Barcode.ALL_FORMATS)
              .build();
      BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(this);
      barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

      if (!barcodeDetector.isOperational()) {
          // Note: The first time that an app using the barcode or face API is installed on a
          // device, GMS will download a native libraries to the device in order to do detection.
          // Usually this completes before the app is run for the first time.  But if that
          // download has not yet completed, then the above call will not detect any barcodes.
          //
          // isOperational() can be used to check if the required native libraries are currently
          // available.  The detectors will automatically become operational once the library
          // downloads complete on device.
          Log.w(TAG, "Detector dependencies are not yet available.");
      }

      // Creates and starts the camera.  Note that this uses a higher resolution in comparison
      // to other detection examples to enable the barcode detector to detect small barcodes
      // at long distances.
      DisplayMetrics metrics = new DisplayMetrics();
      getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

      CameraSource.Builder builder = new CameraSource.Builder(getActivity().getApplicationContext(), barcodeDetector)
              .setFacing(CameraSource.CAMERA_FACING_BACK)
              .setRequestedPreviewSize(metrics.widthPixels, metrics.heightPixels)
              .setRequestedFps(24.0f);

      // make sure that auto focus is an available option
      builder = builder.setFocusMode(
              autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);

      mCameraSource = builder
              .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
              .build();
  }

  private void startCameraSource() throws SecurityException {
      if (mCameraSource != null && mCameraSourcePreview.mCameraExist) {
          // check that the device has play services available.
          try {
              int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                      getActivity().getApplicationContext());
              if (code != ConnectionResult.SUCCESS) {
                  Dialog dlg =
                          GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
                  if (null != dlg) {
                      dlg.show();
                  }
              }
          } catch (ActivityNotFoundException e) {
              Log.e(TAG, "Unable to start camera source.", e);
              mCameraSource.release();
              mCameraSource = null;

              return;
          }
          try {
              mCameraSourcePreview.start(mCameraSource);
          } catch (IOException e) {
              Log.e(TAG, "Unable to start camera source.", e);
              mCameraSource.release();
              mCameraSource = null;
          }
      }
  }

  @Override
  public void onResume() {
      super.onResume();
      try {
          if (null != mCameraSourcePreview && View.GONE != mScrollViewSyncChainCode.getVisibility()) {
              startCameraSource();
          }
      } catch (SecurityException se) {
          Log.e(TAG,"Do not have permission to start the camera", se);
      } catch (RuntimeException e) {
          Log.e(TAG, "Could not start camera source.", e);
      }
  }

  @Override
  public void onPause() {
      super.onPause();
      if (mCameraSourcePreview != null) {
          mCameraSourcePreview.stop();
      }
      InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
  }

  @Override
  public void onDestroy() {
      super.onDestroy();
      if (mCameraSourcePreview != null) {
          mCameraSourcePreview.release();
      }
  }

  private boolean isBarCodeValid(String barcode, boolean hexValue) {
      if (hexValue && barcode.length() != 64) {
          return false;
      } else if (!hexValue) {
          String[] split = barcode.split(",");
          if (split.length != 32) {
              return false;
          }
      }

      return true;
  }

  @Override
  public void onDetectedQrCode(Barcode barcode) {
      if (barcode != null) {
          //Log.i(TAG, "!!!code == " + barcode.displayValue);
          final String barcodeValue = barcode.displayValue;
          if (!isBarCodeValid(barcodeValue, true)) {
              showEndDialog(getResources().getString(R.string.sync_device_failure));
              showMainSyncScrypt();

              return;
          }
          String[] barcodeString = barcodeValue.replaceAll("..(?!$)", "$0 ").split(" ");
          String seed = "";
          for (int i = 0; i < barcodeString.length; i++) {
              if (0 != seed.length()) {
                  seed += ",";
              }
              seed += String.valueOf(Integer.parseInt(barcodeString[i], 16));
          }
          //Log.i(TAG, "!!!seed == " + seed);
          // Save seed and deviceId in preferences
          SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
          SharedPreferences.Editor editor = sharedPref.edit();
          editor.putString(HuhiSyncWorker.PREF_SEED, seed);
          editor.apply();
          getActivity().runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
                  if (null != application && null != application.mHuhiSyncWorker) {
                      application.mHuhiSyncWorker.SetSyncEnabled(true);
                      application.mHuhiSyncWorker.InitSync(true, false);
                  }
                  setAppropriateView();
              }
          });
      }
  }

  private void showEndDialog(String message) {
      AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.Theme_Chromium_AlertDialog);
      if (null == alert) {
          return;
      }
      DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int button) {
          }
      };
      AlertDialog alertDialog = alert
              .setTitle(getResources().getString(R.string.sync_device))
              .setMessage(message)
              .setPositiveButton(R.string.ok, onClickListener)
              .create();
      alertDialog.getDelegate().setHandleNativeActionModesEnabled(false);
      alertDialog.show();
  }

  private void startTimeoutTimerWithPopup(String message) {
      Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
      mTimeoutTimer.start();
  }

  private void cancelTimeoutTimer() {
      mTimeoutTimer.cancel();
  }

  private void showAddDeviceNameDialog(boolean createNewChain) {
      LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
              Context.LAYOUT_INFLATER_SERVICE);
      View view = inflater.inflate(R.layout.add_sync_device_name_dialog, null);
      final EditText input = (EditText) view.findViewById(R.id.device_name);

      DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int button) {
              if (button == AlertDialog.BUTTON_POSITIVE) {
                  mDeviceName = input.getText().toString();
                  if (mDeviceName.isEmpty()) {
                      mDeviceName = input.getHint().toString();
                  }
                  //Log.i(TAG, "mDeviceName just set: " + mDeviceName);
                  SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
                  SharedPreferences.Editor editor = sharedPref.edit();
                  editor.putString(HuhiSyncWorker.PREF_SYNC_DEVICE_NAME, mDeviceName);
                  editor.apply();
                  if (!createNewChain) {
                      setJoinExistingChainLayout();
                  } else {
                      setNewChainLayout();
                  }
              }
          }
      };

      AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.Theme_Chromium_AlertDialog);
      if (null == alert) {
          return;
      }
      AlertDialog alertDialog = alert
              .setTitle(R.string.sync_settings_add_device_name_title)
              .setMessage(getResources().getString(R.string.sync_settings_add_device_name_label))
              .setView(view)
              .setPositiveButton(R.string.ok, onClickListener)
              .setNegativeButton(R.string.cancel, onClickListener)
              .create();
      alertDialog.getDelegate().setHandleNativeActionModesEnabled(false);
      alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
          @Override
          public void onShow(DialogInterface dialog) {
            KeyboardVisibilityDelegate.getInstance().showKeyboard(input);
          }
      });
      alertDialog.show();
      Button cancelButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
      cancelButton.setVisibility(View.GONE);
  }

  private void deleteDeviceDialog(String deviceName, String deviceId, String deviceObjectId, View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity(), R.style.Theme_Chromium_AlertDialog);
        if (null == alert) {
            return;
        }
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int button) {
                if (button == AlertDialog.BUTTON_POSITIVE) {
                    ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
                    if (null != application && null != application.mHuhiSyncWorker) {
                        new Thread() {
                            @Override
                            public void run() {
                                application.mHuhiSyncWorker.SetUpdateDeleteDeviceName(HuhiSyncWorker.DELETE_RECORD, deviceName, deviceId, deviceObjectId);
                                application.mHuhiSyncWorker.InterruptSyncSleep();
                            }
                        }.start();
                        v.setEnabled(false);
                        startTimeoutTimerWithPopup(getResources().getString(R.string.huhi_sync_delete_sent));
                    }
                }
            }
        };
        AlertDialog alertDialog = alert
                .setTitle(getResources().getString(R.string.huhi_sync_remove_device_text))
                .setMessage(getString(R.string.huhi_sync_delete_device, deviceName))
                .setPositiveButton(R.string.ok, onClickListener)
                .setNegativeButton(R.string.cancel, onClickListener)
                .create();
        alertDialog.getDelegate().setHandleNativeActionModesEnabled(false);
        alertDialog.show();
  }

  private void setJoinExistingChainLayout() {
      if (null != mScrollViewSyncInitial) {
          mScrollViewSyncInitial.setVisibility(View.GONE);
      }
      if (null != mScrollViewEnterCodeWords) {
          mScrollViewEnterCodeWords.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddMobileDevice) {
          mScrollViewAddMobileDevice.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddLaptop) {
          mScrollViewAddLaptop.setVisibility(View.GONE);
      }
      if (null != mScrollViewSyncStartChain) {
          mScrollViewSyncStartChain.setVisibility(View.GONE);
      }
      setQRCodeText();
      getActivity().setTitle(R.string.huhi_sync_scan_chain_code);
      if (null != mScrollViewSyncChainCode) {
          adjustWidth(mScrollViewSyncChainCode, false);
          mScrollViewSyncChainCode.setVisibility(View.VISIBLE);
      }
      if (null != mCameraSourcePreview) {
          int rc = ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA);
          if (rc == PackageManager.PERMISSION_GRANTED) {
              try {
                startCameraSource();
              } catch (SecurityException exc) {
              }
          }
      }
  }

  private void setNewChainLayout() {
      getActivity().setTitle(R.string.huhi_sync_start_new_chain);
      if (null != mScrollViewSyncInitial) {
          mScrollViewSyncInitial.setVisibility(View.GONE);
      }
      if (null != mScrollViewEnterCodeWords) {
          mScrollViewEnterCodeWords.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddMobileDevice) {
          mScrollViewAddMobileDevice.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddLaptop) {
          mScrollViewAddLaptop.setVisibility(View.GONE);
      }
      if (null != mScrollViewSyncStartChain) {
          mScrollViewSyncStartChain.setVisibility(View.VISIBLE);
      }
      if (null != mScrollViewSyncDone) {
          mScrollViewSyncDone.setVisibility(View.GONE);
      }
      adjustImageButtons(getActivity().getApplicationContext().getResources().getConfiguration().orientation);
  }

  private void cancelLoadingResetAndBack() {
      ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
      if (null != application && null != application.mHuhiSyncWorker) {
          application.mHuhiSyncWorker.ResetSync();
      }
  }

  private void setAddMobileDeviceLayout() {
      getActivity().setTitle(R.string.huhi_sync_btn_mobile);
      if (null != mHuhiSyncTextViewAddMobileDevice) {
          setSyncText(getResources().getString(R.string.huhi_sync_scan_sync_code),
                        getResources().getString(R.string.huhi_sync_add_mobile_device_text_part_1) + "\n\n" +
                        getResources().getString(R.string.huhi_sync_add_mobile_device_text_part_2) + "\n", mHuhiSyncTextViewAddMobileDevice);
      }
      if (null != mHuhiSyncWarningTextViewAddMobileDevice) {
        String huhiSyncCodeWarning = getResources().getString(R.string.huhi_sync_code_warning);
        SpannableString huhiSyncCodeWarningSpanned = new SpannableString(huhiSyncCodeWarning);

        ForegroundColorSpan foregroundSpan = new ForegroundColorSpan(getResources().getColor(R.color.default_red));
        huhiSyncCodeWarningSpanned.setSpan(foregroundSpan, 0, huhiSyncCodeWarningSpanned.length()-1, 0);
        mHuhiSyncWarningTextViewAddMobileDevice.setText(huhiSyncCodeWarningSpanned);
      }
      if (null != mScrollViewSyncInitial) {
          mScrollViewSyncInitial.setVisibility(View.GONE);
      }
      if (null != mScrollViewEnterCodeWords) {
          mScrollViewEnterCodeWords.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddMobileDevice) {
          adjustWidth(mScrollViewAddMobileDevice, false);
          mScrollViewAddMobileDevice.setVisibility(View.VISIBLE);
      }
      if (null != mScrollViewAddLaptop) {
          mScrollViewAddLaptop.setVisibility(View.GONE);
      }
      if (null != mScrollViewSyncStartChain) {
          mScrollViewSyncStartChain.setVisibility(View.GONE);
      }
      getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
              ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
              if (null != application && null != application.mHuhiSyncWorker) {
                  SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
                  String seed = sharedPref.getString(HuhiSyncWorker.PREF_SEED, null);
                  if (null == seed || seed.isEmpty()) {
                      startTimeoutTimerWithPopup(getResources().getString(R.string.huhi_sync_loading_data_title));
                      // Init to receive new seed
                      application.mHuhiSyncWorker.InitSync(true, true);
                  } else {
                      mSyncScreensObserver.onSeedReceived(seed, false, true);
                  }
              }
          }
      });
  }

  private void setAddLaptopLayout() {
      getActivity().setTitle(R.string.huhi_sync_btn_laptop);
      if (null != mHuhiSyncTextViewAddLaptop) {
          setSyncText(getResources().getString(R.string.huhi_sync_add_laptop_text_title),
                        getResources().getString(R.string.huhi_sync_add_laptop_text_part_1) + "\n\n" +
                        getResources().getString(R.string.huhi_sync_add_laptop_text_part_2_new) + "\n", mHuhiSyncTextViewAddLaptop);
      }

      if (null != mHuhiSyncWarningTextViewAddLaptop) {
        String huhiSyncCodeWarning = getResources().getString(R.string.huhi_sync_code_warning);
        SpannableString huhiSyncCodeWarningSpanned = new SpannableString(huhiSyncCodeWarning);

        ForegroundColorSpan foregroundSpan = new ForegroundColorSpan(getResources().getColor(R.color.default_red));
        huhiSyncCodeWarningSpanned.setSpan(foregroundSpan, 0, huhiSyncCodeWarningSpanned.length()-1, 0);
        mHuhiSyncWarningTextViewAddLaptop.setText(huhiSyncCodeWarningSpanned);
      }

      if (null != mScrollViewSyncInitial) {
          mScrollViewSyncInitial.setVisibility(View.GONE);
      }
      if (null != mScrollViewEnterCodeWords) {
          mScrollViewEnterCodeWords.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddMobileDevice) {
          mScrollViewAddMobileDevice.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddLaptop) {
          adjustWidth(mScrollViewAddLaptop, false);
          mScrollViewAddLaptop.setVisibility(View.VISIBLE);
      }
      if (null != mScrollViewSyncStartChain) {
          mScrollViewSyncStartChain.setVisibility(View.GONE);
      }
      getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
              ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
              if (null != application && null != application.mHuhiSyncWorker) {
                  SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
                  String seed = sharedPref.getString(HuhiSyncWorker.PREF_SEED, null);
                  if (null == seed || seed.isEmpty()) {
                      startTimeoutTimerWithPopup(getResources().getString(R.string.huhi_sync_loading_data_title));
                      // Init to receive new seed
                      application.mHuhiSyncWorker.InitSync(true, true);
                  } else {
                      mSyncScreensObserver.onSeedReceived(seed, false, true);
                  }
              }
          }
      });
  }

  private void scheduleCancelButton() {
      SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(HuhiSyncWorker.PREF_NAME, 0);
      String deviceId = sharedPref.getString(HuhiSyncWorker.PREF_DEVICE_ID, null);
      boolean syncChainExists = (deviceId != null && !deviceId.isEmpty());
      if (!syncChainExists) {
          mCancelLoadingButtonUpdater = new Timer();
          mCancelLoadingButtonUpdater.schedule(new TimerTask() {
              @Override
              public void run() {
                  ThreadUtils.runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          mCancelLoadingButton.setVisibility(View.VISIBLE);
                      }
                  });
              }
          }, CANCEL_LOAD_BUTTON_TIMEOUT);
      }
  }

  private void dismissCancelLoadingButton() {
      mCancelLoadingButton.setVisibility(View.GONE);
      if (mCancelLoadingButtonUpdater != null) {
          mCancelLoadingButtonUpdater.cancel();
      }
  }

  private void setSyncDoneLayout() {
      getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
      getActivity().setTitle(R.string.prefs_sync_official);
      if (null != mCameraSourcePreview) {
          mCameraSourcePreview.stop();
      }
      if (null != mScrollViewSyncInitial) {
          mScrollViewSyncInitial.setVisibility(View.GONE);
      }
      if (null != mScrollViewSyncChainCode) {
          mScrollViewSyncChainCode.setVisibility(View.GONE);
      }
      if (null != mScrollViewEnterCodeWords) {
          mScrollViewEnterCodeWords.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddMobileDevice) {
          mScrollViewAddMobileDevice.setVisibility(View.GONE);
      }
      if (null != mScrollViewAddLaptop) {
          mScrollViewAddLaptop.setVisibility(View.GONE);
      }
      if (null != mScrollViewSyncStartChain) {
          mScrollViewSyncStartChain.setVisibility(View.GONE);
      }
      if (null != mScrollViewSyncDone) {
          adjustWidth(mScrollViewSyncDone, false);
          mScrollViewSyncDone.setVisibility(View.VISIBLE);
      }
      ChromeApplication application = (ChromeApplication)ContextUtils.getBaseApplicationContext();
      if (null != application && null != application.mHuhiSyncWorker) {
          if (null != mSyncSwitchBookmarks) {
              mSyncSwitchBookmarks.setChecked(application.mHuhiSyncWorker.IsSyncBookmarksEnabled());
          }
          if (null != mSyncSwitchTabs) {
              if (null != mPrefSwitchTabs) {
                  mSyncSwitchTabs.setChecked(mPrefSwitchTabs.isChecked());
              }
          }
          if (null != mSyncSwitchHistory) {
              if (null != mPrefSwitchHistory) {
                  mSyncSwitchHistory.setChecked(mPrefSwitchHistory.isChecked());
              }
          }
          if (null != mSyncSwitchAutofillPasswords) {
              if (null != mPrefSwitchAutofillPasswords) {
                  mSyncSwitchAutofillPasswords.setChecked(mPrefSwitchAutofillPasswords.isChecked());
              }
          }
          if (null != mSyncSwitchPaymentSettings) {
              if (null != mPrefSwitchPaymentSettings) {
                  mSyncSwitchPaymentSettings.setChecked(mPrefSwitchPaymentSettings.isChecked());
              }
          }
      }
      if (null != mRemoveDeviceButton) {
          // It should become visible as soon as we get all devices info
          mRemoveDeviceButton.setVisibility(View.GONE);
      }
      if (null != application && null != application.mHuhiSyncWorker) {
          mHuhiSyncTextDevicesTitle.setText(getResources().getString(R.string.huhi_sync_loading_devices_title));
          application.mHuhiSyncWorker.InterruptSyncSleep();
      }

      scheduleCancelButton();

      if (null != mSyncScreensObserver) {
          mSyncScreensObserver.onDevicesAvailable();
      }
  }

  private void adjustWidth(View view, boolean special) {
      if (DeviceFormFactor.isTablet()) {
          DisplayMetrics metrics = new DisplayMetrics();
          getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
          LayoutParams params = view.getLayoutParams();
          if (special) {
              params.width = metrics.widthPixels * 3 / 5;
              params.height = metrics.heightPixels * 3 / 5;
          } else {
              int width = (metrics.widthPixels > metrics.heightPixels) ? metrics.widthPixels : metrics.heightPixels;
              width = width / 2;
              params.width = width;
              params.height = LayoutParams.MATCH_PARENT;
          }
          view.setLayoutParams(params);
      }
  }

  private void adjustImageButtons(int orientation) {
      if ((null != mLayoutSyncStartChain) && (null != mScrollViewSyncStartChain) && (View.VISIBLE == mScrollViewSyncStartChain.getVisibility())) {
          adjustWidth(mScrollViewSyncStartChain, true);
          LayoutParams params = mLayoutMobile.getLayoutParams();
          if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
              mLayoutSyncStartChain.setOrientation(LinearLayout.HORIZONTAL);
              params.width = 0;
              params.height = LayoutParams.MATCH_PARENT;
          } else {
              mLayoutSyncStartChain.setOrientation(LinearLayout.VERTICAL);
              params.width = LayoutParams.MATCH_PARENT;
              params.height = 0;
          }
          mLayoutMobile.setLayoutParams(params);
          mLayoutLaptop.setLayoutParams(params);
      }
  }

  private void clearBackground(View view) {
      if (null != view) {
          view.setBackgroundColor(Color.TRANSPARENT);
      }
  }

  // Handles 'Back' button. Returns true if it is handled, false otherwise.
  @Override
  public boolean onBackPressed() {
      if ((View.VISIBLE == mScrollViewSyncChainCode.getVisibility())
      || (View.VISIBLE == mScrollViewSyncStartChain.getVisibility())) {
          setAppropriateView();
          return true;
      } else if ((View.VISIBLE == mScrollViewAddMobileDevice.getVisibility())
      || (View.VISIBLE == mScrollViewAddLaptop.getVisibility())) {
          setNewChainLayout();
          return true;
      } else if (View.VISIBLE == mScrollViewEnterCodeWords.getVisibility()) {
          setJoinExistingChainLayout();
          return true;
      }
      cancelTimeoutTimer();
      return false;
  }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }
}
