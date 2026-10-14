ANDROID STUDIO KE BINA BUILD (GitHub se, free) - sirf browser chahiye

1. github.com par free account banao.
2. New repository banao. Naam: candyblast. **PRIVATE** chuno (zaroori, kyunki keystore wahan hogi).
3. Is folder ke SAARE files repo mein upload karo:
   "Add file" > "Upload files". .github folder bhi jana chahiye
   (agar laptop par .github dikhta nahi, hidden files ON karo).
   Commit karo.
4. Repo > Actions tab > "Build Candy Blast" > Run workflow. 5-10 minute mein khatam.
5. Run khulne par neeche Artifacts mein "CandyBlast-test-apk" download karo.
   Phone mein install karke test karo (test ads aayenge).
6. KEY BANANA (sirf ek baar):
   Actions > "Make upload key (run ONCE)" > Run workflow > password likho (yaad rakho).
   Khatam hone par artifact "your-keystore" download karo (upload-keystore.jks + keystore-base64.txt).
   DONO FILES KA BACKUP rakho (Drive + pendrive). Kho di toh update nahi kar paoge.
7. Repo > Settings > Secrets and variables > Actions > New repository secret. 4 secrets banao:
   KEYSTORE_BASE64   = keystore-base64.txt ke andar ka poora text
   KEYSTORE_PASSWORD = jo password aapne chuna
   KEY_ALIAS         = upload
   KEY_PASSWORD      = wahi password
8. Actions > "Build Candy Blast" dobara Run. Is baar "CandyBlast-release-aab" signed AAB hoga.
9. Wo .aab Play Console mein upload karo.

DHYAN:
- Repo PRIVATE rakho.
- Agar app pehle us converter ki key se Play Console par upload thi, toh wahi key chahiye
  ya Play Console > App integrity > upload key reset ka option dekho.
- versionCode: app/build.gradle.kts mein, pehle upload hua ho toh bada rakho.
