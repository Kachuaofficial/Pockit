from pathlib import Path
import re
from PIL import Image, ImageOps, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'app/src/main'
JAVA = SRC/'java/com/invatech/pockit'

def change(path, old, new):
    p=ROOT/path
    s=p.read_text(encoding='utf-8')
    assert old in s, str(path)+': missing target '+old[:60]
    p.write_text(s.replace(old,new), encoding='utf-8')

# Package the supplied artwork without redrawing or generating a different logo.
original=Image.open(r'C:/Users/shiva/Downloads/ChatGPT Image Nov 23, 2026, 07_37_40 AM.png').convert('RGB')
assets=SRC/'res/drawable-nodpi'; assets.mkdir(exist_ok=True)
brand=original.crop((167,177,1087,1097))
brand.resize((768,768),Image.Resampling.LANCZOS).save(assets/'pockit_brand.png')
original.save(ROOT/'docs/POCKIT_OFFICIAL_LOGO.png')
# The transparent monochrome layer retains the rupee and stitching as negative space.
mask=Image.new('L',original.size)
mask.putdata([255 if min(rgb)<175 or max(rgb)-min(rgb)>42 else 0 for rgb in original.getdata()])
icon=Image.new('RGB',(432,432),(255,250,241))
fitted=original.resize((400,400),Image.Resampling.LANCZOS)
icon.paste(fitted,(16,16));icon.save(assets/'pockit_adaptive_foreground.png')
alpha=Image.new('L',(432,432));alpha.paste(mask.resize((400,400),Image.Resampling.LANCZOS),(16,16))
mono=Image.new('RGBA',(432,432),'white');mono.putalpha(alpha);mono.save(assets/'pockit_monochrome.png')
bitmap=lambda name: f'''<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@drawable/{name}" android:gravity="fill" android:filter="true"/>\n'''
for name,target in [('ic_launcher_foreground','pockit_adaptive_foreground'),('ic_launcher_monochrome','pockit_monochrome'),('ic_splash_logo','pockit_adaptive_foreground')]:
    (SRC/f'res/drawable/{name}.xml').write_text(bitmap(target))
for name in ['ic_launcher','ic_launcher_round']:
    (SRC/f'res/mipmap-anydpi/{name}.xml').write_text(bitmap('pockit_brand'))
for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    folder=SRC/f'res/mipmap-{density}'
    brand.resize((size,size),Image.Resampling.LANCZOS).save(folder/'ic_launcher.webp',lossless=True)
    rounded=brand.resize((size,size),Image.Resampling.LANCZOS).convert('RGBA')
    roundmask=Image.new('L',(size,size));ImageDraw.Draw(roundmask).ellipse((0,0,size-1,size-1),fill=255)
    rounded.putalpha(roundmask);rounded.save(folder/'ic_launcher_round.webp',lossless=True)
    icon.resize((size*2,size*2),Image.Resampling.LANCZOS).save(folder/'ic_launcher_foreground.webp',lossless=True)
    mono.resize((size*2,size*2),Image.Resampling.LANCZOS).save(folder/'ic_launcher_monochrome.webp',lossless=True)
brand.resize((512,512),Image.Resampling.LANCZOS).save(SRC/'ic_launcher-playstore.png')
for path in ['res/values/ic_launcher_background.xml','res/drawable/ic_launcher_background.xml']:
    p=SRC/path
    p.write_text(re.sub(r'#[0-9A-Fa-f]{6,8}', '#FFFAF1', p.read_text()))

p=JAVA/'ui/home/components/CinematicDashboard.kt'
s=p.read_text(); start=s.index('/** Resolution-independent'); end=s.index('\n@Composable\nfun CinematicCard',start)
s=s[:start]+'''/** Official supplied Pockit artwork, shared across app surfaces. */
@Composable
fun PockitMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.invatech.pockit.R.drawable.pockit_brand),
        contentDescription = "Pockit official logo",
        modifier = modifier.clip(RoundedCornerShape(20)),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}
''' + s[end:];p.write_text(s)

