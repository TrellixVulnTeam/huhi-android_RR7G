/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

// Utils
import { debounce } from '../../common/debounce'

const keyName = 'rewards-data'

export const defaultState: Rewards.State = {
  createdTimestamp: null,
  enabledMain: false,
  onlyAnonWallet: false,
  enabledContribute: true,
  firstLoad: null,
  walletCreated: false,
  walletCreateFailed: false,
  contributionMinTime: 8,
  contributionMinVisits: 1,
  contributionMonthly: 10,
  contributionNonVerified: true,
  contributionVideos: true,
  donationAbilityYT: true,
  donationAbilityTwitter: true,
  numExcludedSites: 0,
  excludedPublishersNumber: 0,
  walletInfo: {
    choices: [5.0, 7.5, 10.0, 17.5, 25.0, 50.0, 75.0, 100.0]
  },
  connectedWallet: false,
  recoveryKey: '',
  reconcileStamp: 0,
  ui: {
    walletRecoverySuccess: null,
    emptyWallet: true,
    walletServerProblem: false,
    modalBackup: false
  },
  adsData: {
    adsEnabled: false,
    adsPerHour: 0,
    adsUIEnabled: false,
    adsIsSupported: false,
    adsEstimatedPendingRewards: 0,
    adsNextPaymentDate: '',
    adsAdNotificationsReceivedThisMonth: 0
  },
  autoContributeList: [],
  reports: {},
  safetyNetFailed: false,
  recurringList: [],
  tipsList: [],
  tipsLoad: false,
  recurringLoad: false,
  pendingContributionTotal: 0,
  excluded: [],
  grants: [],
  currentGrant: undefined,
  rewardsIntervalId: 0,
  excludedList: [],
  balance: {
    total: 0,
    rates: {},
    wallets: {}
  }
}

const cleanData = (state: Rewards.State) => {
  if (!state.balance) {
    state.balance = defaultState.balance
  }

  return state
}

export const load = (): Rewards.State => {
  const data = window.localStorage.getItem(keyName)
  let state: Rewards.State = defaultState
  if (data) {
    try {
      state = JSON.parse(data)
    } catch (e) {
      console.error('Could not parse local storage data: ', e)
    }
  }
  return cleanData(state)
}

export const debouncedSave = debounce((data: Rewards.State) => {
  if (data) {
    window.localStorage.setItem(keyName, JSON.stringify(cleanData(data)))
  }
}, 50)
