# Changelog

## [1.4.3](https://github.com/huhisoft/huhi-android/releases/tag/v1.4.3)

 - Fixed webview crash upon visiting certain sites. ([#2166](https://github.com/huhisoft/huhi-android/issues/2166))
 - Increased max number of Huhi Ads from 12 to 20 to match desktop. ([#2068](https://github.com/huhisoft/huhi-android/issues/2068))
 - Upgraded to Chromium 78.0.3904.62. ([#2163](https://github.com/huhisoft/huhi-android/issues/2163))

## [1.4.2](https://github.com/huhisoft/huhi-android/releases/tag/v1.4.2)

 - Improved code optimization. ([#2151](https://github.com/huhisoft/huhi-android/issues/2151))

## [1.4.1](https://github.com/huhisoft/huhi-android/releases/tag/v1.4.1)

 - Fixed missing "Share" option under menu on Android Tablet. ([#2121](https://github.com/huhisoft/huhi-android/issues/2121)
 - Fixed "Not enough tokens" message shown while tipping due to wallet balance being rounded up. ([#2079](https://github.com/huhisoft/huhi-android/issues/2079))
 - Fixed an issue where rewards grant is claimed but not confirmed on rewards panel. ([#2074](https://github.com/huhisoft/huhi-android/issues/2074))
 - Fixed rewards summary items being wrongly categorized under rewards panel. ([#2082](https://github.com/huhisoft/huhi-android/issues/2082))
 - Fixed crash when deleting browser data. ([#2090](https://github.com/huhisoft/huhi-android/issues/2090))
 - Upgrade to Chromium 77.0.3865.116. ([#2136](https://github.com/huhisoft/huhi-android/issues/2136))

## [1.4.0](https://github.com/huhisoft/huhi-android/releases/tag/v1.4.0)
 
 - Added dark mode. ([#1798](https://github.com/huhisoft/huhi-android/issues/1798)) 
 - Added an option to exit from settings menu. ([#532](https://github.com/huhisoft/huhi-android/issues/532))
 - Added long press feature on new tab button. ([#1894](https://github.com/huhisoft/huhi-android/issues/1894))
 - Added maximum of three ads in notification tray. ([#1932](https://github.com/huhisoft/huhi-android/issues/1932))
 - Added DuckDuckGo as default search engine for Australia/Germany/Ireland/New Zealand on clean install. ([#2014](https://github.com/huhisoft/huhi-android/issues/2014))
 - Added an option to delete download history when clearing data. ([#1715](https://github.com/huhisoft/huhi-android/issues/1715))
 - Added an option to show ads in background. ([#2027](https://github.com/huhisoft/huhi-android/issues/2027))
 - Updated new images for sync start screen. ([#1864](https://github.com/huhisoft/huhi-android/issues/1864))
 - Removed onboarding flow for existing users in New Zealand/Ireland/Australia. ([#1893](https://github.com/huhisoft/huhi-android/issues/1893))
 - Fixed BAT wallet balance not being rounded to one decimal point. ([#1964](https://github.com/huhisoft/huhi-android/issues/1964))
 - Fixed browser ads service from being launched without UI when a notification is dismissed from notification tray. ([#1946](https://github.com/huhisoft/huhi-android/issues/1946))
 - Fixed "Ads and Trackers Blocked" not being displayed correctly under New Tab Page when reaching four digits. ([#1879](https://github.com/huhisoft/huhi-android/issues/1879))
 - Fixed icons size for rewards and shields in address bar. ([#1384](https://github.com/huhisoft/huhi-android/issues/1384))
 - Fixed share button functionality in browser menu. ([#2000](https://github.com/huhisoft/huhi-android/issues/2000))

## [1.3.2](https://github.com/huhisoft/huhi-android/releases/tag/v1.3.2)
 
 - Fixed ad notifications not being delivered on new installs in certain cases. ([#2046](https://github.com/huhisoft/huhi-android/issues/2046))
 - Updated Huhi ads delivery policy. ([#1913](https://github.com/huhisoft/huhi-android/issues/1913))

## [1.3.1](https://github.com/huhisoft/huhi-android/releases/tag/v1.3.1)
 
 - Implemented onboarding flow for new users. ([#1992](https://github.com/huhisoft/huhi-android/issues/1992))
 - Implemented an option to handle "Close all tabs" behaviour. ([#1716](https://github.com/huhisoft/huhi-android/issues/1716))
 - Implemented new tab button when home page is disabled. ([#1707](https://github.com/huhisoft/huhi-android/issues/1707))
 - Changed ad notifications to be shown only when Huhi is being used. ([#1854](https://github.com/huhisoft/huhi-android/issues/1854))
 - Disabled haptics for Huhi ads notification. ([#1866](https://github.com/huhisoft/huhi-android/issues/1866))
 - Refactored Huhi Sync warning message. ([#1819](https://github.com/huhisoft/huhi-android/issues/1819))
 - Removed "Check Back Soon for Free Token Grant" message for all regions. ([#1940](https://github.com/huhisoft/huhi-android/issues/1940))
 - Fixed browser crash when ads switch was toggled in certain situations. ([#1855](https://github.com/huhisoft/huhi-android/issues/1855))
 - Fixed browser crash when "Home page" option was turned off. ([#1981](https://github.com/huhisoft/huhi-android/issues/1981))
 - Fixed search engine "DuckDuckGo Lite" name in settings. ([#1876](https://github.com/huhisoft/huhi-android/issues/1876))
 - Fixed incorrect icon shown for verified publishers in rewards panel. ([#1829](https://github.com/huhisoft/huhi-android/issues/1829))
 - Fixed verified YouTube publisher favicon not being displayed in auto-contribute list. ([#1635](https://github.com/huhisoft/huhi-android/issues/1635))
 - Fixed crash during onboarding for devices running Android 4.x - 5.x. ([#2012](https://github.com/huhisoft/huhi-android/issues/2012))
 - Upgraded to Chromium 76.0.3809.132. ([#2005](https://github.com/huhisoft/huhi-android/issues/2005))

## [1.2.0](https://github.com/huhisoft/huhi-android/releases/tag/v1.2.0)
 
 - Replaced adblock library with new Rust based library. ([#1838](https://github.com/huhisoft/huhi-android/issues/1838))
 - Added chrome://rewards-internals to assist with rewards support. ([#1341](https://github.com/huhisoft/huhi-android/issues/1341))
 - Fixed retry logic for failed ads confirmations. ([#1950](https://github.com/huhisoft/huhi-android/issues/1950))
 - Fixed hang when attempting to create wallet on devices without Google Play Services. ([#1936](https://github.com/huhisoft/huhi-android/issues/1936))
 - Upgraded Chromium to 76.0.3809.111. ([#1947](https://github.com/huhisoft/huhi-android/issues/1947))

## [1.1.2](https://github.com/huhisoft/huhi-android/releases/tag/v1.1.2)
 
 - Upgraded Chromium to 75.0.3770.143. ([#1853](https://github.com/huhisoft/huhi-android/issues/1853))

## [1.1.1](https://github.com/huhisoft/huhi-android/releases/tag/v1.1.1)
 
 - Fixed several websites not loading correctly when Huhi Ads has been enabled. ([#1844](https://github.com/huhisoft/huhi-android/issues/1844))
 - Fixed crash when ads notification is displayed and Huhi isn't running. ([#1839](https://github.com/huhisoft/huhi-android/issues/1839))

## [1.1.0](https://github.com/huhisoft/huhi-android/releases/tag/v1.1.0)
 
 - Implemented Huhi Ads for supported regions. ([#1826](https://github.com/huhisoft/huhi-android/issues/1826))
 - Added warning message for Sync code. ([#1475](https://github.com/huhisoft/huhi-android/issues/1475))
 - Changed location of background video play option for easier access. ([#1544](https://github.com/huhisoft/huhi-android/issues/1544))
 - Changed Kabab menu options for better accessibility. ([#1373](https://github.com/huhisoft/huhi-android/issues/1373)) and ([#1718](https://github.com/huhisoft/huhi-android/issues/1718)) 
 - Changed default browser notification text. ([#1742](https://github.com/huhisoft/huhi-android/issues/1742))
 - Fixed Huhi Rewards panel separators when auto-contribute is disabled. ([#1671](https://github.com/huhisoft/huhi-android/issues/1671))
 - Removed Huhi Rewards flag. ([#1555](https://github.com/huhisoft/huhi-android/issues/1555))

## [1.0.99](https://github.com/huhisoft/huhi-android/releases/tag/v1.0.99)

 - Changed forward button icon for phones. ([#911](https://github.com/huhisoft/huhi-android/issues/911))
 - Removed "Set as default browser" option from settings menu when Huhi is already default browser. ([#1495](https://github.com/huhisoft/huhi-android/issues/1495))
 - Upgraded Chromium to 75.0.3770.101. ([#1741](https://github.com/huhisoft/huhi-android/issues/1741))

## [1.0.98](https://github.com/huhisoft/huhi-android/releases/tag/v1.0.98)

- Fixed bookmarks not being synced in certain cases. ([#1702](https://github.com/huhisoft/huhi-android/issues/1702))
- Fixed android devices randomly being removed from the sync chain. ([#1354](https://github.com/huhisoft/huhi-android/issues/1354))
- Fixed android devices randomly being disconnected from the sync chain. ([#1535](https://github.com/huhisoft/huhi-android/issues/1535))
- Upgraded Chromium to 75.0.3770.89. ([#1681](https://github.com/huhisoft/huhi-android/issues/1681))
