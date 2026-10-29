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
