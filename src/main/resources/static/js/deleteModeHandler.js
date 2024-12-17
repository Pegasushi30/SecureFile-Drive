document.addEventListener("DOMContentLoaded", function () {
    let deleteModeActive = false;

    function toggleDeleteMode() {
        deleteModeActive = !deleteModeActive;
        const directoryContainer = document.querySelector('.directory-container');

        if (deleteModeActive) {
            directoryContainer.classList.add('delete-mode-active');
            document.getElementById('toggleDeleteMode').textContent = 'Silme Modunu Kapat';
        } else {
            directoryContainer.classList.remove('delete-mode-active');
            document.getElementById('toggleDeleteMode').textContent = 'Silme Modunu Aç';
        }
    }

    // Silme Modu Butonuna Event Listener Ekle
    document.getElementById('toggleDeleteMode').addEventListener('click', toggleDeleteMode);
});
