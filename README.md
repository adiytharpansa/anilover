# REA

Aplikasi streaming anime gaya Netflix untuk gudang **project-X**, memakai API
**api.anime** (https://api-anime.adiyth-arpansa.workers.dev).

**Unduh APK:** [REA-2.1.0.apk](https://github.com/adiytharpansa/rea-app/raw/main/REA-2.1.0.apk)

## REA 2.1.0 — fondasi profesional

- **Keystore tetap resmi** (`debug.keystore` di repo, kunci yang sama untuk debug
  & rilis): update berikutnya menimpa langsung — riwayat, Daftarku, pengaturan,
  dan unduhan TIDAK lagi terhapus saat ganti versi. Catatan: memasang 2.1.0 di
  atas 2.0.0 tetap perlu copot-pasang **satu kali terakhir**, karena kunci lama
  dibuat acak oleh runner.
- **Build rilis** (R8/minify): APK 2,6 MB (dari 20,5 MB), ditandatangani kunci tetap.
- **Splash screen** bermerek REA saat aplikasi dibuka.
- Poster & banner JJK S3 kini gambar resmi dari gudang sendiri (jsDelivr):
  poster 460x650, banner lanskap 1762x371 — bukan lagi gambar potret yang dipotong.
- Judul kartu dua baris utuh: nama seri tebal + baris season di bawahnya.
- Beranda: baris **Episode** per episode (ketuk langsung putar, progres tampil).
- Ikon billboard satu gaya garis; navigasi bawah tanpa pil merah, ikon "Baru" baru.
- Profil: avatar gradasi, statistik (episode selesai / judul ditonton / Daftarku),
  pengaturan berikon + pemisah, Daftarku kosong diberi keterangan.
- Target sentuh diperbesar (≥48dp di tombol aksi utama).

Fitur 2.0.0 tetap: navigasi bawah 5 tab, billboard % kecocokan, Cari + chip genre,
Baru, Unduhan offline, Detail lengkap (Daftarku/Sukai/Bagikan/Judul Serupa),
pemutar Media3 (Lewati Intro, Episode Berikutnya, kualitas, kecepatan, PiP, kunci).

Build: GitHub Actions (`assembleRelease`) — artefak `REA-release-apk`.
