// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "ios/chrome/browser/ui/overlays/web_content_area/app_launcher/app_launcher_alert_overlay_mediator.h"

#include "components/strings/grit/components_strings.h"
#import "ios/chrome/browser/overlays/public/overlay_request.h"
#import "ios/chrome/browser/overlays/public/overlay_response.h"
#import "ios/chrome/browser/overlays/public/web_content_area/app_launcher_alert_overlay.h"
#import "ios/chrome/browser/ui/alert_view_controller/alert_action.h"
#import "ios/chrome/browser/ui/alert_view_controller/test/fake_alert_consumer.h"
#include "ios/chrome/grit/ios_strings.h"
#include "testing/gtest_mac.h"
#include "testing/platform_test.h"
#include "ui/base/l10n/l10n_util.h"

#if !defined(__has_feature) || !__has_feature(objc_arc)
#error "This file requires ARC support."
#endif

class AppLauncherAlertOverlayMediatorTest : public PlatformTest {
 protected:
  AppLauncherAlertOverlayMediatorTest()
      : consumer_([[FakeAlertConsumer alloc] init]) {
    UpdateConsumer();
  }

  FakeAlertConsumer* consumer() const { return consumer_; }

  // Setter for whether the test is for a repeated app launch request.
  void set_is_repeated_request(bool is_repeated_request) {
    if (is_repeated_request_ == is_repeated_request)
      return;
    is_repeated_request_ = is_repeated_request;
    UpdateConsumer();
  }

 private:
  // Instantiates |request_| with an OverlayRequest configured with an
  // AppLauncherAlertOverlayRequestConfig set up using |is_repeated_request_|.
  // Resets |mediator_| to a new AppLauncherAlertOverlayMediator created with
  // |request_| and sets its consumer to |consumer_|.
  void UpdateConsumer() {
    request_ =
        OverlayRequest::CreateWithConfig<AppLauncherAlertOverlayRequestConfig>(
            is_repeated_request_);
    mediator_ = [[AppLauncherAlertOverlayMediator alloc]
        initWithRequest:request_.get()];
    mediator_.consumer = consumer_;
  }

  FakeAlertConsumer* consumer_;
  bool is_repeated_request_ = false;
  std::unique_ptr<OverlayRequest> request_;
  AppLauncherAlertOverlayMediator* mediator_ = nil;
};

// Tests that the consumer values are set correctly for the first app launch
// request.
TEST_F(AppLauncherAlertOverlayMediatorTest, FirstRequestAlertSetup) {
  EXPECT_NSEQ(l10n_util::GetNSString(IDS_IOS_OPEN_IN_ANOTHER_APP),
              consumer().message);
  ASSERT_EQ(2U, consumer().actions.count);
  EXPECT_EQ(UIAlertActionStyleDefault, consumer().actions[0].style);
  EXPECT_NSEQ(
      l10n_util::GetNSString(IDS_IOS_APP_LAUNCHER_OPEN_APP_BUTTON_LABEL),
      consumer().actions[0].title);
  EXPECT_EQ(UIAlertActionStyleCancel, consumer().actions[1].style);
  EXPECT_NSEQ(l10n_util::GetNSString(IDS_CANCEL), consumer().actions[1].title);
}

// Tests that the consumer values are set correctly for the repeated app launch
// requests.
TEST_F(AppLauncherAlertOverlayMediatorTest, RepeatedRequestAlertSetup) {
  set_is_repeated_request(true);
  EXPECT_NSEQ(l10n_util::GetNSString(IDS_IOS_OPEN_REPEATEDLY_ANOTHER_APP),
              consumer().message);
  ASSERT_EQ(2U, consumer().actions.count);
  EXPECT_EQ(UIAlertActionStyleDefault, consumer().actions[0].style);
  EXPECT_NSEQ(l10n_util::GetNSString(IDS_IOS_OPEN_REPEATEDLY_ANOTHER_APP_ALLOW),
              consumer().actions[0].title);
  EXPECT_EQ(UIAlertActionStyleCancel, consumer().actions[1].style);
  EXPECT_NSEQ(l10n_util::GetNSString(IDS_CANCEL), consumer().actions[1].title);
}
