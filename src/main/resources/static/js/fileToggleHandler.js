document.addEventListener('DOMContentLoaded', function() {
    const toggleFileMode = document.getElementById('toggleFileMode');
    const body = document.querySelector('body');

    if (!toggleFileMode.checked) {
        body.classList.add('file-minimized');
    }

    toggleFileMode.addEventListener('change', function() {
        if (toggleFileMode.checked) {
            body.classList.remove('file-minimized');
        } else {
            body.classList.add('file-minimized');
        }
    });
});
