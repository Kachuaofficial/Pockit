# Google sign-in fix

The login button now uses GetSignInWithGoogleOption (explicit button flow) with a foreground activity-based MutableContextWrapper. The previous GetGoogleIdOption bottom-sheet request returned a generic connection message for every failure. Error handling now distinguishes unavailable accounts/providers, interrupted requests, OAuth configuration and actual network failures. Cancellation stays silent and credentials/tokens are never logged.

The connected debug APK's SHA-1 was already registered. Its SHA-256 and the original release APK's SHA-1/SHA-256 were added to the existing Firebase Android app. The refreshed google-services.json includes Android OAuth clients for both certificates. A public Google provider request was accepted by the backend; this diagnostic did not sign in a user.

Verification: debug build and release Kotlin compilation passed; 31 unit tests passed. The updated debug APK was installed as an in-place update with the same certificate, preserving app data. The user confirmed that the Google account picker opened and selecting their account successfully opened Home on the connected phone.

Generate a new signed release export to include these source/config changes; older app/release APKs are not automatically updated. Play-distributed builds must also use a registered Play App Signing certificate, which can differ from the local upload certificate.

References: [Android button flow](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation), [Firebase Google authentication setup](https://firebase.google.com/docs/auth/android/google-signin).
