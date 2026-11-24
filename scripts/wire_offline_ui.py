from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/invatech/pockit'
p=JAVA/'ui/navigation/PockitApp.kt';s=p.read_text()
s=s.replace('    val context = LocalContext.current','''    val context = LocalContext.current
    val online = com.invatech.pockit.ui.common.rememberOnline()
    val offlineSync by com.invatech.pockit.data.offline.OfflineSyncManager.state.collectAsState()''',1)
s=s.replace('containerColor = MaterialTheme.colorScheme.background, bottomBar = {', '''containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            com.invatech.pockit.ui.common.OfflineStatusCard(online, offlineSync) {
                navController.navigate("perks") { launchSingleTop = true }
            }
        }, bottomBar = {''')
s=s.replace('''                NavHost(
''','''                val offlineRoutes = setOf("home", "all_transactions", "insights", "advanced_reports", "planner", "budgets", "perks")
                if (!online && currentRoute != null && currentRoute !in offlineRoutes) {
                    androidx.compose.foundation.layout.Box(Modifier.padding(innerPadding)) {
                        com.invatech.pockit.ui.common.OfflineFeatureScreen {
                            navController.navigate("home") { popUpTo("home"); launchSingleTop = true }
                        }
                    }
                } else NavHost(
''',1)
p.write_text(s)

p=JAVA/'data/commerce/ProBillingClient.kt';s=p.read_text()
s=s.replace('    const val PRODUCT_ID="pockit_pro"','''    const val PRODUCT_ID="pockit_pro"
    const val INDIA_MONTHLY_PRICE_MICROS = 49_000_000L''')
s=s.replace('it.basePlanId in setOf("monthly","yearly")', 'it.basePlanId == "monthly"')
s=s.replace('if(phase.recurrenceMode!=ProductDetails.RecurrenceMode.INFINITE_RECURRING)', 'if(!monthlyPriceAllowed(phase.priceCurrencyCode, phase.priceAmountMicros))return@mapNotNull null\n                        if(phase.recurrenceMode!=ProductDetails.RecurrenceMode.INFINITE_RECURRING)')
s=s.replace('Subscription plans are currently unavailable. You can still use coins.', 'The ₹49 monthly plan is currently unavailable. Install Pockit from Google Play to subscribe, or try again later. You can also unlock features with reward ads.')
s=s.replace('''    fun start() {
''', '''    fun start() {
        if(!com.invatech.pockit.data.network.NetworkMonitor.online.value) {
            mutable.value=mutable.value.copy(loading=false,error="You're offline. Connect to Google Play to view subscriptions.");return
        }
''',1)
s+='''
internal fun monthlyPriceAllowed(currency: String, micros: Long) =
    micros > 0 && (currency != "INR" || micros == ProBillingClient.INDIA_MONTHLY_PRICE_MICROS)
'''
p.write_text(s)

p=JAVA/'ui/commerce/PerksScreen.kt';s=p.read_text()
s=s.replace('    val commerce by CommerceRepository.state.collectAsState()', '''    val commerce by CommerceRepository.state.collectAsState()
    val online = com.invatech.pockit.ui.common.rememberOnline()
    val sync by com.invatech.pockit.data.offline.OfflineSyncManager.state.collectAsState()''',1)
s=s.replace('LaunchedEffect(ready,commerce.userId,commerce.canEarn){if(ready && commerce.canEarn)', 'LaunchedEffect(ready,online,commerce.userId,commerce.canEarn){if(online && ready && commerce.canEarn)')
s=s.replace('else if(!ready)controller.invalidate()', 'else if(!ready || !online)controller.invalidate()')
s=s.replace('PerksContent(commerce,billing,ad,ready,focus,onBack,', 'PerksContent(commerce,billing,ad,ready && online,focus,onBack,')
s=s.replace('''onLegal={privacy->legal=if(privacy)com.invatech.pockit.ui.common.legal.LegalDocType.PRIVACY_POLICY else com.invatech.pockit.ui.common.legal.LegalDocType.TERMS_OF_SERVICE})''','''onLegal={privacy->legal=if(privacy)com.invatech.pockit.ui.common.legal.LegalDocType.PRIVACY_POLICY else com.invatech.pockit.ui.common.legal.LegalDocType.TERMS_OF_SERVICE},
        online=online,sync=sync,onSync={scope.launch{com.invatech.pockit.data.offline.OfflineSyncManager.sync()}})''')
