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
