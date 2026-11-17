# Cards, identity photos and motion

Home and Activity share one transaction component with a category icon, readable date, prominent exact amount, category chip and separate overflow actions. Amounts have their own row so long names and large balances do not compete for horizontal space. Finance containers use a restrained border, softer spring entrance and animated content-size changes.

Activity list and detail match container, title and amount keys in the same SharedTransitionLayout. Back reverses these transitions. Motion-disabled mode removes shared transitions and snaps finance layout changes; transaction press scaling and avatar crossfades also respect the preference.

UserAvatar observes users/{uid}.photoUrl, with the signed-in Firebase Auth photo as a fallback for the current user. Home, Profile, connected-friend list/details and Space member avatars use it. Name-only personal udhar records have no linked account and retain initials. Photos load through Coil with bounded Compose constraints and the shared image loader caches. See [Coil Compose](https://coil-kt.github.io/coil/compose/) and [network loading](https://coil-kt.github.io/coil/network/).

Validation: debug build and instrumentation APK compile; three card/avatar/Activity navigation tests pass on normal and 360dp compact emulator configurations. Tests exercise long names, maximum allowed amount, separate edit/delete actions, initials fallback and Activity detail forward/back with motion enabled/disabled. A real signed-in photo URL was not fetched during these tests; production account image loading and real-device frame timing remain manual validation items.
