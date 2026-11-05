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
# Tambahan: Palet warna mengikuti ikon

- ui/Theme.kt : AniloverAccent = #6FFA42 (hijau ikon), AniloverOnAccent = hitam, AniloverGold & AniloverGreen = alias aksen, AniloverAccentSoft (gradasi), AniloverDanger (merah hanya untuk Hapus/Keluar/galat).
- res/values/themes.xml : colorAccent tema pemutar & Cast jadi hijau.
- Semua halaman (Masuk/Daftar, Beranda, Detail, Cari, Baru, Daftarku, Unduhan, Profil, Pemutar, Komentar) memakai token yang sama, jadi otomatis ikut.
- WatchPage.kt : avatar komentar memakai palet hijau/putih/abu; PlayerActivity.kt : spinner memuat hijau; AuthScreen.kt : semburat atas lebih halus.

---
# Tambahan: Masuk/Daftar & komentar tanpa Firebase (mode lokal)

- data/AuthRepository.kt : bila Firebase belum dipasang, Masuk/Daftar memakai akun lokal (PBKDF2 + salt, sesi tersimpan). init() kini idempotent.
- ui/AuthScreen.kt : AuthGate selalu menampilkan layar Masuk/Daftar (atau "Lanjut tanpa akun"); "Lupa kata sandi?" hanya bila Firebase aktif.
- data/CommentRepository.kt : komentar lokal (balasan, suka, hapus, urutan Teratas/Terbaru) saat Firebase belum aktif; Firestore tetap dipakai bila ada.
- MainActivity.kt & PlayerActivity.kt : memanggil AuthRepository.init + CommentRepository.init; tombol Keluar selalu tersedia di Profil.
- player/WatchPage.kt : keterangan "Komentar tersimpan di perangkat ini" di mode lokal.

---
# Tambahan: Bahasa mengikuti perangkat

- Semua teks tampilan dipindah ke resource: res/values/strings.xml (Inggris, bahasa cadangan) dan res/values-in/strings.xml (Indonesia). HP berbahasa Indonesia -> Indonesia; bahasa lain -> Inggris.
- Str.kt : Str.get(R.string.x, ...) / Str.plural(R.plurals.x, n) bisa dipanggil dari mana saja (Composable, Service, Worker). AniloverApp.kt mengisinya saat aplikasi mulai (didaftarkan di AndroidManifest.xml).
- res/xml/locales_config.xml + android:localeConfig : di Android 13+ muncul pilihan bahasa per-aplikasi (Pengaturan > Aplikasi > Anilover > Bahasa).
- Format angka skor & nama bahasa trek audio/subtitle memakai bahasa perangkat.
- Menambah bahasa baru: salin values/strings.xml ke values-<kode>/ lalu terjemahkan, tambahkan <locale> di locales_config.xml.
- Tidak ikut diterjemahkan: judul, sinopsis, dan genre dari katalog (datanya dari server), serta pesan diagnostik teknis internal.

## Profil — rapikan total
- `ProfileScreen` dipindah ke `ui/ProfileScreen.kt` (signature sama, MainActivity tidak berubah).
- Header bergradasi hijau, avatar bercincin, lencana status email.
- Statistik jadi 3 kartu berikon; Daftarku kosong tampil dalam kartu.
- Pengaturan dipecah jadi kartu: Autoplay, Subtitle bawaan (segmented, tidak lagi terpotong "No na ktif"), Tampilan Subtitle (pratinjau langsung, segmented ukuran, pilihan warna bulat).
- Riwayat: tombol bersihkan selebar penuh (nonaktif bila kosong). Keluar: merah + ikon; tamu: tombol hijau Masuk/Daftar.
- String baru: `subtitle_preview` (id & en).

