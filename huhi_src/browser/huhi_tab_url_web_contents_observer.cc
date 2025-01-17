/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "huhi_src/browser/huhi_tab_url_web_contents_observer.h"

#include "chrome/browser/content_settings/host_content_settings_map_factory.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/common/renderer_configuration.mojom.h"
#include "components/content_settings/core/browser/host_content_settings_map.h"
#include "content/browser/frame_host/frame_tree_node.h"
#include "content/browser/frame_host/navigator.h"
#include "content/public/browser/navigation_entry.h"
#include "content/public/browser/navigation_handle.h"
#include "content/public/browser/render_frame_host.h"
#include "content/public/browser/render_process_host.h"
#include "content/public/browser/web_contents.h"
#include "content/public/browser/web_contents_user_data.h"

namespace {

// Content Settings are only sent to the main frame currently.
// Chrome may fix this at some point, but for now we do this as a work-around.
// You can verify if this is fixed by running the following test:
// npm run test -- huhi_browser_tests --filter=HuhiContentSettingsObserverBrowserTest.*
// Chrome seems to also have a bug with RenderFrameHostChanged not updating the content settings
// so this is fixed here too. That case is coveredd in tests by:
// npm run test -- huhi_browser_tests --filter=HuhiContentSettingsObserverBrowserTest.*
void UpdateContentSettingsToRendererFrames(content::WebContents* web_contents) {
  for (content::RenderFrameHost* frame : web_contents->GetAllFrames()) {
    Profile* profile =
        Profile::FromBrowserContext(web_contents->GetBrowserContext());
    const HostContentSettingsMap* map =
        HostContentSettingsMapFactory::GetForProfile(profile);
    RendererContentSettingRules rules;
    GetRendererContentSettingRules(map, &rules);
    IPC::ChannelProxy* channel =
        frame->GetProcess()->GetChannel();
    // channel might be NULL in tests.
    if (channel) {
      chrome::mojom::RendererConfigurationAssociatedPtr rc_interface;
      channel->GetRemoteAssociatedInterface(&rc_interface);
      rc_interface->SetContentSettingRules(rules);
    }
  }
}

}  // namespace

using content::RenderFrameHost;
using content::WebContents;

namespace huhi {

base::Lock HuhiTabUrlWebContentsObserver::frame_data_map_lock_;
std::map<HuhiTabUrlWebContentsObserver::RenderFrameIdKey, TabProperties>
    HuhiTabUrlWebContentsObserver::frame_key_to_tab_url_;
std::map<int, TabProperties>
    HuhiTabUrlWebContentsObserver::frame_tree_node_id_to_tab_url_;

HuhiTabUrlWebContentsObserver::RenderFrameIdKey::RenderFrameIdKey()
    : render_process_id(content::ChildProcessHost::kInvalidUniqueID),
      frame_routing_id(MSG_ROUTING_NONE) {}

HuhiTabUrlWebContentsObserver::RenderFrameIdKey::RenderFrameIdKey(
    int render_process_id,
    int frame_routing_id)
    : render_process_id(render_process_id),
      frame_routing_id(frame_routing_id) {}

bool HuhiTabUrlWebContentsObserver::RenderFrameIdKey::operator<(
    const RenderFrameIdKey& other) const {
  return std::tie(render_process_id, frame_routing_id) <
         std::tie(other.render_process_id, other.frame_routing_id);
}

bool HuhiTabUrlWebContentsObserver::RenderFrameIdKey::operator==(
    const RenderFrameIdKey& other) const {
  return render_process_id == other.render_process_id &&
         frame_routing_id == other.frame_routing_id;
}

HuhiTabUrlWebContentsObserver::~HuhiTabUrlWebContentsObserver() {
}

HuhiTabUrlWebContentsObserver::HuhiTabUrlWebContentsObserver(
    WebContents* web_contents)
    : WebContentsObserver(web_contents) {
}

void HuhiTabUrlWebContentsObserver::RenderFrameCreated(
    RenderFrameHost* rfh) {
  WebContents* web_contents = WebContents::FromRenderFrameHost(rfh);
  if (web_contents) {
    UpdateContentSettingsToRendererFrames(web_contents);

    base::AutoLock lock(frame_data_map_lock_);
    const RenderFrameIdKey key(rfh->GetProcess()->GetID(), rfh->GetRoutingID());
    TabProperties tab_prop(true);
    tab_prop.tab_url = web_contents->GetURL();
    tab_prop.is_incognito = web_contents->GetBrowserContext() ?
                              web_contents->GetBrowserContext()->IsOffTheRecord() :
                              false;
    frame_key_to_tab_url_[key] = tab_prop;
    frame_tree_node_id_to_tab_url_[rfh->GetFrameTreeNodeId()] = tab_prop;
  }
}

void HuhiTabUrlWebContentsObserver::RenderFrameDeleted(
    RenderFrameHost* rfh) {
  base::AutoLock lock(frame_data_map_lock_);
  const RenderFrameIdKey key(rfh->GetProcess()->GetID(), rfh->GetRoutingID());
  frame_key_to_tab_url_.erase(key);
  frame_tree_node_id_to_tab_url_.erase(rfh->GetFrameTreeNodeId());
}

void HuhiTabUrlWebContentsObserver::RenderFrameHostChanged(
    RenderFrameHost* old_host, RenderFrameHost* new_host) {
  if (old_host) {
    RenderFrameDeleted(old_host);
  }
  if (new_host) {
    RenderFrameCreated(new_host);
  }
}

void HuhiTabUrlWebContentsObserver::DidFinishNavigation(
    content::NavigationHandle* navigation_handle) {
  RenderFrameHost* main_frame = web_contents()->GetMainFrame();
  if (!web_contents() || !main_frame) {
    return;
  }
  int process_id = main_frame->GetProcess()->GetID();
  int routing_id = main_frame->GetRoutingID();
  int tree_node_id = main_frame->GetFrameTreeNodeId();

  base::AutoLock lock(frame_data_map_lock_);
  TabProperties tab_prop(true);
  tab_prop.tab_url = web_contents()->GetURL();
  tab_prop.is_incognito = web_contents()->GetBrowserContext() ?
                            web_contents()->GetBrowserContext()->IsOffTheRecord() :
                            false;
  frame_key_to_tab_url_[{process_id, routing_id}] = tab_prop;
  frame_tree_node_id_to_tab_url_[tree_node_id] = tab_prop;
}

// static
TabProperties HuhiTabUrlWebContentsObserver::GetTabPropertiesFromRenderFrameInfo(
    int render_process_id, int render_frame_id, int render_frame_tree_node_id) {
  base::AutoLock lock(frame_data_map_lock_);
  if (-1 != render_process_id && -1 != render_frame_id) {
    auto iter = frame_key_to_tab_url_.find({render_process_id,
                                            render_frame_id});
    if (iter != frame_key_to_tab_url_.end()) {
      return iter->second;
    }
  }
  if (-1 != render_frame_tree_node_id) {
    auto iter2 = frame_tree_node_id_to_tab_url_.find(render_frame_tree_node_id);
    if (iter2 != frame_tree_node_id_to_tab_url_.end()) {
      return iter2->second;
    }
  }

  return TabProperties(false);
}

WEB_CONTENTS_USER_DATA_KEY_IMPL(HuhiTabUrlWebContentsObserver)

}  // namespace huhi
