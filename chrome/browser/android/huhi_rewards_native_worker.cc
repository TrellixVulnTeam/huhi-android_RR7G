/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
#include "huhi_rewards_native_worker.h"
#include "base/android/jni_android.h"
#include "base/android/jni_string.h"
#include "base/android/jni_array.h"
#include "huhi/components/huhi_ads/browser/ads_service.h"
#include "huhi/components/huhi_ads/browser/ads_service_factory.h"
#include "huhi/components/huhi_rewards/browser/rewards_service_factory.h"
#include "huhi/components/huhi_rewards/browser/rewards_service.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/profiles/profile_manager.h"
#include "content/public/browser/url_data_source.h"
#include "chrome/android/chrome_jni_headers/HuhiRewardsNativeWorker_jni.h"

#define DEFAULT_ADS_PER_HOUR 2
#define DEFAULT_ADS_PER_DAY 12

namespace chrome {
namespace android {

HuhiRewardsNativeWorker::HuhiRewardsNativeWorker(JNIEnv* env, const base::android::JavaRef<jobject>& obj):
    weak_java_huhi_rewards_native_worker_(env, obj),
    huhi_rewards_service_(nullptr),
    weak_factory_(this) {

  Java_HuhiRewardsNativeWorker_setNativePtr(env, obj, reinterpret_cast<intptr_t>(this));

  huhi_rewards_service_ = huhi_rewards::RewardsServiceFactory::GetForProfile(
      ProfileManager::GetActiveUserProfile()->GetOriginalProfile());
  if (huhi_rewards_service_) {
    huhi_rewards_service_->AddObserver(this);
    huhi_rewards_service_->AddPrivateObserver(this);
    huhi_rewards::RewardsNotificationService* notification_service =
      huhi_rewards_service_->GetNotificationService();
    if (notification_service) {
      notification_service->AddObserver(this);
    }
  }
}

HuhiRewardsNativeWorker::~HuhiRewardsNativeWorker() {
}

void HuhiRewardsNativeWorker::Destroy(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->RemoveObserver(this);
    huhi_rewards_service_->RemovePrivateObserver(this);
    huhi_rewards::RewardsNotificationService* notification_service =
      huhi_rewards_service_->GetNotificationService();
    if (notification_service) {
      notification_service->RemoveObserver(this);
    }
  }
  delete this;
}

void HuhiRewardsNativeWorker::CreateWallet(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->CreateWallet(base::Bind(
            &HuhiRewardsNativeWorker::OnCreateWallet,
            weak_factory_.GetWeakPtr()));
  }
}

void HuhiRewardsNativeWorker::OnCreateWallet(int32_t result) {
}

void HuhiRewardsNativeWorker::GetWalletProperties(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->FetchWalletProperties();
  }
}

void HuhiRewardsNativeWorker::GetPublisherInfo(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller, int tabId,
        const base::android::JavaParamRef<jstring>& host) {
  if (huhi_rewards_service_) {
    // TODO fill publisher_blob
    huhi_rewards_service_->GetPublisherActivityFromUrl(tabId,
      base::android::ConvertJavaStringToUTF8(env, host), "", "");
  }
}

void HuhiRewardsNativeWorker::OnPanelPublisherInfo(
      huhi_rewards::RewardsService* rewards_service,
      int error_code,
      const ledger::PublisherInfo* info,
      uint64_t tabId) {
  if (!info) {
    return;
  }
  ledger::PublisherInfoPtr pi = info->Clone();
  map_publishers_info_[tabId] = std::move(pi);
  JNIEnv* env = base::android::AttachCurrentThread();
  Java_HuhiRewardsNativeWorker_OnPublisherInfo(env,
        weak_java_huhi_rewards_native_worker_.get(env), tabId);
}

base::android::ScopedJavaLocalRef<jstring> HuhiRewardsNativeWorker::GetPublisherURL(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId) {
  base::android::ScopedJavaLocalRef<jstring> res = base::android::ConvertUTF8ToJavaString(env, "");

  PublishersInfoMap::const_iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    res = base::android::ConvertUTF8ToJavaString(env, iter->second->url);
  }

  return res;
}

