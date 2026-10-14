CANDY BLAST - ANDROID PROJECT (sirf AdMob + Google consent)

KYA HAI IS PROJECT MEIN
- Aapki game HTML: app/src/main/assets/index.html
- Sirf AdMob. Unity / AppLovin / Vungle aur faltu plugins nahi hain.
- Google UMP consent popup (EU/UK users ke liye), Ad Privacy Options button settings mein.
- Home screen par banner, hint ke liye rewarded ad (3 free hints ke baad).
- Debug build = TEST ads automatically. Release build = REAL ads.

BUILD KARNE KE STEPS (laptop par)
1. Android Studio install karo (developer.android.com/studio). Pehli baar chalane par SDK 36 download hone do.
2. Android Studio > Open > is folder "CandyBlast-AndroidProject" ko chuno.
   Sync (Gradle) chalne do. Internet chahiye. Kuch minute lagte hain.
3. TEST: phone par USB debugging ON karke Run dabao (debug build, test ads dikhenge).
   - Banner sirf home screen par aana chahiye.
   - 3 hint free, 4th hint par rewarded test ad.
4. EU popup test (optional): Logcat mein "ConsentDebugSettings" search karke device hash lo,
   ConsentManager.kt mein TEST_DEVICE_HASH mein daalo, debug run karo. Baad mein wapas "" kar do.
5. RELEASE AAB: Build > Generate Signed App Bundle / APK > Android App Bundle.
   Keystore banao aur USKA BACKUP + PASSWORD SAHI SE RAKHO (kho diya toh update nahi kar paoge).
6. app/build.gradle.kts mein versionCode: agar Play Console par pehle koi version upload hai,
   toh versionCode usse bada rakho (1 -> 2 -> 3 ...).
7. AAB ko Play Console mein upload karo.

DHYAN DO
- Release build (real ads) ko apne phone par install karke ads par TAP mat karo. Account ban ho sakta hai.
- AdMob > Privacy & messaging mein European regulations ka message banake PUBLISH karo, warna popup nahi aayega.
- Play Console: Target audience 13+, "Contains ads" = Yes, Data safety, Advertising ID declaration,
  aur public privacy policy URL.
- Agar app pehle Play par kisi aur key se upload hui thi, toh Play Console > App integrity mein dekho
  ki upload key kaunsi hai.
- play-store-icon-512.png Play listing ke 512x512 icon ke liye hai.
