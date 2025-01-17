// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chromeos/services/secure_channel/client_connection_parameters_impl.h"

#include <memory>

#include "base/run_loop.h"
#include "base/test/task_environment.h"
#include "chromeos/services/secure_channel/fake_channel.h"
#include "chromeos/services/secure_channel/fake_client_connection_parameters.h"
#include "chromeos/services/secure_channel/fake_connection_delegate.h"
#include "testing/gtest/include/gtest/gtest.h"

namespace chromeos {

namespace secure_channel {

namespace {
const char kTestFeature[] = "testFeature";
}  // namespace

class SecureChannelClientConnectionParametersImplTest : public testing::Test {
 protected:
  SecureChannelClientConnectionParametersImplTest() = default;
  ~SecureChannelClientConnectionParametersImplTest() override = default;

  // testing::Test:
  void SetUp() override {
    fake_connection_delegate_ = std::make_unique<FakeConnectionDelegate>();
    auto fake_connection_delegate_interface_ptr =
        fake_connection_delegate_->GenerateInterfacePtr();
    fake_connection_delegate_proxy_ =
        fake_connection_delegate_interface_ptr.get();

    client_connection_parameters_ =
        ClientConnectionParametersImpl::Factory::Get()->BuildInstance(
            kTestFeature, std::move(fake_connection_delegate_interface_ptr));

    fake_observer_ = std::make_unique<FakeClientConnectionParametersObserver>();
    client_connection_parameters_->AddObserver(fake_observer_.get());
  }

  void TearDown() override {
    client_connection_parameters_->RemoveObserver(fake_observer_.get());
  }

  void DisconnectConnectionDelegatePtr() {
    base::RunLoop run_loop;
    fake_observer_->set_closure_for_next_callback(run_loop.QuitClosure());
    fake_connection_delegate_->DisconnectGeneratedPtrs();
    run_loop.Run();
  }

  void CallOnConnection(
      mojom::ChannelPtr channel,
      mojom::MessageReceiverRequest message_receiver_request) {
    base::RunLoop run_loop;
    fake_connection_delegate_->set_closure_for_next_delegate_callback(
        run_loop.QuitClosure());
    client_connection_parameters_->SetConnectionSucceeded(
        std::move(channel), std::move(message_receiver_request));
    run_loop.Run();
  }

  void CallOnConnectionAttemptFailure(
      mojom::ConnectionAttemptFailureReason reason) {
    base::RunLoop run_loop;
    fake_connection_delegate_->set_closure_for_next_delegate_callback(
        run_loop.QuitClosure());
    client_connection_parameters_->SetConnectionAttemptFailed(reason);
    run_loop.Run();
  }

  void VerifyStatus(bool expected_to_be_waiting_for_response,
                    bool expected_to_be_canceled) {
    EXPECT_EQ(expected_to_be_waiting_for_response,
              client_connection_parameters_->IsClientWaitingForResponse());
    EXPECT_EQ(expected_to_be_canceled,
              fake_observer_->has_connection_request_been_canceled());
  }

  const FakeConnectionDelegate* fake_connection_delegate() {
    return fake_connection_delegate_.get();
  }

 private:
  base::test::TaskEnvironment task_environment_;

  std::unique_ptr<FakeConnectionDelegate> fake_connection_delegate_;
  mojom::ConnectionDelegate::Proxy_* fake_connection_delegate_proxy_ = nullptr;

  std::unique_ptr<FakeClientConnectionParametersObserver> fake_observer_;

  std::unique_ptr<ClientConnectionParameters> client_connection_parameters_;

  DISALLOW_COPY_AND_ASSIGN(SecureChannelClientConnectionParametersImplTest);
};

TEST_F(SecureChannelClientConnectionParametersImplTest,
       ConnectionDelegateDisconnected) {
  DisconnectConnectionDelegatePtr();
  VerifyStatus(false /* expected_to_be_waiting_for_response */,
               true /* expected_to_be_canceled */);
}

TEST_F(SecureChannelClientConnectionParametersImplTest, OnConnection) {
  auto fake_channel = std::make_unique<FakeChannel>();
  mojom::MessageReceiverPtr message_receiver_ptr;

  CallOnConnection(fake_channel->GenerateInterfacePtr(),
                   mojo::MakeRequest(&message_receiver_ptr));
  VerifyStatus(false /* expected_to_be_waiting_for_response */,
               false /* expected_to_be_canceled */);

  EXPECT_TRUE(fake_connection_delegate()->channel());
  EXPECT_TRUE(fake_connection_delegate()->message_receiver_request());
}

TEST_F(SecureChannelClientConnectionParametersImplTest, OnConnectionFailed) {
  const mojom::ConnectionAttemptFailureReason kTestReason =
      mojom::ConnectionAttemptFailureReason::AUTHENTICATION_ERROR;

  CallOnConnectionAttemptFailure(kTestReason);
  VerifyStatus(false /* expected_to_be_waiting_for_response */,
               false /* expected_to_be_canceled */);

  EXPECT_EQ(kTestReason,
            *fake_connection_delegate()->connection_attempt_failure_reason());
}

}  // namespace secure_channel

}  // namespace chromeos
