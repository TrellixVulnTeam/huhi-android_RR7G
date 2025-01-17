// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "base/at_exit.h"
#include "chromecast/public/media/media_capabilities_shlib.h"

namespace chromecast {
namespace media {

namespace {
base::AtExitManager g_at_exit_manager;
}  // namespace

bool MediaCapabilitiesShlib::IsSupportedAudioConfig(const AudioConfig& config) {
  switch (config.codec) {
    case kCodecPCM:
    case kCodecPCM_S16BE:
    case kCodecAAC:
    case kCodecMP3:
    case kCodecVorbis:
      return true;
    default:
      break;
  }
  return false;
}

}  // namespace media
}  // namespace chromecast
