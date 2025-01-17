// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chrome/utility/chrome_content_utility_client.h"

#include <stddef.h>
#include <utility>
#include <vector>

#include "base/bind.h"
#include "base/command_line.h"
#include "base/files/file_path.h"
#include "base/lazy_instance.h"
#include "base/memory/ref_counted.h"
#include "base/threading/sequenced_task_runner_handle.h"
#include "base/time/time.h"
#include "chrome/common/buildflags.h"
#include "chrome/utility/services.h"
#include "components/safe_browsing/buildflags.h"
#include "content/public/common/content_features.h"
#include "content/public/common/content_switches.h"
#include "content/public/common/service_manager_connection.h"
#include "content/public/common/simple_connection_filter.h"
#include "content/public/utility/utility_thread.h"
#include "extensions/buildflags/buildflags.h"
#include "services/service_manager/public/cpp/binder_registry.h"
#include "services/service_manager/sandbox/switches.h"
#include "ui/base/buildflags.h"

#if !defined(OS_ANDROID)
#include "services/network/url_request_context_builder_mojo.h"
#endif  // !defined(OS_ANDROID)

#if BUILDFLAG(ENABLE_PRINTING) && defined(OS_WIN)
#include "chrome/services/printing/pdf_to_emf_converter_factory.h"
#endif

#if BUILDFLAG(ENABLE_PRINT_PREVIEW) && defined(OS_WIN)
#include "chrome/utility/printing_handler.h"
#endif

#include "huhi/components/huhi_ads/browser/buildflags/buildflags.h"
#include "huhi/components/huhi_rewards/browser/buildflags/buildflags.h"

#if BUILDFLAG(HUHI_ADS_ENABLED)
#include "huhi/components/services/bat_ads/bat_ads_app.h"
#include "huhi/components/services/bat_ads/public/interfaces/bat_ads.mojom.h"
#endif

#if BUILDFLAG(HUHI_REWARDS_ENABLED)
#include "huhi/components/services/bat_ledger/bat_ledger_app.h"
#include "huhi/components/services/bat_ledger/public/interfaces/bat_ledger.mojom.h"
#endif

namespace {

#if BUILDFLAG(HUHI_ADS_ENABLED) || BUILDFLAG(HUHI_REWARDS_ENABLED)
void RunServiceAsyncThenTerminateProcess(
    std::unique_ptr<service_manager::Service> service) {
  service_manager::Service::RunAsyncUntilTermination(
      std::move(service),
      base::BindOnce([] { content::UtilityThread::Get()->ReleaseProcess(); }));
}
#endif

#if BUILDFLAG(HUHI_ADS_ENABLED)
std::unique_ptr<service_manager::Service> CreateBatAdsService(
    service_manager::mojom::ServiceRequest request) {
  return std::make_unique<bat_ads::BatAdsApp>(
      std::move(request));
}
#endif

#if BUILDFLAG(HUHI_REWARDS_ENABLED)
std::unique_ptr<service_manager::Service> CreateBatLedgerService(
    service_manager::mojom::ServiceRequest request) {
  return std::make_unique<bat_ledger::BatLedgerApp>(
      std::move(request));
}
#endif

}  // namespace

namespace {

base::LazyInstance<ChromeContentUtilityClient::NetworkBinderCreationCallback>::
    Leaky g_network_binder_creation_callback = LAZY_INSTANCE_INITIALIZER;

}  // namespace

ChromeContentUtilityClient::ChromeContentUtilityClient()
    : utility_process_running_elevated_(false) {
#if BUILDFLAG(ENABLE_PRINT_PREVIEW) && defined(OS_WIN)
  printing_handler_ = std::make_unique<printing::PrintingHandler>();
#endif
}

ChromeContentUtilityClient::~ChromeContentUtilityClient() = default;

void ChromeContentUtilityClient::UtilityThreadStarted() {
#if defined(OS_WIN)
  base::CommandLine* command_line = base::CommandLine::ForCurrentProcess();
  utility_process_running_elevated_ = command_line->HasSwitch(
      service_manager::switches::kNoSandboxAndElevatedPrivileges);
#endif

  content::ServiceManagerConnection* connection =
      content::ChildThread::Get()->GetServiceManagerConnection();

  // NOTE: Some utility process instances are not connected to the Service
  // Manager. Nothing left to do in that case.
  if (!connection)
    return;

  auto registry = std::make_unique<service_manager::BinderRegistry>();
  // If our process runs with elevated privileges, only add elevated Mojo
  // interfaces to the interface registry.
  if (!utility_process_running_elevated_) {
#if BUILDFLAG(ENABLE_PRINTING) && defined(OS_WIN)
    // TODO(crbug.com/798782): remove when the Cloud print chrome/service is
    // removed.
    registry->AddInterface(
        base::BindRepeating(printing::PdfToEmfConverterFactory::Create),
        base::ThreadTaskRunnerHandle::Get());
#endif
  }

  connection->AddConnectionFilter(
      std::make_unique<content::SimpleConnectionFilter>(std::move(registry)));
}

bool ChromeContentUtilityClient::OnMessageReceived(
    const IPC::Message& message) {
  if (utility_process_running_elevated_)
    return false;

#if BUILDFLAG(ENABLE_PRINT_PREVIEW) && defined(OS_WIN)
  if (printing_handler_->OnMessageReceived(message))
    return true;
#endif
  return false;
}

void ChromeContentUtilityClient::RegisterNetworkBinders(
    service_manager::BinderRegistry* registry) {
  if (g_network_binder_creation_callback.Get())
    g_network_binder_creation_callback.Get().Run(registry);
}

mojo::ServiceFactory*
ChromeContentUtilityClient::GetMainThreadServiceFactory() {
  if (utility_process_running_elevated_)
    return ::GetElevatedMainThreadServiceFactory();
  return ::GetMainThreadServiceFactory();
}

mojo::ServiceFactory* ChromeContentUtilityClient::GetIOThreadServiceFactory() {
  return ::GetIOThreadServiceFactory();
}

// static
void ChromeContentUtilityClient::SetNetworkBinderCreationCallback(
    const NetworkBinderCreationCallback& callback) {
  g_network_binder_creation_callback.Get() = callback;
}

bool ChromeContentUtilityClient::HandleServiceRequest(
    const std::string& service_name,
    service_manager::mojom::ServiceRequest request) {
#if BUILDFLAG(HUHI_ADS_ENABLED)
  if (service_name == bat_ads::mojom::kServiceName) {
    RunServiceAsyncThenTerminateProcess(
        CreateBatAdsService(std::move(request)));
    return true;
  }
#endif

#if BUILDFLAG(HUHI_REWARDS_ENABLED)
  if (service_name == bat_ledger::mojom::kServiceName) {
    RunServiceAsyncThenTerminateProcess(
        CreateBatLedgerService(std::move(request)));
    return true;
  }
#endif

  return false;
}