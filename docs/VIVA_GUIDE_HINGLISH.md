# Pockit viva guide

Is guide ko code ke saath padho. Examples samajh kar apne words mein explain karo; comments kisi feature ke implemented ya secure hone ka independent proof nahi hain.

## 1. Project ko 45 seconds mein explain karo

Pockit ek native Android expense-recording application hai. Ismein personal income/expense ledger, shared spaces, connected-friend records, budgets, goals aur payment-notification review hain. Kotlin aur Jetpack Compose UI banate hain. ViewModels screen state manage karte hain, repositories data operations coordinate karti hain, Room local records rakhta hai aur Firebase account identity/cloud collaboration provide karta hai. Important money calculations integer paise mein hoti hain. App bank se paisa transfer nahi karti; recorded expenses aur acknowledged settlements track karti hai.

## 2. Sabse pehle kaun si files kholni hain

Paths repository root se relative hain.

| File / package | Kya explain karna hai |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | Android entry points, permissions, services, widget receiver |
| `MainActivity.kt` | Splash, onboarding, sign-in, optional device lock, main app |
| `PockitApplication.kt` | Process startup, manager initialization, account cache ownership |
| `ui/navigation/PockitApp.kt` | Routes, shared ViewModels, intent consumption, listener disposal |
| `ui/home/HomeScreen.kt` | Dashboard aur transaction dialogs |
| `ui/home/HomeViewModel.kt` | StateFlow, loading/errors, request fingerprint, optimistic actions |
| `data/repository/WalletRepository.kt` | Transaction + balance atomicity, stable request ID, cache |
| `domain/Money.kt` | Positive amount vs signed balance, paise, splitting |
| `ui/space/util/BalanceCalculator.kt` | Net positions, sorted greedy matching, complexity |
| `data/repository/SpaceRepository.kt` | Expenses, payer enforcement, pending receipt/confirmation |
| `data/local/database/PockitDatabase.kt` | Main Room database, per-account filename, migrations |
| `data/local/database/FinanceDatabase.kt` | Owner/kind-scoped udhar, goals, UPI review rows |
| `ui/finance/FinanceViewModel.kt` | Approval, personal udhar signs, goal contribution |
| `data/notification/UpiNotificationParser.kt` | Text heuristics aur ambiguous input rejection |
| `data/notification/UpiNotificationManager.kt` | Stable review IDs; no direct wallet entry |
| `functions/index.js` | Saved friend/space event ke baad FCM alert |
| `firestore.rules` | Actual server access policy aur its present limitations |

Main Kotlin paths ka common prefix: `app/src/main/java/com/invatech/pockit/`.

## 3. Architecture aur Compose ke questions

**MVVM kya hai?** View screen dikhati hai, ViewModel observable state aur user actions coordinate karta hai, aur repository persistence ko handle karti hai. Domain helpers jaise Money aur BalanceCalculator calculations ko UI se alag rakhte hain. Project ko layered MVVM implementation kehna accurate hai; har file perfectly independent Clean Architecture module nahi hai.

**Unidirectional data flow kya hai?** User action -> ViewModel/repository -> updated state -> screen redraw. Screen database state ko directly edit karke successful transaction assume nahi karti.

**Compose recomposition kya hota hai?** Observed state change hone par relevant UI functions dobara evaluate ho sakte hain. Composable ke body mein unguarded network write rakhna repeated saves ka reason ban sakta hai.

**remember, rememberSaveable aur ViewModel mein difference?** remember composition ke andar value retain karta hai. rememberSaveable supported small UI state ko saved-state mechanism se restore kar sakta hai. ViewModel screen/business state ko configuration changes ke across rakhta hai. Inmein se koi bhi apne aap permanent database storage nahi hai; process restart ke liye Room/preferences/cloud ka role hai.

**StateFlow kyun?** ViewModel ek current observable state expose kar sakta hai, aur UI collect karke display update kar leti hai. MutableStateFlow private rakhna screen ko direct arbitrary mutation se bachata hai.

**LaunchedEffect vs DisposableEffect?** LaunchedEffect key ke lifecycle se linked coroutine chalata hai. DisposableEffect callbacks/listeners register karke onDispose mein cleanup karta hai. PockitApp preference listeners aur OfflineErrors network callback cleanup ke examples hain.

**viewModelScope ka role?** ViewModel-owned asynchronous work launch karna. ViewModel clear hone par is scope ka work cancel hota hai. Firestore callback listeners ko awaitClose/onCleared/onDispose se explicitly remove karna bhi zaroori ho sakta hai.

## 4. App open hone ka actual flow

