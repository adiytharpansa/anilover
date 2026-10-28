# REA

Aplikasi streaming anime gaya Netflix untuk gudang **project-X**, memakai API
**api.anime** (https://api-anime.adiyth-arpansa.workers.dev).

**Unduh APK:** [REA-2.0.0.apk](https://github.com/adiytharpansa/rea-app/raw/main/REA-2.0.0.apk)

## REA 2.0.0 — setara Netflix

- Navigasi bawah 5 tab: Beranda, Cari, Baru, Unduhan, Profil
- Beranda: billboard (persen kecocokan, chip status/HD, sinopsis, tombol Daftarku),
  baris Lanjutkan Menonton (menu hapus dari baris), Daftarku, Di Gudang,
  Skor Tertinggi, Sedang Tayang, Serial Tamat, baris per genre — semua dinamis
- Cari: kata kunci + chip genre + grid poster; Baru: daftar judul terbaru
- Detail: thumbnail episode + progres, Daftarku, Sukai, Bagikan, sinopsis bisa dibuka,
  unduh per episode, Judul Serupa
- Unduhan offline: unduh playlist + segmen per episode, putar tanpa internet dari tab Unduhan
- Profil: autoplay episode berikutnya, subtitle bawaan (Indonesia/Inggris/Nonaktif),
  bersihkan riwayat, Daftarku
- Cache katalog: beranda langsung tampil dari cache, disegarkan di belakang layar
- Pemutar (Media3/ExoPlayer): ketuk-dua-kali ±10 dtk, kecepatan, panel audio & subtitle,
  panel episode, panel kualitas, Lewati Intro, tombol Episode Berikutnya, post-play
  hitung mundur (menghormati pengaturan autoplay), PiP, kunci layar, gestur
  kecerahan/volume/geser posisi, zoom (pas/isi/zoom), sisa waktu "-mm:ss"
- Video diputar sebagai HLS dari salinan segmen di repo project-X lewat jsDelivr (pin commit)

Build: GitHub Actions (`assembleDebug`) — artefak `REA-debug-apk`.
