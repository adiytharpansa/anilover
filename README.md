# Anilover

**Unduh APK terbaru:** [Anilover-2.7.1.apk](https://github.com/adiytharpansa/anilover/raw/main/Anilover-2.7.1.apk)

Aplikasi streaming anime gaya Netflix untuk gudang **project-X**, memakai API
**api.anime** (https://api-anime.adiyth-arpansa.workers.dev).

- Beranda gaya Netflix: billboard, baris "Di Gudang", "Lanjutkan Menonton" dengan bilah progres
- Halaman detail: sinopsis, metadata, daftar episode dengan progres per episode
- Pemutar (Media3/ExoPlayer) bergaya Netflix: ketuk-dua-kali ±10 dtk, panel kecepatan,
  panel audio & subtitle, panel episode, post-play episode berikutnya, PiP, kunci layar,
  gestur kecerahan/volume/geser posisi, zoom layar (pas/isi/zoom), sisa waktu "-mm:ss"
- Video diputar sebagai HLS dari salinan segmen di repo project-X lewat jsDelivr (pin commit)
- Bila masuk dengan Firebase Auth: riwayat tonton, Daftarku, nilai, pengingat, dan pengaturan
  tersinkron otomatis antar perangkat lewat Firestore (`users/{uid}`); tamu tetap lokal
- Pencarian mencocokkan judul, genre, DAN sinopsis

Build: GitHub Actions (`assembleDebug`) — artefak `Anilover-debug-apk`.

## 2.8.0

- **Sinkronisasi cloud (Firestore)**: saat masuk, riwayat tonton (maks 500 entri), Daftarku,
  nilai, sembunyikan-dari-Lanjutkan, pengingat "Segera Tayang", dan pengaturan tersinkron
  antar perangkat — yang lebih baru menang, perubahan lokal diunggah otomatis (debounce
  1,5 dtk), perubahan dari HP lain masuk realtime. Tamu & unduhan tetap lokal.
  Lihat `PANDUAN-FIREBASE.md` (aturan `users/{userId}` sudah disertakan).
- **Pencarian lebih luas**: kata kunci kini juga dicocokkan dengan sinopsis judul.
- **Urutan "Baru Ditambahkan" lebih kokoh**: memakai cap waktu terbit (`addedAt`) dari
  katalog bila tersedia; bila tidak, tetap urutan katalog (terakhir masuk = terbaru).
- Koreksi komentar kode: lencana 18+ tidak lagi dikaitkan dengan filter profil yang belum ada.
- **Closed Caption (CC) di pemutar**: trek CEA-608/708 bawaan stream HLS (dari
  `#EXT-X-MEDIA TYPE=CLOSED-CAPTIONS` atau in-band di segmen TS) kini tampil di panel
  "Audio & Subtitle" sebagai bagian **Closed Caption (CC)** berlabel CC1/CC3; trek berbahasa
  "und" otomatis terpilih bila tidak ada subtitle bahasa pilihan, dan bisa dijadikan default
  lewat opsi baru "CC bawaan" di Profil.
- **Ikon aplikasi dirapikan & diseragamkan**: segitiga play sakura (#FF96B6 → #FF5C85,
  sama dengan `AniloverAccent`) ber-sudut-bulat di atas dasar `#0D0E17` (`AniloverBackground`) —
  untuk launcher (legacy + adaptive v26), splash, logo dalam aplikasi (transparan), ikon
  notifikasi, dan Play Store 512px; sebelumnya hijau polos di atas putih.

## Perubahan 2.7.0

**Tampilan (gaya Netflix)**
- Billboard potret: poster penuh, judul di tengah, tombol Putar + Daftarku sejajar, chip genre melayang di atasnya.
- Bilah atas melayang dengan gradasi, menghitam pekat saat digeser.
- Top 10: angka raksasa kini terlihat (sebelumnya garis tepi sewarna latar sehingga angka tak tampak).
- Baris yang berisi judul identik tidak lagi diulang ("Di Gudang" = "Action" = "Drama" dsb.).
- Kolom cari berlatar solid, tombol kembali & judul di Detail lebih tegas, versi di Profil dibaca dari APK.

**Bug stream**
- Ketuk layar tidak pernah menampilkan/menyembunyikan kontrol & ketuk-dua-kali ±10 dtk mati: dua lapisan gestur bertumpuk, yang atas menelan sentuhan. Digabung jadi satu.
- Bilah seek tidak bisa diketuk (hanya diseret). Kini ketuk = loncat.
- Autoplay/"Episode Berikutnya" memakai nilai basi sehingga bisa salah episode; kini selalu membaca nilai terbaru.
- Tombol "putar ulang" episode terakhir tidak pernah hilang; status "selesai" kini direset.
- Durasi episode sebelumnya bocor ke episode baru (tombol "Episode Berikutnya" muncul sesaat); kini direset.
- Progres episode yang baru selesai/ditinggalkan disimpan tepat saat pindah.
- Suara tetap jalan saat layar mati/aplikasi ditutup; kini dijeda di onStop. Ditambah fokus audio + jeda saat headset dicabut.
- Jaringan: User-Agent, timeout 15/20 dtk, redirect lintas-protokol, retry segmen 6x, buffer lebih besar, mime HLS dipaksa, dan coba ulang otomatis 3x (1/2/4 dtk) dari posisi terakhir. Pesan galat kini jelas + kode galat + tombol Kembali.
- Unduhan: playlist lokal dulu ditulis di AWAL, jadi episode setengah-unduh/gagal diputar dari berkas lalu error. Kini playlist ditulis terakhir, segmen lewat berkas .part, nama berkas dinormalkan (subfolder/query/init fMP4/kunci), master playlist ditangani, dan unduhan yang tertinggal "mengunduh" ditandai gagal saat app dibuka.

## 2.7.1
- Crash "background can not be translucent: #0" saat pemutar dibuka: tombol Cast (`MediaRouteButton`) dibuat dengan tema yang tidak punya `colorPrimary` opak. Ditambah tema `Theme.Anilover.Cast`, `colorPrimary` di tema pemutar, dan try/catch agar kegagalan Cast tidak menutup aplikasi.

## Desain ulang UI ("Tinta")
- Sistem desain baru di `ui/Theme.kt`: palet biru-tinta + aksen sakura, skala radius (`AniloverShapes`), skala tipografi, `ScreenTitle`, `panel()`, warna Switch/Chip seragam.
- Beranda: hero unggulan, Lanjutkan Menonton, Baru Ditambahkan, Jelajahi Genre, Semua Judul.
- Tab Baru, Unduhan, Profil, Cari: judul halaman, panel bergaris tepi, kontrol seragam.
- Pemutar: tombol, panel, seek bar, dan HUD memakai token yang sama.
- Transisi tab searah urutan tab, Detail masuk dengan skala halus, skeleton Beranda mengikuti layout baru.