db='app/src/main/java/com/invatech/pockit/data/local/database/PockitDatabase.kt'
change(db,'CategoryBudgetEntity::class\n','CategoryBudgetEntity::class,\n        com.invatech.pockit.data.local.entity.PendingWalletEntry::class\n')
change(db,'version = 3,','version = 4,')
change(db,'abstract fun budgetDao(): BudgetDao','abstract fun budgetDao(): BudgetDao\n    abstract fun pendingWalletDao(): com.invatech.pockit.data.local.dao.PendingWalletDao')
change(db,'PockitMigrations.FROM_2_TO_3)','PockitMigrations.FROM_2_TO_3, PockitMigrations.FROM_3_TO_4)')
p=JAVA/'data/local/database/PockitMigrations.kt';s=p.read_text();i=s.rfind('}')
s=s[:i]+'''    val FROM_3_TO_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS pending_wallet_entries (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, amountPaise INTEGER NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)")
        }
    }
'''+s[i:];p.write_text(s)

change('app/src/main/java/com/invatech/pockit/PockitApplication.kt','com.invatech.pockit.data.commerce.CommerceRepository.initialize()',
'''com.invatech.pockit.data.network.NetworkMonitor.initialize(this)
        com.invatech.pockit.data.commerce.CommerceRepository.initialize()
        com.invatech.pockit.data.offline.OfflineSyncManager.initialize()''')

p=JAVA/'ui/common/OfflineErrors.kt';s=p.read_text();start=s.index('    val context = LocalContext.current.applicationContext');end=s.index('\n}\n',start)
s=s[:start]+'''    val online by com.invatech.pockit.data.network.NetworkMonitor.online.collectAsState()
    return online'''+s[end:];p.write_text(s)

change('functions/monetization/service.js','const updated={balance,lifetimeEarned:earned,updatedAtMs:timestamp};','const updated={balance,lifetimeEarned:earned,updatedAtMs:timestamp,latestSyncRewardId:nonce};')
change('functions/monetization/index.js','exports.syncCommerce=callable(uid=>commerce.sync(uid));',
'''const offline = require("./offline").createOfflineService(db);
exports.syncOfflineTransactions=callable((uid,data)=>offline.sync(uid,data),true);
exports.syncCommerce=callable(uid=>commerce.sync(uid));''')
change('firestore.rules','match /coinRewardSessions/{session}', 'match /offlineSyncReceipts/{receipt} { allow read, write: if false; }\n    match /coinRewardSessions/{session}')

change('app/src/main/java/com/invatech/pockit/data/commerce/CommerceModels.kt','val trustedNowMs:Long=0,','val syncRewardId:String?=null,\n    val trustedNowMs:Long=0,')
change('app/src/main/java/com/invatech/pockit/data/commerce/CommerceRepository.kt','proActive=pro["active"]==true,', 'syncRewardId=data["latestSyncRewardId"] as? String,proActive=pro["active"]==true,')
change('app/src/main/java/com/invatech/pockit/data/commerce/CommerceRepository.kt','val options=HttpsCallableOptions.Builder()', 'com.invatech.pockit.data.network.NetworkMonitor.requireOnline()\n        val options=HttpsCallableOptions.Builder()')
change('app/src/main/java/com/invatech/pockit/data/commerce/CommerceRepository.kt','val code=(e as? FirebaseFunctionsException)?.code',
'''if(!com.invatech.pockit.data.network.NetworkMonitor.online.value) return "You're offline. Saved records and local tools are available. Connect to use rewards, subscriptions and cloud sync."
        val code=(e as? FirebaseFunctionsException)?.code''')
change('app/src/main/java/com/invatech/pockit/data/commerce/CommerceRepository.kt','FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Sign in and verify this app to use the store."',
'''FirebaseFunctionsException.Code.NOT_FOUND -> "Pockit's store service has not been activated yet. Your saved records are safe. Please try again after the service is ready."
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "App verification failed. Update Pockit from Google Play and sign in again."
            FirebaseFunctionsException.Code.UNAVAILABLE, FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Pockit's server is temporarily unavailable. Your local entries are safe; retry sync later."''')

change('app/build.gradle.kts','versionCode = 9','versionCode = 10')
change('app/build.gradle.kts','versionName = "2.1"','versionName = "2.2"')
print('Official brand assets, schema v4 and commerce wiring updated.')
