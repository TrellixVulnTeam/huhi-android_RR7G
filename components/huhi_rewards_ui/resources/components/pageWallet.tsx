/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

import * as React from 'react'
import { bindActionCreators, Dispatch } from 'redux'
import { connect } from 'react-redux'
import BigNumber from 'bignumber.js'

// Components
import { AlertWallet } from 'huhi-ui/src/features/rewards/walletWrapper'
import {
  WalletSummary,
  WalletWrapper,
  WalletEmpty,
  WalletOff
} from 'huhi-ui/src/features/rewards'
import { CloseStrokeIcon } from 'huhi-ui/src/components/icons'
import { StyledWalletClose, StyledWalletOverlay, StyledWalletWrapper } from './style'

// Utils
import { getLocale } from '../../../common/locale'
import * as rewardsActions from '../actions/rewards_actions'
import * as utils from '../utils'

interface State {
  activeTabId: number
}

interface Props extends Rewards.ComponentProps {
  visible?: boolean
  toggleAction: () => void
}

class PageWallet extends React.Component<Props, State> {
  constructor (props: Props) {
    super(props)

    this.state = {
      activeTabId: 0
    }
  }

  get actions () {
    return this.props.actions
  }

  getConversion = () => {
    const balance = this.props.rewardsData.balance
    return utils.convertBalance(balance.total.toString(), balance.rates)
  }

  getGrants = () => {
    const grants = this.props.rewardsData.walletInfo.grants
    if (!grants) {
      return []
    }

    return grants.map((grant: Rewards.Grant) => {
      return {
        tokens: utils.convertProbiToFixed(grant.probi, 1, BigNumber.ROUND_HALF_UP),
        expireDate: new Date(grant.expiryTime * 1000).toLocaleDateString(),
        type: grant.type
      }
    })
  }

  walletAlerts = (): AlertWallet | null => {
    const { walletServerProblem } = this.props.rewardsData.ui

    if (walletServerProblem) {
      return {
        node: <React.Fragment><b>{getLocale('uhOh')}</b> {getLocale('serverNotResponding')}</React.Fragment>,
        type: 'error'
      }
    }

    return null
  }

  getWalletSummary = () => {
    const { balance, reports } = this.props.rewardsData

    let props = {}

    const currentTime = new Date()
    const reportKey = `${currentTime.getFullYear()}_${currentTime.getMonth() + 1}`
    const report: Rewards.Report = reports[reportKey]
    if (report) {
      for (let key in report) {
        const item = report[key]

        if (item.length > 1 && key !== 'total') {
          const tokens = utils.convertProbiToFixed(item, 1, BigNumber.ROUND_HALF_UP)
          props[key] = {
            tokens,
            converted: utils.convertBalance(tokens, balance.rates)
          }
        }
      }
    }

    return {
      report: props
    }
  }

  render () {
    const { visible, toggleAction } = this.props
    const {
      enabledMain,
      connectedWallet,
      balance,
      ui,
      pendingContributionTotal,
      onlyAnonWallet
    } = this.props.rewardsData
    const { emptyWallet } = ui
    const { total } = balance
    const pendingTotal = parseFloat((pendingContributionTotal || 0).toFixed(1))

    if (!visible) {
      return null
    }

    return (
      <React.Fragment>
        <StyledWalletOverlay>
          <StyledWalletClose>
            <CloseStrokeIcon onClick={toggleAction}/>
          </StyledWalletClose>
          <StyledWalletWrapper>
            <WalletWrapper
              balance={total.toFixed(1)}
              converted={utils.formatConverted(this.getConversion())}
              actions={[]}
              compact={true}
              isMobile={true}
              showCopy={false}
              showSecActions={false}
              grants={this.getGrants()}
              alert={this.walletAlerts()}
              onlyAnonWallet={onlyAnonWallet}
              connectedWallet={connectedWallet}
            >
              {
                enabledMain
                ? emptyWallet
                  ? <WalletEmpty hideAddFundsText={true} />
                  : <WalletSummary
                    reservedAmount={pendingTotal}
                    reservedMoreLink={'https://huhisoft.com/faq-rewards/#unclaimed-funds'}
                    {...this.getWalletSummary()}
                  />
                : <WalletOff/>
              }
            </WalletWrapper>
          </StyledWalletWrapper>
        </StyledWalletOverlay>
      </React.Fragment>
    )
  }
}

const mapStateToProps = (state: Rewards.ApplicationState) => ({
  rewardsData: state.rewardsData
})

const mapDispatchToProps = (dispatch: Dispatch) => ({
  actions: bindActionCreators(rewardsActions, dispatch)
})

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(PageWallet)
