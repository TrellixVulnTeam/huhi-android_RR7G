/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef HUHI_BROWSER_HUHI_TAB_URL_WEB_CONTENTS_OBSERVER_H_
#define HUHI_BROWSER_HUHI_TAB_URL_WEB_CONTENTS_OBSERVER_H_

#include "base/macros.h"
#include "base/synchronization/lock.h"
#include "content/public/browser/web_contents_observer.h"
#include "content/public/browser/web_contents_user_data.h"

namespace content {
class WebContents;
}

class PrefRegistrySimple;

namespace huhi {

struct TabProperties {
  TabProperties(bool valid = false) :
    is_incognito(false),
    is_valid(valid) {}
  void operator=(const TabProperties& src) {
    if (&src == this) return;
    tab_url = src.tab_url;
    is_incognito = src.is_incognito;
    is_valid = src.is_valid;
  }
  GURL tab_url;
  bool is_incognito;
  bool is_valid;
};

// Taken from huhi-core/components/huhi_shields/browser/huhi_shields_web_contents_observer.h
// TODO(alexeyb) on merge browser-android-tabs and huhi-core either keep this
// or re-use HuhiShieldsWebContentsObserver with a ton of
// `#if !defined(OS_ANDROID)`
class HuhiTabUrlWebContentsObserver : public content::WebContentsObserver,
    public content::WebContentsUserData<HuhiTabUrlWebContentsObserver> {
 public:
  HuhiTabUrlWebContentsObserver(content::WebContents*);
  ~HuhiTabUrlWebContentsObserver() override;

  static TabProperties GetTabPropertiesFromRenderFrameInfo(int render_process_id,
                                           int render_frame_id,
                                           int render_frame_tree_node_id);

 protected:
    // A set of identifiers that uniquely identifies a RenderFrame.
  struct RenderFrameIdKey {
    RenderFrameIdKey();
    RenderFrameIdKey(int render_process_id, int frame_routing_id);

    // The process ID of the renderer that contains the RenderFrame.
    int render_process_id;

    // The routing ID of the RenderFrame.
    int frame_routing_id;

    bool operator<(const RenderFrameIdKey& other) const;
    bool operator==(const RenderFrameIdKey& other) const;
  };

  // content::WebContentsObserver overrides.
  void RenderFrameCreated(content::RenderFrameHost* host) override;
  void RenderFrameDeleted(content::RenderFrameHost* render_frame_host) override;
  void RenderFrameHostChanged(content::RenderFrameHost* old_host,
                              content::RenderFrameHost* new_host) override;
  void DidFinishNavigation(
      content::NavigationHandle* navigation_handle) override;

  // TODO(iefremov): Refactor this away or at least put into base::NoDestructor.
  // Protects global maps below from being concurrently written on the UI thread
  // and read on the IO thread.
  static base::Lock frame_data_map_lock_;
  static std::map<RenderFrameIdKey, TabProperties> frame_key_to_tab_url_;
  static std::map<int, TabProperties> frame_tree_node_id_to_tab_url_;

 private:
  friend class content::WebContentsUserData<HuhiTabUrlWebContentsObserver>;
  WEB_CONTENTS_USER_DATA_KEY_DECL();
  DISALLOW_COPY_AND_ASSIGN(HuhiTabUrlWebContentsObserver);
};

}  // namespace huhi

#endif  // HUHI_BROWSER_HUHI_TAB_URL_WEB_CONTENTS_OBSERVER_H_
