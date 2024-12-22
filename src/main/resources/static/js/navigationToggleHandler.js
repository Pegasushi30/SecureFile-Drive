function toggleDrawer() {
    const drawer = document.getElementById('drawer');
    const overlay = document.getElementById('drawer-overlay');
    const hamburger = document.getElementById('hamburger-menu');

    // Açık/kapat durumu
    const isOpen = drawer.classList.contains('open');

    // Sınıfları ekle/kaldır
    drawer.classList.toggle('open', !isOpen);
    overlay.classList.toggle('open', !isOpen);
    hamburger.classList.toggle('open', !isOpen);

    // Eğer drawer açılırsa, sayfa kaydırma olayını dinle
    if (!isOpen) {
        window.addEventListener('scroll', closeDrawerOnScroll);
    } else {
        window.removeEventListener('scroll', closeDrawerOnScroll);
    }
}

// Scroll sırasında drawer'ı kapatacak fonksiyon
function closeDrawerOnScroll() {
    const drawer = document.getElementById('drawer');
    const overlay = document.getElementById('drawer-overlay');
    const hamburger = document.getElementById('hamburger-menu');

    if (drawer.classList.contains('open')) {
        drawer.classList.remove('open');
        overlay.classList.remove('open');
        hamburger.classList.remove('open');

        // Scroll olayı dinleyicisini kaldır
        window.removeEventListener('scroll', closeDrawerOnScroll);
    }
}
