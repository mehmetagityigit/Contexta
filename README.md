# Contexta — Bağlam Temelli İngilizce Çalışma Asistanı

Contexta, İngilizce öğrenen ve Genel İngilizce, SAT, IELTS veya AP / Akademik İngilizce hedeflerine hazırlanan kullanıcılar için geliştirilmiş native Android (Kotlin + Jetpack Compose + Material 3) uygulamasıdır.

> **Önemli Not:** SAT, IELTS ve AP etiketleri yalnızca kullanıcı çalışma hedefi ve içerik sınıflandırması amacıyla kullanılır. Uygulama hiçbir resmî sınav ortaklığı, resmî sınav sorusu veya sınav puanı tahmini iddiasında bulunmaz.

## Temel Özellikler

- **Yüzen Kelime Yakalama (`FloatingCaptureService`):** Video izlerken veya okuma yaparken Android `WindowManager` (`TYPE_APPLICATION_OVERLAY`) üzerinden açılan sürüklenebilir yüzen balon ile kelime, bağlam cümlesi ve kaynak bilgisi kaydetme. İzin iptalinde temiz kapanma ve hata durumunda yazılmış metni koruma.
- **Bas-Konuş İngilizce Sesli Yakalama (`VoiceCaptureController`):** `SpeechRecognizer` (`en-US`) ile uygulama içi veya yüzen panel üzerinden sesli kelime ekleme; sistem yüzen panelde mikrofonu kısıtlarsa otomatik olarak uygulama içi ses ekranına yönlendirme.
- **Kelime Defteri ve SM-2 Aralıklı Tekrar (`SpacedRepetitionEngine`):** Arama, filtreleme (`Tümü`, `Bugün`, `Yeni`, `Favoriler`), çoklu bağlam örnekleri (`ContextExample`), isteğe bağlı AI kelime ipucu (`FirebaseAiWordHelperRepository`) ve deterministik SM-2 flashcard tekrar oturumu.
- **Ayrılmış 3 Çalışma Modülü:**
  - **Grammar:** 15 konu, Türkçe özetler, sık hatalar ve açıklamalı çoktan seçmeli mini testler (45 soru).
  - **Reading:** 2 CEFR grubunda (B1–B2, C1–C2) 4 türde 8 akademik metin (32 soru), metin boyutu ayarı, metinden dokunarak bağlamıyla kelime kaydetme ve *Command of Evidence* paragraf vurgulama.
  - **Listening:** 3 ders (Akademik Ders, Diyalog, Kısa Hikâye — 12 soru), Media3 ExoPlayer ve dürüstçe etiketlenmiş Android TTS modu, oynatma hızı, açılır transkriptten kelime kaydetme.
- **Gerçek İlerleme Analizi (`PracticeScoringEngine`):** 7 ve 30 günlük gerçek çalışma süresi, tekrar edilen kart sayısı, bölüm doğruluk oranları ve %80 altı zayıf alanlar (uydurulmuş skor veya sahte istatistik yok).
- **Ayarlar ve Veri Taşınabilirliği:** Türkçe / İngilizce arayüz, Sistem / Açık / Koyu tema, JSON dışa aktarma ve şema doğrulamalı JSON içe aktarma, hesap ve tüm verileri silme.

## Kurulum ve Çalıştırma

### Gereksinimler
- Android Studio (Meerkat veya üzeri önerilir) / JDK 11+ / Gradle
- Firebase projesi (`app/google-services.json` ve `app/src/main/res/values/firebase_applet_config.xml`)

### Kimlik Doğrulama Notu
Bu sürümde Firebase Auth için **Google ile Giriş (`CredentialManager` / `GetSignInWithGoogleOption`)** ve **Demo Modu** etkindir. Demo Modu bellek içi izole çalışır ve hiçbir veriyi Firestore'a yazmaz. E-posta/şifre ile kayıt ve şifre sıfırlama desteği ilerleyen adımda Firebase Konsolu üzerinden etkinleştirilerek eklenebilir.

### İçerik Doğrulama ve Yükleme (Harici CLI)
```bash
# 1. JSON içerik paketlerini şema, ID, CEFR ve kanıt referanslarına göre doğrula
node tools/validate_content.mjs

# 2. Yükleme önizlemesi (Dry-run)
node tools/seed_firestore_content.mjs

# 3. Emulator veya GOOGLE_APPLICATION_CREDENTIALS ile Firestore'a upsert
FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 node tools/seed_firestore_content.mjs --apply
```

### Derleme ve Yerel Testler
```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