1. Android manifest se `PockitApplication` aur launch activity resolve karta hai.
2. Application startup managers initialize karta hai aur legacy cache owner record karta hai.
3. MainActivity Compose host banati hai; authentication-check state splash ko inform karti hai.
4. Branch order onboarding -> Google sign-in -> optional configured device lock -> PockitApp hai.
5. GoogleAuthClient foreground activity se Google identity token leta hai. AuthRepository us token ko Firebase credential se exchange karta hai.
6. PockitApp navigation aur account-keyed ViewModels banata hai. Account preference/profile listeners disposal par remove hote hain.
7. Notification/widget intent route ko pending state mein rakha jaata hai, phir handle hone ke baad clear kiya jaata hai.

Biometric app lock local device access check hai. Google/Firebase login account identity hai. Dono ko bank payment verification mat kehna. Current lock setup ko har background/resume par guaranteed relock ke roop mein describe mat karna; actual activity state inspect karo.

## 5. Personal expense save ka end-to-end flow

1. HomeScreen expense action se AddWalletTransactionSheet open karta hai.
2. Sheet description, amount text aur category leti hai. Money.parse valid decimal rupees ko paise mein convert karta hai.
3. HomeViewModel addExpense/addIncome common addTransaction path use karta hai.
4. ViewModel input fingerprint aur request ID retain karta hai. Same pending form retry ho to wahi ID reuse hoti hai.
5. WalletRepository signed-in owner check karta hai. Local per-account Mutex same process ke competing calls serialize karta hai.
6. Firestore transaction wallet document aur candidate event ID read karti hai. Existing identical event par second balance effect nahi lagta. Same ID with conflicting details reject hota hai.
7. Naya event ho to income balance badhata hai aur expense ghatata hai. Wallet aur event transaction mein commit hote hain.
8. Accepted operation ke baad local cache/state, widget refresh aur applicable budget check hote hain.
9. Cloud failure par error aata hai. Cache available reh sakti hai; failed save ko completed payment mat kehna.

**Atomicity vs idempotency:** Atomicity = wallet + event ek transaction mein commit hona. Idempotency = same request retry se second financial effect na lagna. Dono alag problems solve karte hain.

**Mutex vs Firestore transaction:** Mutex is app process ke local calls coordinate karta hai. Do alag devices ka conflict server transaction handle karti hai. Mutex distributed lock nahi hai.

**Retry key ki limit:** HomeViewModel ka pending fingerprint memory mein hai. Same ViewModel ke retry ko protect karta hai; app process death ke across durable request queue nahi hai. UPI approval ka deterministic `upi_<review ID>` key separately stable hai.

## 6. Money aur balance ke examples

**Double kyun avoid karte hain?** Binary floating-point decimal amounts ko exactly represent nahi karta. Wallet amount 12.50 rupees ko 1250 Long paise rakhte hain. User text ko BigDecimal se parse karke exact scale check hota hai.

**Kya poori app integer-only hai?** Nahi. Wallet/budget/finance amountPaise canonical Long fields hain. Legacy shared/friend models aur kuch display totals Double rupees retain karte hain. Conversion boundary Money.legacyPaise hai; central balance/split arithmetic paise mein hoti hai.

**parse vs parseBalance:** Regular expense/income amount positive hona chahiye. Recorded balance zero ya negative bhi ho sakta hai, isliye alag parser hai. `1.234`, `NaN`, scientific notation ya out-of-range values valid transaction input nahi hain.

**Equal split:** INR 100.00 = 10000 paise. Three participants: quotient 3333, remainder 1. Output [3334, 3333, 3333] paise. Total exactly 10000 rahta hai. Stable member order batata hai extra paisa kise milega.

**Math.addExact/subtractExact kyun?** Long overflow par wrap hone ke bajay exception aati hai. Financial total silently incorrect nahi banna chahiye.

**Recorded balance correction kya hai?** User app-recorded opening/current balance set kar sakta hai. Repository expected old value ko cloud value se compare karti hai aur adjustment audit record bhi likhti hai. Yeh bank se fetched balance nahi hai.

## 7. Group balance aur settlement algorithm

Formula: `net(member) = total paid - allocated shares`, confirmed record set ke liye. Positive = user ko milna hai; negative = user ko dena hai. Reconciled group mein all net positions ka sum zero hona chahiye.

Example: Alice dinner INR 90 pay karti hai; Bob cab INR 30. Alice, Bob, Charlie dono bills equally share karte hain. Har person ka cost INR 40 hai. Net: Alice +50, Bob -10, Charlie -40.

1. Creditors aur debtors ko absolute outstanding amount ke order mein sort karte hain.
2. Current debtor aur creditor ke beech minimum outstanding amount suggest hota hai.
3. Dono remaining positions update hoti hain.
4. Zero hui side ka index aage badhta hai.