s=s.replace('text={Text("Watch the reward ad to completion to earn ${commerce.rewardCoins} coins. You can skip this offer. Coins appear after the ad is verified.")}', 'text={Text("Watch the reward ad to completion to earn ${commerce.rewardCoins} coins.${if(sync.count>0) " This also syncs up to 100 pending entries to your account after server verification." else ""} You can skip this offer. Coins appear after the ad is verified.")}')
s=s.replace('onSubscribe:(ProPlan)->Unit,onRestore:()->Unit,onManage:()->Unit,onLegal:(Boolean)->Unit={})', '''onSubscribe:(ProPlan)->Unit,onRestore:()->Unit,onManage:()->Unit,onLegal:(Boolean)->Unit={},
    online:Boolean=true,sync:com.invatech.pockit.data.offline.OfflineSyncState=com.invatech.pockit.data.offline.OfflineSyncState(),onSync:()->Unit={})''')
s=s.replace('''        if(focus!=null)item''','''        if(!online)item{CinematicCard{
            Text("You're offline",style=MaterialTheme.typography.titleLarge)
            Text("Your saved records and local tools are available. Ads, purchases and cloud sync resume when you're online.")
        }}
        if(sync.count>0)item{CinematicCard{
            Text("${sync.count} entries saved on this phone",style=MaterialTheme.typography.titleLarge)
            Text("Connect and finish one reward video to save up to 100 pending entries to your account. The video also earns ${commerce.rewardCoins} coins. Active Pro syncs automatically without ads.",color=PockitSecondaryText)
            if(sync.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            sync.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            if(online && (commerce.isPro || commerce.syncRewardId!=null))Button(onClick=onSync,enabled=!sync.busy){Text("Sync saved entries")}
        }}
        if(focus!=null)item''',1)
s=s.replace('enabled=commerce.canEarn && adsReady', 'enabled=online && commerce.canEarn && adsReady')
s=s.replace('enabled=commerce.serverVerified && !commerce.busy', 'enabled=online && commerce.serverVerified && !commerce.busy')
s=s.replace('enabled=commerce.serverVerified && commerce.userId', 'enabled=online && commerce.serverVerified && commerce.userId')
s=s.replace('''            Text("All premium features. One subscription.",style=MaterialTheme.typography.titleMedium)''','''            Text("₹49 / month in India",style=MaterialTheme.typography.headlineSmall,color=PockitOrange)
            Text("All premium features. One monthly subscription.",style=MaterialTheme.typography.titleMedium)''')
s=s.replace('"Ad-free everyday use").forEach', '"Ad-free everyday use","Automatic cloud sync for offline entries").forEach')
s=s.replace('Text("Basic tracking stays free. Your coins remain available when Pro ends."', 'Text("Basic tracking stays free. Watch videos for coins, then use coins to unlock feature passes without a subscription. Your coins remain available when Pro ends."')
s=s.replace('''                billing.plans.forEach{plan->''','''                if(billing.plans.isEmpty() && !billing.loading) {
                    Text(if(online) "Monthly subscriptions are currently unavailable. Try again later or unlock passes with reward ads." else "Connect to view the monthly plan.",color=PockitSecondaryText)
                }
                billing.plans.filter{it.basePlanId=="monthly"}.forEach{plan->''')
s=s.replace('TextButton(onClick=onRestore,enabled=!billing.busy)', 'TextButton(onClick=onRestore,enabled=online && !billing.busy)')
p.write_text(s)
print('Global offline status, feature routing and ₹49 monthly offer wired.')