base::android::ScopedJavaLocalRef<jstring> HuhiRewardsNativeWorker::GetPublisherFavIconURL(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId) {
  base::android::ScopedJavaLocalRef<jstring> res = base::android::ConvertUTF8ToJavaString(env, "");

  PublishersInfoMap::const_iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    res = base::android::ConvertUTF8ToJavaString(env, iter->second->favicon_url);
  }

  return res; 
}

base::android::ScopedJavaLocalRef<jstring> HuhiRewardsNativeWorker::GetPublisherName(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId) {
  base::android::ScopedJavaLocalRef<jstring> res = base::android::ConvertUTF8ToJavaString(env, "");

  PublishersInfoMap::const_iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    res = base::android::ConvertUTF8ToJavaString(env, iter->second->name);
  }

  return res;
}

base::android::ScopedJavaLocalRef<jstring> HuhiRewardsNativeWorker::GetPublisherId(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId) {
  base::android::ScopedJavaLocalRef<jstring> res = base::android::ConvertUTF8ToJavaString(env, "");

  PublishersInfoMap::const_iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    res = base::android::ConvertUTF8ToJavaString(env, iter->second->id);
  }

  return res;
}

int HuhiRewardsNativeWorker::GetPublisherPercent(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId) {
  int res = 0;

  PublishersInfoMap::const_iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    res = iter->second->percent;
  }

  return res;
}

bool HuhiRewardsNativeWorker::GetPublisherExcluded(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId) {
  bool res = false;

  PublishersInfoMap::const_iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    res = iter->second->excluded == ledger::PublisherExclude::EXCLUDED;
  }

  return res;
}

bool HuhiRewardsNativeWorker::GetPublisherVerified(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId) {
  bool res = false;

  PublishersInfoMap::const_iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    res = (uint32_t)iter->second->status == (uint32_t)ledger::WalletStatus::VERIFIED ||
          (uint32_t)iter->second->status == (uint32_t)ledger::WalletStatus::CONNECTED;
  }

  return res;
}

void HuhiRewardsNativeWorker::IncludeInAutoContribution(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId, bool exclude) {
  PublishersInfoMap::iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    if (exclude) {
      iter->second->excluded = ledger::PublisherExclude::EXCLUDED;
    } else {
      iter->second->excluded = ledger::PublisherExclude::INCLUDED;
    }
    if (huhi_rewards_service_) {
      huhi_rewards_service_->SetPublisherExclude(iter->second->id, exclude);
    }
  }
}

void HuhiRewardsNativeWorker::RemovePublisherFromMap(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId) {
  PublishersInfoMap::const_iterator iter(map_publishers_info_.find(tabId));
  if (iter != map_publishers_info_.end()) {
    map_publishers_info_.erase(iter);
  }
}

void HuhiRewardsNativeWorker::OnWalletInitialized(huhi_rewards::RewardsService* rewards_service,
        int32_t error_code) {
  JNIEnv* env = base::android::AttachCurrentThread();
  
  Java_HuhiRewardsNativeWorker_OnWalletInitialized(env,
        weak_java_huhi_rewards_native_worker_.get(env), error_code);
}

void HuhiRewardsNativeWorker::OnWalletProperties(huhi_rewards::RewardsService* rewards_service,
        int error_code, 
        std::unique_ptr<huhi_rewards::WalletProperties> wallet_properties) {
  if (wallet_properties) {
    wallet_properties_ = *wallet_properties;
  }
  if (error_code == 0) {
    rewards_service->FetchBalance(
        base::Bind(
            &HuhiRewardsNativeWorker::OnBalance,
            weak_factory_.GetWeakPtr()));
  } else {
    JNIEnv* env = base::android::AttachCurrentThread();
    Java_HuhiRewardsNativeWorker_OnWalletProperties(env,
          weak_java_huhi_rewards_native_worker_.get(env), error_code);
  }
}