Is example mein Charlie -> Alice INR 40 aur Bob -> Alice INR 10. Dinner delete hone par sirf cab bachta hai: Alice -10, Bob +20, Charlie -10. Balances remaining history se recompute hote hain.

**Complexity:** N member positions sort karna O(N log N), matching pass O(N). Expense aggregation additional O(S), jahan S processed share entries hain.

**N-1 kyun?** Har transfer kam se kam ek outstanding position settle karta hai, aur last transfer do sides clear karta hai. N non-zero reconciled positions ke liye at most N-1 transfers bante hain.

**Kya globally minimum transactions guarantee hain?** Nahi. Yeh sorted greedy simplification hai. Valid reconciliation aur the upper bound explain kar sakte ho; har input ke liye global optimum prove nahi kiya gaya.

**Pending settlement kyun?** SpaceRepository settle() `isConfirmed=false` record banata hai. Recipient acknowledgement ke baad confirmed calculation mein aata hai. Yeh external payment ka acknowledgement hai, automatic bank transfer nahi.

**Payer kaun ho sakta hai?** Current shared-expense creation path authenticated user ko payer enforce karta hai. Method signature mein paidById hone ka matlab arbitrary multi-payer UI support nahi hai.

**Household space kyun different?** Current BalanceCalculator household case ko bill-split debt model mein process nahi karta. Household summary aur regular bill-split settlement ko same calculation assume mat karo.

## 8. Room, Firestore aur offline behaviour

**Room kya karta hai?** SQLite ke upar typed entities, DAOs aur observable queries deta hai. Entity local row structure hai; DAO read/write/query boundary hai; RoomDatabase access point hai.

**Firestore kya karta hai?** Cloud document storage, listeners aur transactions. User private paths aur shared space paths ka ownership alag hai. Local cache aur cloud data ka meaning same units/IDs se map karna zaroori hai.

**Do Room databases kyun?** PockitDatabase main wallet/space/friend/notification/subscription/budget cache hai. FinanceDatabase generic finance_records table se UDHAR, GOAL aur UPI rows handle karta hai. Finance records owner/kind se filter hote hain.

**Account cache isolation?** Main database filename UID hash se choose hota hai. SHA-256 yahan naming/isolation ke liye hai, SQLite encryption ke liye nahi. FinanceDatabase shared instance mein owner filtering ka role important hai.

**Migration kya hai?** Existing schema se next schema par data-preserving upgrade. PockitDatabase version 3 hai aur 1->2, 2->3 migrations register hain. Sirf version number badha dena sufficient nahi hota.

**Kya app fully offline write karti hai?** Cached reads available ho sakti hain. Wallet cloud transaction ko successful online commit chahiye. Har operation ke liye durable queued offline-write/conflict strategy implemented assume nahi karni.

**History ka default limit 100 hai?** Signature mein limit=100 hai, lekin current normal server path 500-document pages mein full history fetch karta hai. Woh parameter local fallback mein use hota hai. Comment/code padh kar actual implementation explain karo.

## 9. UPI notification aur receipt OCR

Notification listener ke liye user ka explicit OS access aur feature opt-in chahiye. Parser supported package, rejection keywords, direction aur amount inspect karta hai. Different distinct amounts milen to guess karne ke bajay reject karta hai.

Detected candidate ka stable review ID reference/notification identity se banta hai. Manager pending record store karta hai. User review screen par details correct karke approve karta hai; FinanceViewModel stable wallet ID se entry post karta hai aur phir review status update karta hai.

**Approval ek atomic transaction hai?** Wallet commit aur review APPROVED update separate operations hain. Beech mein failure ho sakta hai. Deterministic wallet ID retry ko duplicate money effect se protect karta hai; cross-operation atomicity claim nahi hai.

**Kya notification proof of payment hai?** Nahi. Text heuristic hai; app bank ledger verify nahi karti. Parser locally run hota hai, lekin review records account cloud collection mein sync ho sakte hain.

**OCR ka role?** ML Kit selected receipt se text nikalta hai. Amount/merchant/category extraction heuristic hai, kyunki receipt mein subtotal, tax, total aur change sab ho sakte hain. User review ke baad hi save hota hai. Isko automatic item-wise AI accounting mat describe karo.

## 10. Friends aur personal udhar mein difference

Personal udhar named-person FinanceRecord entries hain. `LENT` aur `REPAID` net ko positive direction mein le jaate hain; `BORROWED` aur `RECEIVED` negative direction mein. Example: 100 lent, 25 received => 75 receivable. 100 borrowed, 30 repaid => -70 payable.

Connected-friend records separate friendship and friend_transactions collections use karte hain. Same event ko creator aur other participant opposite direction mein dekhte hain. Settlement payerId direction/sign decide karne mein help karta hai; pending/rejected status ko confirmed accounting se alag rakhna hota hai.

