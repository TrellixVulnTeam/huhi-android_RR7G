/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

import BigNumber from 'bignumber.js'
import { Address } from 'huhi-ui/src/features/rewards/modalAddFunds'

export let actions: any = null

export const getActions = () => actions
export const setActions = (newActions: any) => actions = newActions

export const convertBalance = (tokens: string, rates: Record<string, number> | undefined, currency: string = 'USD'): string => {
  const tokensNum = parseFloat(tokens)
  if (tokensNum === 0 || !rates || !rates[currency]) {
    return '0.00'
  }

  const converted = tokensNum * rates[currency]

  if (isNaN(converted)) {
    return '0.00'
  }

  return converted.toFixed(2)
}

export const formatConverted = (converted: string, currency: string = 'USD'): string | null => {
  return `${converted} ${currency}`
}

export const generateContributionMonthly = (list: number[], rates: Record<string, number> | undefined) => {
  if (!list) {
    return []
  }

  return list.map((item: number) => {
    return {
      tokens: item.toFixed(1),
      converted: convertBalance(item.toString(), rates)
    }
  })
}

export const donationTotal = (report: Rewards.Report) => {
  if (!report) {
    return '0.0'
  }

  const tips = new BigNumber(report.tips)
  return new BigNumber(report.donation).plus(tips).dividedBy('1e18').toFixed(1, BigNumber.ROUND_DOWN)
}

export const convertProbiToFixed = (probi: string, places: number = 1, rm: 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | undefined = BigNumber.ROUND_DOWN ) => {
  const result = new BigNumber(probi).dividedBy('1e18').toFixed(places, rm)

  if (result === 'NaN') {
    return '0.0'
  }

  return result
}

export const getAddresses = (addresses?: Record<Rewards.AddressesType, Rewards.Address>) => {
  let result: Address[] = []

  if (!addresses) {
    return result
  }

  const sortedArray = [ 'BTC', 'ETH', 'BAT', 'LTC' ]

  sortedArray.forEach((type: Rewards.AddressesType) => {
    const item: Rewards.Address = addresses[type]
    if (item) {
      result.push({
        type,
        qr: item.qr,
        address: item.address
      })
    }
  })

  return result
}

export const constructBackupString = (backupKey: string) => {
  return `Huhi Wallet Recovery Key\nDate created: ${new Date(Date.now()).toLocaleDateString()} \n\nRecovery Key: ${backupKey}` +
    '\n\nNote: This key is not stored on Huhi servers. ' +
    'This key is your only method of recovering your Huhi wallet. ' +
    'Save this key in a safe place, separate from your Huhi browser. ' +
    'Make sure you keep this key private, or else your wallet will be compromised.'
}

export const isPublisherConnectedOrVerified = (status?: Rewards.PublisherStatus) => {
  if (status === undefined) {
    return false
  }

  return status === 2 || status === 1
}