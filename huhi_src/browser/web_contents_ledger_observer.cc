/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "bat/ledger/ledger.h"
#include "huhi/components/huhi_rewards/browser/rewards_service.h"
#include "huhi/components/huhi_rewards/browser/rewards_service_factory.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/profiles/profile_manager.h"
#include "chrome/browser/sessions/session_tab_helper.h"
#include "chrome/browser/ui/browser_list.h"
#include "content/public/browser/navigation_handle.h"
#include "content/public/common/favicon_url.h"
#include "net/base/registry_controlled_domains/registry_controlled_domain.h"
#include "web_contents_ledger_observer.h"
#include "huhi_src/browser/web_contents_ledger_observer.h"
#include "content/public/browser/web_contents.h"
#include "content/public/browser/web_contents_user_data.h"
#include "content/public/common/resource_load_info.mojom.h"

using content::WebContents;

namespace huhi {

WebContentsLedgerObserver::WebContentsLedgerObserver(WebContents* web_contents)
    : WebContentsObserver(web_contents),
    web_contents_(web_contents),
    huhi_rewards_service_(nullptr),
    is_being_destroyed_(false) {
  bool incognito = web_contents->GetBrowserContext()->IsOffTheRecord();
  if (incognito == false) {
    huhi_rewards_service_ = huhi_rewards::RewardsServiceFactory::GetForProfile(
     ProfileManager::GetActiveUserProfile()->GetOriginalProfile());
  }
}

WebContentsLedgerObserver::~WebContentsLedgerObserver() {
}

void WebContentsLedgerObserver::OnVisibilityChanged(content::Visibility visibility) {
  if (!huhi_rewards_service_) {
    return;
  }

  if (content::Visibility::VISIBLE == visibility) {
    huhi_rewards_service_->OnShow(SessionTabHelper::IdForTab(web_contents_));
  } else if (content::Visibility::HIDDEN == visibility) {
    huhi_rewards_service_->OnHide(SessionTabHelper::IdForTab(web_contents_));
  }
}

void WebContentsLedgerObserver::WebContentsDestroyed() {
  if (huhi_rewards_service_) {
    huhi_rewards_service_->OnUnload(SessionTabHelper::IdForTab(web_contents_));
  }
  is_being_destroyed_ = true;
}

void WebContentsLedgerObserver::DidFinishLoad(content::RenderFrameHost* render_frame_host,
                           const GURL& validated_url) {
  if (!huhi_rewards_service_ || render_frame_host->GetParent()) {
    return;
  }

  huhi_rewards_service_->OnLoad(SessionTabHelper::IdForTab(web_contents_),
    validated_url);
}

void WebContentsLedgerObserver::DidUpdateFaviconURL(const std::vector<content::FaviconURL>& candidates) {
  for (size_t i = 0; i < candidates.size(); i++) {
    switch(candidates[i].icon_type) {
      case content::FaviconURL::IconType::kFavicon:
        return;
      default:
        break;
    }
  }
}

void WebContentsLedgerObserver::DidAttachInterstitialPage() {
  if (huhi_rewards_service_)
    huhi_rewards_service_->OnUnload(SessionTabHelper::IdForTab(web_contents_));
}

void WebContentsLedgerObserver::DidFinishNavigation(content::NavigationHandle* navigation_handle) {
  if (!huhi_rewards_service_ ||
      !navigation_handle->IsInMainFrame() ||
      !navigation_handle->HasCommitted() ||
      navigation_handle->IsDownload()) {
    return;
  }

  huhi_rewards_service_->OnUnload(SessionTabHelper::IdForTab(web_contents_));
}

void WebContentsLedgerObserver::ResourceLoadComplete(
    content::RenderFrameHost* render_frame_host,
    const content::GlobalRequestID& request_id,
    const content::mojom::ResourceLoadInfo& resource_load_info) {
  if (!huhi_rewards_service_ || !render_frame_host) {
    return;
  }

  if (resource_load_info.resource_type == content::ResourceType::kMedia ||
      resource_load_info.resource_type == content::ResourceType::kXhr ||
      resource_load_info.resource_type == content::ResourceType::kImage ||
      resource_load_info.resource_type == content::ResourceType::kScript) {

    // TODO fill first_party_url and referrer with actual values
    huhi_rewards_service_->OnXHRLoad(SessionTabHelper::IdForTab(web_contents_),
      resource_load_info.url, render_frame_host->GetLastCommittedURL(), 
      resource_load_info.referrer);
  }
}

bool WebContentsLedgerObserver::IsBeingDestroyed() {
  return is_being_destroyed_;
}

WEB_CONTENTS_USER_DATA_KEY_IMPL(WebContentsLedgerObserver)

}
