# REA

Aplikasi streaming anime gaya Netflix untuk gudang **project-X**, memakai API
**api.anime** (https://api-anime.adiyth-arpansa.workers.dev).

- Beranda gaya Netflix: billboard, baris "Di Gudang", "Lanjutkan Menonton" dengan bilah progres
- Halaman detail: sinopsis, metadata, daftar episode dengan progres per episode
- Pemutar (Media3/ExoPlayer) bergaya Netflix: ketuk-dua-kali ±10 dtk, panel kecepatan,
  panel audio & subtitle, panel episode, post-play episode berikutnya, PiP, kunci layar,
  gestur kecerahan/volume/geser posisi, zoom layar (pas/isi/zoom), sisa waktu "-mm:ss"
- Video diputar sebagai HLS dari salinan segmen di repo project-X lewat jsDelivr (pin commit)

Build: GitHub Actions (`assembleDebug`) — artefak `REA-debug-apk`.