QR identifier decode karna account authentication nahi hai. Current manual non-QR input path a derived placeholder user ID banata hai; woh verified email-directory search nahi hai. Repository connection failure par optimistic Friend return kar sakti hai, isliye local success message remote persistence ka independent proof nahi hai.

## 11. Backend, security aur supporting features

**Cloud Functions:** functions/index.js saved friend/space expense create hone ke baad recipients ke tokens fetch karke FCM alert bhejta hai. Notification send fail hone se original saved expense automatically reverse nahi hota. Functions ko source folder hone se deployed assume nahi karna.

**FCM token:** Device delivery address hai; rotate ho sakta hai. Token manager signed-in profile se associate karta hai. Notification permission delivery/display concern hai, account authentication nahi.

**WorkManager:** Deferred/periodic reminder work ke liye hai. Exact-second alarm guarantee nahi deta; OS conditions affect kar sakti hain. Subscription reminder payment execute nahi karta.

**Glance widget:** Launcher par separate host mein render hota hai. Main activity ko intent se open karta hai. Wallet mutation widget update request kar sakti hai.

**Ads:** Consent/SDK readiness ke baad screen-owned banner/native resources load hote hain. Bounded LRU cache attached ads ko evict nahi karta. Transient failures ke first two attempts ke liye bounded retry policy hai.

**Rewards:** Cosmetic coins wallet INR balance se separate hain. Legacy ad-free compatibility methods currently disabled hain; inko active subscription/payment entitlement mat batana.

### Configuration files

`AndroidManifest.xml` Android entry points/permissions define karta hai. `app/build.gradle.kts` SDK, dependencies, signing aur packaging configure karta hai; `gradle/libs.versions.toml` version aliases rakhta hai. `firebase.json` deployment configuration, `firestore.indexes.json` query indexes aur `google-services.json` app-to-Firebase configuration supply karte hain. JSON syntax comments allow nahi karti, isliye un files ke values ko chhede bina unka role yahan explain kiya gaya hai.

### Present server-rule boundaries

- User-owned wallet/finance/budget paths owner checks use karte hain.
- Current nested space-expense rule `allow read, write: if isAuthenticated()` hai. Ismein member-only access ya recipient-only confirmation enforce nahi hota.
- Space membership update alternative ka scope broad hai; UI restrictions server policy ko automatically narrow nahi karti.
- Join-by-code query current member-only space read rule ke saath non-member ke liye reject ho sakti hai. Existing join method errors ko NotFound mein map karta hai.
- Friend transaction update rule participant membership check karti hai; recipient-only confirmation stronger policy ka separate design/test topic hai.

Yeh code inspection ki current boundaries hain. Is comment/documentation change mein authorization behaviour modify ya backend deploy nahi kiya gaya. Security improvement ke liye stricter rules, a supported invite-resolution path aur emulator allow/deny tests chahiye.

## 12. Testing aur demonstration

Unit tests mein MoneyTest, RecordedBalanceTest, UdharTest, SpaceExpenseTest, UpiNotificationParserTest, GoogleSignInErrorsTest aur ad cache/load policy suites hain. Generated ExampleUnitTest sirf arithmetic sanity test hai. Available suite mein 41 local unit tests hain; latest command outcome validation record mein check karo.

Build validation aur device testing different hain. AndroidTest sources compile karna unhe device par execute karne ke barabar nahi hai. DatabaseMigrationTest aur FinanceFlowTest jaise tests ka actual device result alag evidence hoga.

Repository root se verification commands:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --console=plain
node --check functions/index.js
```

Demo order:

1. Sign-in complete karo aur Home ka recorded balance samjhao.
2. Invalid amount `1.234` aur valid `12.50` ka difference dikhao.
3. Ek small expense save karke history aur balance effect explain karo.
4. Edit/delete/undo supported path dikhao aur rollback ka code point out karo.
5. Paise split aur Alice/Bob/Charlie settlement example code ke saath samjhao.
6. Available authenticated space mein pending/confirmed distinction dikhao. Join flow ko demonstration se pehle actual account/backend par verify karo.
7. UPI/OCR available na ho to manual-entry flow aur actual parser/helper explain karo; live successful integration assume mat karo.

## 13. Last-minute revision order

Pehle Money aur BalanceCalculator samjho. Phir HomeViewModel -> WalletRepository ka save/retry flow trace karo. Uske baad pending confirmation, Room vs cloud aur authentication vs authorization revise karo. Last mein UI components, reminders, widget aur ads dekho.

Har answer ke liye code ki ek relevant function dikha paana useful hai. Jo feature current version mein restricted ya incomplete hai, uski boundary aur next implementation step clearly explain karo.
