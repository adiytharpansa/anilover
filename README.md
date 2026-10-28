# REA

Aplikasi streaming anime gaya Netflix untuk gudang **project-X**, memakai API
**api.anime** (https://api-anime.adiyth-arpansa.workers.dev).

**Unduh APK:** [REA-2.3.0.apk](https://github.com/adiytharpansa/rea-app/raw/main/REA-2.3.0.apk)

## REA 2.3.0 — Segera Tayang, Ingatkan Saya, Unduhan Cerdas

- Tab **Baru**: bagian **Segera Tayang** — judul yang sedang disalin ke gudang
  (dari API `/api/soon`) dengan banner, sinopsis, dan tombol **Ingatkan Saya**.
- **Notifikasi judul baru**: pemeriksa katalog berkala (tiap ±6 jam) memberi
  tahu bila ada judul baru, dan kabar khusus "Sudah tayang di REA" untuk judul
  yang kamu ingatkan. Jalan pertama hanya mencatat keadaan awal.
- **Unduhan Cerdas** (tab Unduhan): episode berikutnya terunduh otomatis dan
  unduhan episode yang selesai ditonton terhapus sendiri; opsi **Hanya Wi-Fi**.
- Indikator **penyimpanan perangkat** (bebas/total + bilah pemakaian).

## REA 2.2.0 — baris & rasa Netflix

- **10 Teratas di REA Hari Ini**: baris peringkat dengan angka raksasa bergaris
  tepi khas Netflix (peringkat dari skor katalog).
- **Karena Anda Menonton …**: rekomendasi dari judul terakhirmu (genre sama;
  judul yang kamu nilai Tidak Suka dikeluarkan).
- **Baru Ditambahkan** + lencana **BARU** di kartu judul terbaru.
- **Menu tekan-lama** di kartu: Putar, Detail, Daftarku, Unduh Episode 1.
- Penilaian ala Netflix di Detail: **Tidak Suka / Suka / Sangat Suka**
  (ketuk jempol atas berurutan).
- Tab Cari: bagian **Sedang Tren Sekarang** saat kolom kosong.
- **Tampilan Subtitle** di Profil: ukuran (Kecil/Sedang/Besar) + warna
  (Putih/Kuning/Hijau) teks subtitle di pemutar.
- Dukungan data API: judul/sinopsis/gambar cuplikan per episode, lencana 18+,
  dan trailer billboard — tampil otomatis bila gudang menyediakan datanya.

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