## Pengaturan — halaman tersendiri
- Layar baru `ui/SettingsScreen.kt`, dibuka dari ikon gear / menu "Pengaturan" di Profil; tombol Kembali sistem menutupnya.
- Kelompok: Pemutaran (autoplay, subtitle bawaan, tampilan subtitle + pratinjau), Unduhan (Unduhan Cerdas, Hanya Wi-Fi), Riwayat, Akun (keluar / masuk), versi aplikasi.
- Profil kini hanya: header, statistik, Daftarku, menu Pengaturan (+ tombol Masuk untuk tamu). Parameter `ProfileScreen` disederhanakan.
- Komponen bersama (SettingCard, Segmented, ColorSwatch, dll.) ada di SettingsScreen.kt.
- String baru: settings_title, settings_hint, settings_playback, settings_account (id & en).

## Beranda — hero naik penuh ke atas layar
- Poster hero tetap layar penuh seperti semula (HomeScreen.kt tidak diubah); strip hitam di atasnya dihilangkan.
- Tema: statusBarColor transparan; values-v28/themes.xml menambah windowLayoutInDisplayCutoutMode=shortEdges.
- MainActivity: setelah enableEdgeToEdge, dipaksa setDecorFitsSystemWindows(false), status bar transparan, tanpa contrast scrim, dan cutout mode shortEdges/always supaya konten menembus area poni.

## Beranda — ukuran poster ala Netflix
- Top 10: poster ±33% lebar layar (min 112dp, maks 170dp), angka bergaris ikut membesar proporsional; poster menimpa angka ±12% lebarnya.
- Baru Ditambahkan: dari banner lanskap 16:9 menjadi poster potret 2:3 (±34% lebar layar, ±2,7 kartu terlihat), dengan lencana BARU, menu tekan-lama, dan tombol + seperti baris poster lain.

## Cari — dirapikan
- Judul + kolom cari tetap di atas; semua isi di bawahnya satu halaman gulir (LazyColumn), tidak ada lagi grid yang menggulir sendiri di sisa ruang.
- Kolom cari berbentuk pil dengan tombol hapus (×) saat mengetik + tombol mikrofon.
- Chip genre & riwayat memakai gaya bulat bersemburat hijau (terpilih = hijau penuh) dan menggulir sampai tepi layar.
- "Sedang Tren": angka peringkat besar bergaris tepi, poster 68dp, judul 2 baris, genre, skor ★ + jumlah episode (persentase kembar dihapus).
- Grid poster kini punya judul bagian: "Telusuri Semua Judul" / "N hasil"; hasil kosong menampilkan "Rekomendasi untuk Anda".
- String baru: search_browse_all, search_results_count, search_suggestions, clear_search (id & en).

---
# Tambahan: Unduhan ke penyimpanan perangkat + layar Unduhan dirapikan

- data/Api.kt (DownloadManager): segmen HLS digabung (dekripsi AES-128 bila ada) menjadi SATU berkas video MP4 (dikemas ulang tanpa encode ulang lewat MediaExtractor+MediaMuxer; audio berkodek tak didukung dilewati, dicoba ulang video-saja; fMP4 tetap .mp4, aliran TS yang tetap gagal dikemas ditandai gagal) di penyimpanan perangkat: Movies/Anilover lewat MediaStore (Android 10+, muncul di Galeri/File, tetap ada setelah aplikasi dihapus, tanpa izin). Android 9 ke bawah: folder Movies milik aplikasi. Aliran tak lazim (byte-range/SAMPLE-AES) memakai salinan HLS di folder aplikasi seperti sebelumnya. DownloadItem.uri menyimpan alamat video; catatan otomatis dibuang bila videonya dihapus dari Galeri/File.
- player/PlayerActivity.kt: berkas video lokal (content:// / file://) diputar tanpa memaksa tipe M3U8.
- ui/Screens.kt (DownloadsScreen): kartu penyimpanan (bar: terpakai lain vs unduhan Anilover), Unduhan Cerdas di kartu sendiri, unduhan dikelompokkan per judul (poster + ringkasan + baris episode dengan ikon putar/progres/hapus), status kosong rata tengah.
- MainActivity.kt: meneruskan downloader.storageRoot() ke DownloadsScreen.
- strings (id/en): downloads_saved_in, downloading_title, episode_n.
