/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

 // @ts-ignore until react type includes Suspense
import React from 'react'
import { bindActionCreators, Dispatch } from 'redux'
import { connect } from 'react-redux'

// Components
import SettingsPage from './settingsPage'
import WelcomePage from './welcomePage'

// Utils
import * as rewardsActions from '../actions/rewards_actions'

interface Props extends Rewards.ComponentProps {
}

interface State {
  creating: boolean
}

export class App extends React.Component<Props, State> {
  constructor (props: Props) {
    super(props)
    this.state = {
      creating: false
    }
  }

  componentDidMount () {
    this.actions.checkWalletExistence()
    this.actions.getRewardsEnabled()
    this.actions.getExcludedSites()
    this.actions.getBalance()
    this.actions.onlyAnonWallet()
  }

  componentDidUpdate (prevProps: Props, prevState: State) {
    if (
      this.state.creating &&
      !prevProps.rewardsData.walletCreateFailed &&
      this.props.rewardsData.walletCreateFailed
    ) {
      this.setState({
        creating: false
      })
    }
  }

  onCreateWalletClicked = () => {
    if (window &&
        window.navigator &&
        !window.navigator.onLine) {
      alert('The device is offline, please try again later.')
      return
    }

    this.actions.createWallet()
    this.setState({
      creating: true
    })
  }

  get actions () {
    return this.props.actions
  }

  render () {
    const { rewardsData } = this.props
    let props: {onReTry?: () => void} = {}

    if (rewardsData.walletCreateFailed) {
      props = {
        onReTry: this.onCreateWalletClicked
      }
    }

    return (
      <div id='rewardsPage'>
        {
          !rewardsData.walletCreated
          ? <WelcomePage
            creating={this.state.creating}
            optInAction={this.onCreateWalletClicked}
            {...props}
          />
          : null
        }
        {
          rewardsData.walletCreated
          ? <SettingsPage />
          : null
        }
      </div>
    )
  }
}

export const mapStateToProps = (state: Rewards.ApplicationState) => ({
  rewardsData: state.rewardsData
})

export const mapDispatchToProps = (dispatch: Dispatch) => ({
  actions: bindActionCreators(rewardsActions, dispatch)
})

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App)
