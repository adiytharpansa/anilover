# Panduan memasang Firebase Auth (Masuk / Daftar)

Kode sudah siap. Yang perlu kamu lakukan hanya menyiapkan proyek Firebase dan menaruh satu berkas.

## 1. Buat proyek Firebase
1. Buka https://console.firebase.google.com -> **Add project** (nama bebas, Analytics boleh dimatikan).
2. Di halaman proyek, klik ikon **Android** (Add app).
3. **Android package name**: `com.app.anilover` (harus persis sama).
4. Nama panggilan aplikasi: Anilover. SHA-1 boleh dikosongkan (tidak perlu untuk email + kata sandi).
5. Klik **Register app**, lalu **Download google-services.json**.

## 2. Aktifkan metode masuk
Firebase Console -> **Build -> Authentication -> Get started -> Sign-in method -> Email/Password -> Enable -> Save**.

## 3. Taruh berkas konfigurasi
Salin `google-services.json` ke folder **app/** proyek ini:

    Projek_Anilover/
      app/
        google-services.json   <-- di sini (sejajar dengan app/build.gradle.kts)

Commit & push seperti biasa; GitHub Actions akan membangun APK dengan Firebase aktif.

## Cara kerjanya
- **Ada** `app/google-services.json` -> layar Masuk/Daftar muncul saat aplikasi dibuka.
  Pengguna bisa Masuk, Daftar, "Lupa kata sandi?" (email reset dari Firebase), atau "Lanjut tanpa akun" (tamu).
  Sesi tersimpan, jadi tidak perlu login ulang tiap membuka aplikasi.
- **Tidak ada** berkasnya -> build tetap berhasil dan aplikasi berjalan seperti sebelumnya (tanpa layar masuk).
- Tab **Profil** menampilkan nama + email akun dan tombol **Keluar dari akun**
  (untuk tamu: tombol **Masuk atau daftar**).

## Opsional yang disarankan
- Authentication -> Settings -> **User actions**: biarkan "Email enumeration protection" aktif (default).
- Google Cloud Console -> Credentials -> batasi API key Android dengan package `com.app.anilover` + SHA-1 keystore.
- Template email reset kata sandi bisa diterjemahkan ke Indonesia di Authentication -> Templates.

## Catatan batasan
- Riwayat tonton, Daftarku, nilai, pengingat, dan pengaturan **tersinkron otomatis saat masuk**
  (lihat bagian "Sinkronisasi cloud" di bawah bagian Komentar). Mode tamu dan unduhan
  (berkas video) tetap **lokal di perangkat**.
- Masuk dengan Google belum ada (butuh Web client ID + SHA-1 + Credential Manager).

---
# Komentar (Cloud Firestore)

Halaman pemutar punya kolom komentar (balasan, suka, hapus komentar sendiri, realtime).
Datanya disimpan di Firestore, jadi aktifkan satu kali:

1. Firebase Console -> **Build -> Firestore Database -> Create database** (mode produksi, pilih lokasi terdekat, mis. asia-southeast2/Jakarta).
2. Tab **Rules**, tempel aturan berikut lalu **Publish**:

        rules_version = '2';
        service cloud.firestore {
          match /databases/{database}/documents {
            match /comments/{id} {
              allow read: if true;
              allow create: if request.auth != null
                && request.resource.data.uid == request.auth.uid
                && request.resource.data.text is string
                && request.resource.data.text.size() > 0
                && request.resource.data.text.size() <= 500;
              allow delete: if request.auth != null && resource.data.uid == request.auth.uid;
              // Orang lain hanya boleh mengubah daftar "suka" (likedBy).
              allow update: if request.auth != null
                && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['likedBy']);
            }
          }
        }

3. Tidak perlu membuat indeks. Hanya pengguna yang sudah masuk yang bisa menulis; tamu hanya membaca.
- Tanpa Firebase (tidak ada google-services.json) bagian komentar menampilkan "Komentar belum aktif".
