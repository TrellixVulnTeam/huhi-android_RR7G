/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 #include "shields_config.h"
 #include "base/android/jni_android.h"
 #include "base/android/jni_string.h"
 #include "chrome/android/chrome_jni_headers/ShieldsConfig_jni.h"
 #include "chrome/browser/net/blockers/blockers_worker.h"

namespace net {
namespace blockers {

ShieldsConfig* gShieldsConfig = nullptr;

ShieldsConfig::ShieldsConfig(JNIEnv* env, jobject obj):
  weak_java_shields_config_(env, obj) {
}

ShieldsConfig::~ShieldsConfig() {
}

bool ShieldsConfig::getPrivacyAdBlock() {
  JNIEnv* env = base::android::AttachCurrentThread();

  return Java_ShieldsConfig_getPrivacyAdBlock(env, weak_java_shields_config_.get(env));
}

bool ShieldsConfig::getPrivacyAdBlockRegional() {
  JNIEnv* env = base::android::AttachCurrentThread();

  return Java_ShieldsConfig_getPrivacyAdBlockRegional(env, weak_java_shields_config_.get(env));
}

bool ShieldsConfig::getPrivacyHTTPSE() {
  JNIEnv* env = base::android::AttachCurrentThread();

  return Java_ShieldsConfig_getPrivacyHTTPSE(env, weak_java_shields_config_.get(env));
}

std::string ShieldsConfig::getHostSettings(const bool &incognitoTab, const std::string& host) {
  JNIEnv* env = base::android::AttachCurrentThread();
  base::android::ScopedJavaLocalRef<jstring> jhost(base::android::ConvertUTF8ToJavaString(env, host));
  return base::android::ConvertJavaStringToUTF8(
    Java_ShieldsConfig_getHostSettings(env, weak_java_shields_config_.get(env),
    incognitoTab, jhost));
}

void ShieldsConfig::setBlockedCountInfo(const std::string& url, int trackersBlocked, int adsBlocked, int httpsUpgrades,
        int scriptsBlocked, int fingerprintingBlocked) {
  JNIEnv* env = base::android::AttachCurrentThread();
  base::android::ScopedJavaLocalRef<jstring> jurl(base::android::ConvertUTF8ToJavaString(env, url));
  Java_ShieldsConfig_setBlockedCountInfo(env, weak_java_shields_config_.get(env),
    jurl, trackersBlocked, adsBlocked, httpsUpgrades, scriptsBlocked, fingerprintingBlocked);
}

bool ShieldsConfig::needUpdateAdBlocker() {
  JNIEnv* env = base::android::AttachCurrentThread();

  return Java_ShieldsConfig_needUpdateAdBlocker(env, weak_java_shields_config_.get(env));
}

void ShieldsConfig::resetUpdateAdBlockerFlag() {
  JNIEnv* env = base::android::AttachCurrentThread();
  Java_ShieldsConfig_resetUpdateAdBlockerFlag(env, weak_java_shields_config_.get(env));
}

std::shared_ptr<BlockersWorker> ShieldsConfig::getBlockersWorker(bool force_reset) {
  if (!blockers_worker_ || force_reset) {
    blockers_worker_.reset(new BlockersWorker());
  }
  return blockers_worker_;
}

ShieldsConfig* ShieldsConfig::getShieldsConfig() {
    return gShieldsConfig;
}

static void JNI_ShieldsConfig_Clear(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj) {
    delete gShieldsConfig;
    gShieldsConfig = nullptr;
}

static void JNI_ShieldsConfig_Init(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj) {
  // This will automatically bind to the Java object and pass ownership there.
  gShieldsConfig = new ShieldsConfig(env, obj);
}

// static
/*bool ShieldsConfig::RegisterShieldsConfig(JNIEnv* env) {
  return RegisterNativesImpl(env);
}*/

}
}