void HuhiRewardsNativeWorker::OnBalance(
    int32_t result,
    std::unique_ptr<huhi_rewards::Balance> balance) {
  if (result == 0 && balance) {
    balance_ = *balance;
  }

  JNIEnv* env = base::android::AttachCurrentThread();
  Java_HuhiRewardsNativeWorker_OnWalletProperties(env,
        weak_java_huhi_rewards_native_worker_.get(env), 0);
}

double HuhiRewardsNativeWorker::GetWalletBalance(JNIEnv* env,
    const base::android::JavaParamRef<jobject>& obj) {
  return balance_.total;
}

double HuhiRewardsNativeWorker::GetWalletRate(JNIEnv* env,
    const base::android::JavaParamRef<jobject>& obj,
    const base::android::JavaParamRef<jstring>& rate) {
  std::map<std::string, double>::const_iterator iter = balance_.rates.find(
    base::android::ConvertJavaStringToUTF8(env, rate));
  if (iter != balance_.rates.end()) {
    return iter->second;
  }

  return 0.0;
}

void HuhiRewardsNativeWorker::WalletExist(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& jcaller) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->IsWalletCreated(
      base::Bind(&HuhiRewardsNativeWorker::OnIsWalletCreated,
          weak_factory_.GetWeakPtr()));
  }
}

void HuhiRewardsNativeWorker::FetchGrants(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->FetchGrants(std::string(), std::string());
  }
}

void HuhiRewardsNativeWorker::OnIsWalletCreated(bool created) {
  JNIEnv* env = base::android::AttachCurrentThread();
  Java_HuhiRewardsNativeWorker_OnIsWalletCreated(env,
        weak_java_huhi_rewards_native_worker_.get(env), created);
}

void HuhiRewardsNativeWorker::GetCurrentBalanceReport(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj) {
  if (huhi_rewards_service_) {
    return huhi_rewards_service_->GetCurrentBalanceReport();
  }
}

void HuhiRewardsNativeWorker::OnGetCurrentBalanceReport(
        huhi_rewards::RewardsService* rewards_service,
        const huhi_rewards::BalanceReport& balance_report) {
  std::vector<std::string> values;
  values.push_back(balance_report.deposits);
  values.push_back(balance_report.grants);
  values.push_back(balance_report.earning_from_ads);
  values.push_back(balance_report.auto_contribute);
  values.push_back(balance_report.recurring_donation);
  values.push_back(balance_report.one_time_donation);
  values.push_back(balance_report.total);

  JNIEnv* env = base::android::AttachCurrentThread();
  base::android::ScopedJavaLocalRef<jobjectArray> java_array =
      base::android::ToJavaArrayOfStrings(env, values);

  Java_HuhiRewardsNativeWorker_OnGetCurrentBalanceReport(env,
        weak_java_huhi_rewards_native_worker_.get(env), java_array);
}

void HuhiRewardsNativeWorker::Donate(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& publisher_key, 
        int amount, bool recurring) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->OnTip(base::android::ConvertJavaStringToUTF8(env, publisher_key),
      amount, recurring);
  }
}

void HuhiRewardsNativeWorker::GetAllNotifications(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj) {
  huhi_rewards::RewardsNotificationService* notification_service =
    huhi_rewards_service_->GetNotificationService();
  if (notification_service) {
    notification_service->GetNotifications();
  }
}

void HuhiRewardsNativeWorker::DeleteNotification(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& notification_id) {
  huhi_rewards::RewardsNotificationService* notification_service =
    huhi_rewards_service_->GetNotificationService();
  if (notification_service) {
    notification_service->DeleteNotification(
      base::android::ConvertJavaStringToUTF8(env, notification_id));
  }
}

void HuhiRewardsNativeWorker::GetGrant(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& promotionId) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->GetGrantViaSafetynetCheck(base::android::ConvertJavaStringToUTF8(env, promotionId));
  }
}

int HuhiRewardsNativeWorker::GetCurrentGrantsCount(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj) {
  return wallet_properties_.grants.size();
}

