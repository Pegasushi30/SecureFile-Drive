function toggleDrawer() {
    const drawer = document.getElementById('drawer');
    const overlay = document.getElementById('drawer-overlay');
    const hamburger = document.getElementById('hamburger-menu');


    const isOpen = drawer.classList.contains('open');


    drawer.classList.toggle('open', !isOpen);
    overlay.classList.toggle('open', !isOpen);
    hamburger.classList.toggle('open', !isOpen);


    if (!isOpen) {
        window.addEventListener('scroll', closeDrawerOnScroll);
    } else {
        window.removeEventListener('scroll', closeDrawerOnScroll);
    }
}

function closeDrawerOnScroll() {
    const drawer = document.getElementById('drawer');
    const overlay = document.getElementById('drawer-overlay');
    const hamburger = document.getElementById('hamburger-menu');

    if (drawer.classList.contains('open')) {
        drawer.classList.remove('open');
        overlay.classList.remove('open');
        hamburger.classList.remove('open');

        window.removeEventListener('scroll', closeDrawerOnScroll);
    }
}
