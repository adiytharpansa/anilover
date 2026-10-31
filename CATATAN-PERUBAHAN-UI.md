# Catatan perubahan UI (tampilan rapi & modern)

Berkas baru
- app/src/main/java/com/app/anilover/ui/Components.kt  -> PosterImage (shimmer saat memuat, kartu cadangan bila gagal/URL kosong, crossfade) + PosterFallback
- app/src/main/res/font/poppins_{regular,medium,bold}.ttf  -> font Poppins (lisensi OFL)

Berkas diubah
- ui/Theme.kt        : font Poppins + skala tipografi lengkap, AniloverShapes.Poster (10dp)
- ui/HomeScreen.kt   : ditulis ulang (lihat daftar di bawah)
- ui/Screens.kt, ui/DetailScreen.kt : AsyncImage -> PosterImage (semua poster/banner punya placeholder & fallback)
- MainActivity.kt    : tepi-ke-tepi (enableEdgeToEdge), insets, navbar baru, sambungan ke HomeScreen baru

Daftar perbaikan
1.  Poster kosong -> shimmer saat memuat + kartu cadangan (ikon + judul) bila gagal.
2.  Teks kartu terpotong -> tinggi kaku 42dp dihapus; judul maks. 2 baris, sub-judul selalu terlihat.
3.  Konten dobel -> hero bukan judul yang sedang ditonton; "Semua Judul" kini "Koleksi Lainnya" (tanpa judul yang sudah ada di "Baru Ditambahkan").
4.  Navbar -> garis tipis hanya di atas, ikon terisi (aktif) / garis (tidak aktif), tanpa indikator pil, ikon "Baru" = NewReleases.
5.  Ikon dobel -> Cari & Profil dihapus dari header; diganti ikon lonceng (membuka tab "Baru").
6.  Hero -> pager 5 judul + indikator titik + putar otomatis 6 dtk (berhenti bila ada trailer / disentuh); menembus status bar; header transparan -> solid saat scroll; chip genre di atas gambar dihapus.
7.  Poster tanpa border, sudut 10dp; lencana "BARU" dikecilkan.
8.  Lebar poster seragam di semua baris & grid (3 kolom, kartu ke-4 mengintip).
9.  "Lihat semua >" di judul bagian; baris Top 10 (skor tertinggi) & "Pilihan <genre>" (2 genre teratas).
10. Kartu Lanjutkan Menonton -> info "E1 - Sisa mm:ss" di atas thumbnail + gradient, judul 1 baris.
11. Font Poppins + skala tipografi di Theme.kt (Text bawaan otomatis ikut karena MaterialTheme).
12. Animasi -> animateItem() pada baris Lanjutkan Menonton (hapus item mulus), efek tekan poster tetap.

---
# Tambahan: Masuk / Daftar dengan Firebase Auth

Berkas baru
- data/AuthRepository.kt : pembungkus Firebase Auth (masuk, daftar, reset kata sandi, keluar, mode tamu, pesan galat Indonesia)
- ui/AuthScreen.kt       : AuthGate + layar Masuk/Daftar (validasi, tampil/sembunyi sandi, loading, "Lupa kata sandi?", "Lanjut tanpa akun")
- PANDUAN-FIREBASE.md    : langkah setup Firebase Console + letak google-services.json

Berkas diubah
- build.gradle.kts & app/build.gradle.kts : plugin google-services (aktif otomatis hanya bila app/google-services.json ada), Firebase BoM 33.1.2 + firebase-auth
- MainActivity.kt        : AuthRepository.init, AuthGate membungkus AppRoot, data akun & Keluar dikirim ke Profil
- ui/Screens.kt (ProfileScreen) : nama/email akun asli, tombol Keluar (dengan konfirmasi) / Masuk atau daftar untuk tamu

---
# Tambahan: Muat otomatis & efek segarkan di Beranda

- MainActivity.kt : fungsi loadCatalog() bersama; tarik-untuk-segarkan (refreshHome); muat otomatis saat aplikasi dibuka kembali (data > 3 menit) dan berkala tiap 5 menit saat aplikasi terlihat. Bila gagal, data lama tetap tampil + toast.
- ui/HomeScreen.kt : PullToRefreshBox (Material3) membungkus daftar; indikator ditaruh di bawah header melayang. Parameter baru: isRefreshing, onRefresh.

---
# Tambahan: Halaman pemutar khusus + komentar