base::android::ScopedJavaLocalRef<jobjectArray> HuhiRewardsNativeWorker::GetCurrentGrant(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj,
        int position) {
  if ((size_t)position > wallet_properties_.grants.size() - 1) {
    return base::android::ScopedJavaLocalRef<jobjectArray>();
  }
  std::vector<std::string> values;
  values.push_back(wallet_properties_.grants[position].probi);
  values.push_back(std::to_string(wallet_properties_.grants[position].expiryTime));
  values.push_back(wallet_properties_.grants[position].type);

  return base::android::ToJavaArrayOfStrings(env, values);
}

void HuhiRewardsNativeWorker::GetPendingContributionsTotal(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->GetPendingContributionsTotalUI(base::Bind(
          &HuhiRewardsNativeWorker::OnGetPendingContributionsTotal,
          weak_factory_.GetWeakPtr()));
  }
}

void HuhiRewardsNativeWorker::GetRecurringDonations(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->GetRecurringTipsUI(base::Bind(
          &HuhiRewardsNativeWorker::OnGetRecurringTips,
          weak_factory_.GetWeakPtr()));
  }
}

void HuhiRewardsNativeWorker::OnGetRecurringTips(
        std::unique_ptr<huhi_rewards::ContentSiteList> list) {
  map_recurrent_publishers_.clear();
  if (list) {
    for (size_t i = 0; i < list->size(); i++) {
      map_recurrent_publishers_[(*list)[i].id] = (*list)[i];
    }
  }

  JNIEnv* env = base::android::AttachCurrentThread();
  Java_HuhiRewardsNativeWorker_OnRecurringDonationUpdated(env,
        weak_java_huhi_rewards_native_worker_.get(env));
}

bool HuhiRewardsNativeWorker::IsCurrentPublisherInRecurrentDonations(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& publisher) {
  return map_recurrent_publishers_.find(base::android::ConvertJavaStringToUTF8(env, publisher)) !=
    map_recurrent_publishers_.end();
}


void HuhiRewardsNativeWorker::GetAutoContributeProps(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->GetAutoContributeProps(base::Bind(
            &HuhiRewardsNativeWorker::OnGetAutoContributeProps,
            weak_factory_.GetWeakPtr()));
  }
}


void HuhiRewardsNativeWorker::OnGetAutoContributeProps(
        std::unique_ptr<huhi_rewards::AutoContributeProps> props) {
  if (props) {
    auto_contrib_properties_ = *props;
  }

  JNIEnv* env = base::android::AttachCurrentThread();
  Java_HuhiRewardsNativeWorker_OnGetAutoContributeProps(env,
                                                               weak_java_huhi_rewards_native_worker_.get(env));
}

bool HuhiRewardsNativeWorker::IsAutoContributeEnabled(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj){
  return auto_contrib_properties_.enabled_contribute;
}

void HuhiRewardsNativeWorker::GetReconcileStamp(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj){
  if (huhi_rewards_service_) {
    huhi_rewards_service_->GetReconcileStamp(base::Bind(
            &HuhiRewardsNativeWorker::OnGetGetReconcileStamp,
            weak_factory_.GetWeakPtr()));
  }
}

void HuhiRewardsNativeWorker::ResetTheWholeState(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj) {
  auto* ads_service_ = huhi_ads::AdsServiceFactory::GetForProfile(
    ProfileManager::GetActiveUserProfile()->GetOriginalProfile());
  if (ads_service_) {
    ads_service_->ResetTheWholeState(base::Bind(
            &HuhiRewardsNativeWorker::OnAdsResetTheWholeState,
            weak_factory_.GetWeakPtr()));
  } else {
    OnAdsResetTheWholeState(true);
  }
}

void HuhiRewardsNativeWorker::OnAdsResetTheWholeState(bool sucess) {
  if (sucess && huhi_rewards_service_) {
    huhi_rewards_service_->ResetTheWholeState(base::Bind(
            &HuhiRewardsNativeWorker::OnResetTheWholeState,
            weak_factory_.GetWeakPtr()));
  } else {
    JNIEnv* env = base::android::AttachCurrentThread();

    Java_HuhiRewardsNativeWorker_OnResetTheWholeState(env,
            weak_java_huhi_rewards_native_worker_.get(env), sucess);
  }
}

