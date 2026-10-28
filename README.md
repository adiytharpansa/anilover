# REA

Aplikasi streaming anime gaya Netflix untuk gudang **project-X**, memakai API
**api.anime** (https://api-anime.adiyth-arpansa.workers.dev).

**Unduh APK:** [REA-2.6.2.apk](https://github.com/adiytharpansa/rea-app/raw/main/REA-2.6.2.apk)

## REA 2.6.2 — perbaikan keluar sendiri + pelapor galat

- **Cari**: `remember` saran hasil kosong dipindah ke tingkat atas — versi
  2.6.0 menaruhnya di cabang kondisional yang merusak state Compose saat
  hasil berubah kosong/ada (aplikasi keluar sendiri).
- **Pemutar**: inisialisasi Cast kini menangkap semua jenis galat (termasuk
  Error yang lolos dari penangkap lama); state gestur seret dipindah keluar
  blok kondisional.
- **Detail**: kunci item daftar distabilkan; tab aktif menjadi nilai turunan.
- **Pelapor galat bawaan**: bila aplikasi berhenti, jejak galatnya disimpan;
  saat aplikasi dibuka lagi muncul jendela "Aplikasi sempat berhenti" dengan
  tombol Bagikan untuk diagnosis.

## REA 2.6.0 — versi final hasil riset Netflix (bedah APK pakai REA)

- **Chip kategori di Beranda** + halaman kategori: ketuk genre untuk melihat
  semua judul genre itu dalam kisi.
- **Detail bertab ala Netflix**: Episode | Trailer & Lainnya | Lainnya Seperti
  Ini, dengan garis merah di atas tab aktif.
- **Trailer inline di Detail**: trailer memutar bisu otomatis di hero Detail
  dan dapat diputar bersuara dari tab Trailer (aktif bila katalog menyediakan
  trailer).
- **Tombol Putar lengket**: bilah Putar/Lanjutkan menempel di bawah saat
  daftar episode digeser.
- **Pencarian suara**: ikon mikrofon di kolom Cari (pengenalan suara bawaan
  Android).
- **Riwayat pencarian**: chip kata terakhir yang dicari + tombol Hapus.
- **Hasil Cari kosong yang membantu**: saran judul populer, bukan halaman
  kosong.

Yang sengaja TIDAK dibuat dari hasil riset: umpan klip vertikal (keputusan
pemilik), billboard karusel dan tombol Lewati Kredit (tidak terbukti ada di
APK Netflix).

## REA 2.5.0 — polish UI/UX

- **Animasi layar**: perpindahan tab memudar+bergeser; halaman Detail muncul
  mengambang dari bawah.
- **Shimmer**: rangka pemuatan katalog kini berkilau berdenyut.
- **Billboard parallax**: tertinggal perlahan dan memudar saat beranda digeser;
  gradasi bawahnya menyatu ke daftar.
- **Bilah atas lengket**: logo REA tetap di atas dan menghitam pekat saat scroll.
- **Efek tekan kartu**: kartu poster/Top 10/lanskap memampat halus saat ditekan.
- **HUD gestur pemutar**: ikon besar + bilah kecerahan/volume gaya Netflix.
- **Keadaan kosong berikon**: Daftarku & Unduhan kosong tampil berikon lingkaran.
- **Splash beranimasi**: logo membesar memudar saat aplikasi dibuka.

## REA 2.4.0 — Cast ke TV + billboard video

- **Cast ke TV**: tombol Cast resmi (Google Cast) di bar atas pemutar; episode
  streaming yang sedang diputar dikirim ke Chromecast/Android TV lewat Default
  Media Receiver, melanjutkan dari posisi saat ini. Sesi berakhir -> pemutaran
  lokal lanjut dari posisi TV. Episode unduhan tidak bisa di-cast (beri tahu).
- **Billboard video**: bila katalog menyediakan trailer, billboard beranda
  memutar video trailer bisu berulang ala Netflix; tanpa trailer tetap gambar.
- **Lewati Intro** kini membaca batas intro dari data API (`introEnd`) bila
  gudang menyediakannya; tanpa data tetap heuristik 90 detik.

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
