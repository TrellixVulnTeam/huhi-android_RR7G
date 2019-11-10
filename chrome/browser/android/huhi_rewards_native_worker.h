/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef HUHI_REWARDS_NATIVE_WORKER_H_
#define HUHI_REWARDS_NATIVE_WORKER_H_

#include <jni.h>
#include <memory>
#include <map>
#include "base/android/jni_weak_ref.h"
#include "base/memory/weak_ptr.h"
#include "huhi/components/huhi_rewards/browser/balance.h"
#include "huhi/components/huhi_rewards/browser/balance_report.h"
#include "huhi/components/huhi_rewards/browser/rewards_service_observer.h"
#include "huhi/components/huhi_rewards/browser/rewards_notification_service_observer.h"
#include "huhi/components/huhi_rewards/browser/rewards_service_private_observer.h"
#include "huhi/components/huhi_rewards/browser/wallet_properties.h"
#include "huhi/vendor/bat-native-ledger/include/bat/ledger/mojom_structs.h"
#include "huhi/components/huhi_rewards/browser/auto_contribution_props.h"
#include "huhi/components/huhi_rewards/browser/content_site.h"

namespace huhi_rewards {
  class RewardsService;
}

namespace chrome {
namespace android {

typedef std::map<uint64_t, ledger::PublisherInfoPtr> PublishersInfoMap;

class HuhiRewardsNativeWorker : public huhi_rewards::RewardsServiceObserver,
    public huhi_rewards::RewardsServicePrivateObserver,
    public huhi_rewards::RewardsNotificationServiceObserver {
public:
    HuhiRewardsNativeWorker(JNIEnv* env, const base::android::JavaRef<jobject>& obj);
    ~HuhiRewardsNativeWorker() override;

    void Destroy(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller);

    void CreateWallet(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller);

    void OnCreateWallet(int32_t result);

    void WalletExist(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller);

    void GetWalletProperties(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller);

    void GetPublisherInfo(JNIEnv* env, const
        base::android::JavaParamRef<jobject>& jcaller, int tabId,
        const base::android::JavaParamRef<jstring>& host);

    double GetWalletBalance(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    double GetWalletRate(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& rate);

    base::android::ScopedJavaLocalRef<jstring> GetPublisherURL(JNIEnv* env, 
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId);

    base::android::ScopedJavaLocalRef<jstring> GetPublisherFavIconURL(JNIEnv* env, 
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId);

    base::android::ScopedJavaLocalRef<jstring> GetPublisherName(JNIEnv* env, 
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId);

    base::android::ScopedJavaLocalRef<jstring> GetPublisherId(JNIEnv* env, 
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId);

    int GetPublisherPercent(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        uint64_t tabId);

    bool GetPublisherExcluded(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        uint64_t tabId);

    bool GetPublisherVerified(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        uint64_t tabId);

    void GetCurrentBalanceReport(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    void IncludeInAutoContribution(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        uint64_t tabId, bool exclude);

    void RemovePublisherFromMap(JNIEnv* env, 
        const base::android::JavaParamRef<jobject>& obj, uint64_t tabId);

    void Donate(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& publisher_key, int amount, bool recurring);

    void GetAllNotifications(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    void DeleteNotification(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& notification_id);

    void GetGrant(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& promotionId);

    int GetCurrentGrantsCount(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    base::android::ScopedJavaLocalRef<jobjectArray> GetCurrentGrant(JNIEnv* env, 
        const base::android::JavaParamRef<jobject>& obj,
        int position);

    void GetPendingContributionsTotal(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    void GetRecurringDonations(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    bool IsCurrentPublisherInRecurrentDonations(JNIEnv* env, 
        const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& publisher);

    void SetRewardsMainEnabled(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        bool enabled);
    void GetRewardsMainEnabled(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    void GetAutoContributeProps(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    bool IsAutoContributeEnabled(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    void GetReconcileStamp(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    void ResetTheWholeState(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    double GetPublisherRecurrentDonationAmount(JNIEnv* env,
        const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& publisher);

    void RemoveRecurring(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        const base::android::JavaParamRef<jstring>& publisher);

    void FetchGrants(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    int GetAdsPerHour(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    void SetAdsPerHour(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        jint value);

    int GetAdsPerDay(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj);

    void SetAdsPerDay(JNIEnv* env, const base::android::JavaParamRef<jobject>& obj,
        jint value);

    void OnAdsResetTheWholeState(bool sucess);

    void OnResetTheWholeState(bool sucess);

    void OnGetGetReconcileStamp( uint64_t timestamp);

    void OnGetAutoContributeProps( std::unique_ptr<huhi_rewards::AutoContributeProps> info);

    void OnGetRewardsMainEnabled(bool enabled);

    void OnGetPendingContributionsTotal(double amount);

    void OnIsWalletCreated(bool created);

    void OnWalletInitialized(huhi_rewards::RewardsService* rewards_service,
        int32_t error_code) override;

    void OnWalletProperties(huhi_rewards::RewardsService* rewards_service,
        int error_code, 
        std::unique_ptr<huhi_rewards::WalletProperties> wallet_properties) override;

    void OnPanelPublisherInfo(
        huhi_rewards::RewardsService* rewards_service,
        int error_code,
        const ledger::PublisherInfo* info,
        uint64_t tabId) override;

    void OnGetCurrentBalanceReport(huhi_rewards::RewardsService* rewards_service,
        const huhi_rewards::BalanceReport& balance_report) override;

    void OnNotificationAdded(
      huhi_rewards::RewardsNotificationService* rewards_notification_service,
      const huhi_rewards::RewardsNotificationService::RewardsNotification& notification) override;

    void OnGetAllNotifications(
      huhi_rewards::RewardsNotificationService* rewards_notification_service,
      const huhi_rewards::RewardsNotificationService::RewardsNotificationsList&
          notifications_list) override;

    void OnNotificationDeleted(
      huhi_rewards::RewardsNotificationService* rewards_notification_service,
      const huhi_rewards::RewardsNotificationService::RewardsNotification& notification) override;

    void OnGrant(huhi_rewards::RewardsService* rewards_service, unsigned int result,
        huhi_rewards::Grant grant) override;

    void OnGrantFinish(huhi_rewards::RewardsService* rewards_service, unsigned int result,
        huhi_rewards::Grant grant) override;

    void OnGetRecurringTips(std::unique_ptr<huhi_rewards::ContentSiteList> list);

    void OnRewardsMainEnabled(huhi_rewards::RewardsService* rewards_service,
        bool rewards_main_enabled) override;

private:
    void OnBalance(int32_t result, std::unique_ptr<::huhi_rewards::Balance> balance);
    JavaObjectWeakGlobalRef weak_java_huhi_rewards_native_worker_;
    huhi_rewards::RewardsService* huhi_rewards_service_;
    huhi_rewards::WalletProperties wallet_properties_;
    huhi_rewards::Balance balance_;
    huhi_rewards::AutoContributeProps auto_contrib_properties_;
    PublishersInfoMap map_publishers_info_; // <tabId, PublisherInfoPtr>
    std::map<std::string, huhi_rewards::ContentSite> map_recurrent_publishers_;      // <publisher, reconcile_stampt>
    std::map<std::string, std::string> addresses_;
    base::WeakPtrFactory<HuhiRewardsNativeWorker> weak_factory_;
};
}
}

#endif // HUHI_REWARDS_NATIVE_WORKER_H_