void HuhiRewardsNativeWorker::OnResetTheWholeState(bool sucess) {
  JNIEnv* env = base::android::AttachCurrentThread();

  Java_HuhiRewardsNativeWorker_OnResetTheWholeState(env,
          weak_java_huhi_rewards_native_worker_.get(env), sucess);
}

double HuhiRewardsNativeWorker::GetPublisherRecurrentDonationAmount(JNIEnv* env,
                                              const base::android::JavaParamRef<jobject>& obj,
                                              const base::android::JavaParamRef<jstring>& publisher){
  double amount (0.0);
  auto it = map_recurrent_publishers_.find(base::android::ConvertJavaStringToUTF8(env, publisher));
  if ( it != map_recurrent_publishers_.end() ){
    // for Recurrent Donations, the amount is stored in ContentSite::percentage
    amount = it->second.percentage;
  }
  return  amount;
}

void HuhiRewardsNativeWorker::RemoveRecurring(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
    const base::android::JavaParamRef<jstring>& publisher){
  if (huhi_rewards_service_) {
      huhi_rewards_service_->RemoveRecurringTipUI(base::android::ConvertJavaStringToUTF8(env, publisher));
  }
}


void HuhiRewardsNativeWorker::OnGetGetReconcileStamp( uint64_t timestamp){
  JNIEnv* env = base::android::AttachCurrentThread();

  Java_HuhiRewardsNativeWorker_OnGetReconcileStamp(env,
          weak_java_huhi_rewards_native_worker_.get(env), timestamp);
}


void HuhiRewardsNativeWorker::OnGetPendingContributionsTotal(double amount) {
  JNIEnv* env = base::android::AttachCurrentThread();

  Java_HuhiRewardsNativeWorker_OnGetPendingContributionsTotal(env,
        weak_java_huhi_rewards_native_worker_.get(env), amount);
}

void HuhiRewardsNativeWorker::OnNotificationAdded(
      huhi_rewards::RewardsNotificationService* rewards_notification_service,
      const huhi_rewards::RewardsNotificationService::RewardsNotification& notification) {
  JNIEnv* env = base::android::AttachCurrentThread();

  Java_HuhiRewardsNativeWorker_OnNotificationAdded(env,
        weak_java_huhi_rewards_native_worker_.get(env),
        base::android::ConvertUTF8ToJavaString(env, notification.id_), 
        notification.type_,
        notification.timestamp_,
        base::android::ToJavaArrayOfStrings(env, notification.args_));
}

void HuhiRewardsNativeWorker::OnGetAllNotifications(
      huhi_rewards::RewardsNotificationService* rewards_notification_service,
      const huhi_rewards::RewardsNotificationService::RewardsNotificationsList&
          notifications_list) {
  JNIEnv* env = base::android::AttachCurrentThread();
  
  // Notify about notifications count
  Java_HuhiRewardsNativeWorker_OnNotificationsCount(env,
        weak_java_huhi_rewards_native_worker_.get(env),
        notifications_list.size());

  huhi_rewards::RewardsNotificationService::RewardsNotificationsList::const_iterator iter =
    std::max_element(notifications_list.begin(), notifications_list.end(),
      [](const huhi_rewards::RewardsNotificationService::RewardsNotification& notification_a,
        const huhi_rewards::RewardsNotificationService::RewardsNotification& notification_b) {
      return notification_a.timestamp_ > notification_b.timestamp_;
    });

  if (iter != notifications_list.end()) {
    Java_HuhiRewardsNativeWorker_OnGetLatestNotification(env,
        weak_java_huhi_rewards_native_worker_.get(env),
        base::android::ConvertUTF8ToJavaString(env, iter->id_), 
        iter->type_,
        iter->timestamp_,
        base::android::ToJavaArrayOfStrings(env, iter->args_));
  }
}

