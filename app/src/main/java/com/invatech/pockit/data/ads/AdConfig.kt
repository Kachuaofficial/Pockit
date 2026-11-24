package com.invatech.pockit.data.ads

/*
 * Ad placement configuration for the SDK integrations.
 * These identifiers select ad inventory; consent and resource lifecycle are handled by other classes.
 */

/** AdMob identifiers supplied by the owner. Test devices are configured in the AdMob console/SDK. */
object AdConfig {
    const val PRODUCTION_APP_ID = "ca-app-pub-5858375161867194~8387330202"
    const val PRODUCTION_BANNER_UNIT = "ca-app-pub-5858375161867194/7308523574"
    const val PRODUCTION_REWARDED_UNIT = "ca-app-pub-5858375161867194/6728943227"
    // Native advanced unit supplied for this same production AdMob app.
    const val PRODUCTION_NATIVE_UNIT = "ca-app-pub-5858375161867194/8837227666"
    const val APP_ID = PRODUCTION_APP_ID
    const val BANNER_AD_UNIT_ID = PRODUCTION_BANNER_UNIT
    const val REWARDED_AD_UNIT_ID = PRODUCTION_REWARDED_UNIT
    const val NATIVE_AD_UNIT_ID = PRODUCTION_NATIVE_UNIT
}