- player/PlayerActivity.kt : tombol Putar kini membuka halaman tontonan. Potret = video 16:9 di atas (kontrol ringkas) + halaman info; lanskap / tombol layar penuh = video penuh seperti sebelumnya. Putar perangkat otomatis masuk/keluar layar penuh; tombol Kembali keluar dari layar penuh dulu.
- player/WatchPage.kt (baru) : judul + info, aksi (Daftarku/Tidak Suka/Suka/Bagikan), baris episode, komentar (Teratas/Terbaru, balasan, suka, hapus milik sendiri, kolom tulis tersemat).
- data/CommentRepository.kt (baru) : komentar realtime di Firestore, per episode.
- MainActivity.kt : kirim info skor/tahun/genre ke pemutar. AndroidManifest.xml : orientasi tidak lagi dikunci lanskap.
- app/build.gradle.kts : dependensi firebase-firestore. Setup: lihat PANDUAN-FIREBASE.md (bagian Komentar).

---
# Tambahan: Sinkronisasi cloud (Firestore) saat masuk — v2.8.0

- data/CloudSync.kt (baru) : satu dokumen `users/{uid}` di Firestore berisi riwayat tonton
  (maks 500 entri terbaru), Daftarku, nilai, sembunyikan-dari-Lanjutkan, pengingat, dan
  pengaturan. Aktif hanya saat pengguna masuk; tamu tetap lokal.
- Penggabungan saat masuk: yang lebih baru menang (server timestamp); selama sesi,
  perubahan lokal diunggah otomatis (debounce 1,5 dtk) dan perubahan dari perangkat lain
  diterapkan realtime. `clientId` (UUID perangkat) + hash data mencegah loop antar perangkat.
- data/Api.kt : HistoryStore.replaceAll + LibraryStore.replaceMyList/replaceRatings/
  replaceHidden/replaceReminders/replaceSettings untuk menerapkan data cloud ke lokal.
- MainActivity.kt : CloudSync.attach(...) setelah AuthRepository.init.
- PANDUAN-FIREBASE.md : aturan Firestore baru `users/{userId}` (hanya pemilik yang baca/tulis).

---
# Tambahan: Pencarian mencakup sinopsis — v2.8.0

- ui/Screens.kt (SearchScreen) : hasil pencarian kini juga mencocokkan sinopsis judul,
  tidak hanya judul & genre.

---
# Tambahan: Urutan "Baru" lebih kokoh — v2.8.0

- data/Api.kt : Title kini membaca cap waktu terbit opsional (`addedAt`/`added`, detik
  maupun milidetik); helper `List<Title>.newestFirst()` memakai cap itu bila ada,
  jika tidak kembali ke asumsi lama (urutan katalog dibalik).
- ui/HomeScreen.kt & ui/Screens.kt (NewScreen) : baris "Baru Ditambahkan"/"Koleksi Lainnya"
  memakai newestFirst() sehingga tidak lagi bergantung mentah pada urutan katalog.

---
# Koreksi kecil — v2.8.0

- data/Api.kt : komentar field `adult` tidak lagi menjanjikan "filter profil Anak"
  (yang memang tidak diimplementasikan); lencana 18+ tetap tampil di halaman detail.

---
# Tambahan: Closed Caption (CC) di pemutar — v2.8.0

- player/PlayerActivity.kt : deteksi trek CEA-608/708 (isCeaFormat) + label ala aplikasi
  populer (ccLabel: "CC1 (CC)" dari INSTREAM-ID); panel "Audio & Subtitle" kini memisahkan
  bagian Subtitle dan Closed Caption (CC).
- loadEpisode: setSelectUndeterminedTextLanguage(true) agar CC (bahasa "und") otomatis
  terpilih bila tidak ada trek subtitle bahasa pilihan; bila pengaturan = "cc", listener
  onTracksChanged memilih trek CC pertama secara otomatis.
- ui/Screens.kt (Profil): opsi subtitle bawaan baru "CC bawaan" (nilai "cc").

---
# Ikon aplikasi dirapikan & diseragamkan — v2.8.0

- Ikon baru: segitiga play sakura (#FF96B6 -> #FF5C85, sama dengan AniloverAccent) dengan
  sudut membulat + glow lembut di atas dasar #0D0E17 (AniloverBackground) — sebelumnya
  kotak hijau polos di atas putih yang tidak nyambung dengan tema "Tinta" aplikasi.
- Semua aset diperbarui dari satu master 1024px: mipmap ic_launcher (48-192, kotak & round),
  ic_launcher_foreground transparan (108-432, untuk adaptive icon v26 & splash),
  anilover_logo.png (logo di dalam aplikasi: layar masuk & header Beranda, kini transparan),
  dan ic_launcher-playstore.png (512).
- ic_launcher_background.xml kini #0D0E17 (dulu #000000); ic_stat_anilover.xml (notifikasi)
  kini segitiga play putih ber-sudut-bulat, sama dengan mark ikon.