void HuhiRewardsNativeWorker::OnNotificationDeleted(
      huhi_rewards::RewardsNotificationService* rewards_notification_service,
      const huhi_rewards::RewardsNotificationService::RewardsNotification& notification) {
  JNIEnv* env = base::android::AttachCurrentThread();

  Java_HuhiRewardsNativeWorker_OnNotificationDeleted(env,
        weak_java_huhi_rewards_native_worker_.get(env),
        base::android::ConvertUTF8ToJavaString(env, notification.id_));
}

void HuhiRewardsNativeWorker::OnGrant(huhi_rewards::RewardsService* rewards_service,
      unsigned int result, huhi_rewards::Grant grant) {
  // TODO what do we need to do here? We receive notification about deletion
}
void HuhiRewardsNativeWorker::OnGrantFinish(huhi_rewards::RewardsService* rewards_service,
      unsigned int result, huhi_rewards::Grant grant) {
  JNIEnv* env = base::android::AttachCurrentThread();

  Java_HuhiRewardsNativeWorker_OnGrantFinish(env,
        weak_java_huhi_rewards_native_worker_.get(env), result);
}

void HuhiRewardsNativeWorker::SetRewardsMainEnabled(JNIEnv* env,
      const base::android::JavaParamRef<jobject>& obj, bool enabled) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->SetRewardsMainEnabled(enabled);
  }
}

void HuhiRewardsNativeWorker::OnRewardsMainEnabled(huhi_rewards::RewardsService* rewards_service,
    bool rewards_main_enabled) {
  JNIEnv* env = base::android::AttachCurrentThread();

  Java_HuhiRewardsNativeWorker_OnRewardsMainEnabled(env,
        weak_java_huhi_rewards_native_worker_.get(env), rewards_main_enabled);
}

void HuhiRewardsNativeWorker::GetRewardsMainEnabled(JNIEnv* env,
      const base::android::JavaParamRef<jobject>& obj) {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->GetRewardsMainEnabled(base::Bind(
      &HuhiRewardsNativeWorker::OnGetRewardsMainEnabled,
      base::Unretained(this)));
  }
}

void HuhiRewardsNativeWorker::OnGetRewardsMainEnabled(
    bool enabled) {
  JNIEnv* env = base::android::AttachCurrentThread();

  Java_HuhiRewardsNativeWorker_OnGetRewardsMainEnabled(env,
        weak_java_huhi_rewards_native_worker_.get(env), enabled);
}


static void JNI_HuhiRewardsNativeWorker_Init(JNIEnv* env, const
    base::android::JavaParamRef<jobject>& jcaller) {
  new HuhiRewardsNativeWorker(env, jcaller);
}

int HuhiRewardsNativeWorker::GetAdsPerHour(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj) {
  auto* ads_service_ = huhi_ads::AdsServiceFactory::GetForProfile(
    ProfileManager::GetActiveUserProfile()->GetOriginalProfile());
  if (!ads_service_) {
    return DEFAULT_ADS_PER_HOUR;
  }
  return ads_service_->GetAdsPerHour();
}

void HuhiRewardsNativeWorker::SetAdsPerHour(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
    jint value) {
  auto* ads_service_ = huhi_ads::AdsServiceFactory::GetForProfile(
    ProfileManager::GetActiveUserProfile()->GetOriginalProfile());
  if (!ads_service_) {
    return;
  }
  ads_service_->SetAdsPerHour(value);
}

int HuhiRewardsNativeWorker::GetAdsPerDay(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj) {
  auto* ads_service_ = huhi_ads::AdsServiceFactory::GetForProfile(
    ProfileManager::GetActiveUserProfile()->GetOriginalProfile());
  if (!ads_service_) {
    return DEFAULT_ADS_PER_DAY;
  }
  return ads_service_->GetAdsPerDay();
}

void HuhiRewardsNativeWorker::SetAdsPerDay(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
    jint value) {
  auto* ads_service_ = huhi_ads::AdsServiceFactory::GetForProfile(
    ProfileManager::GetActiveUserProfile()->GetOriginalProfile());
  if (!ads_service_) {
    return;
  }
  ads_service_->SetAdsPerDay(value);
}

}
}
