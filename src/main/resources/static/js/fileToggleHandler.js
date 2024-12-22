document.addEventListener('DOMContentLoaded', function() {
    const toggleFileMode = document.getElementById('toggleFileMode');
    const body = document.querySelector('body');

    // Sayfa yüklendiğinde default durumu ayarla
    // Eğer checkbox başta işaretli değilse (toggle off), minimal mod olsun
    if (!toggleFileMode.checked) {
        body.classList.add('file-minimized');
    }

    toggleFileMode.addEventListener('change', function() {
        if (toggleFileMode.checked) {
            // Checkbox ON ise detaylı mod
            body.classList.remove('file-minimized');
        } else {
            // Checkbox OFF ise minimal mod
            body.classList.add('file-minimized');
        }
    });
});
