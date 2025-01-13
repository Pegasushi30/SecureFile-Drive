document.addEventListener("DOMContentLoaded", function () {
    let deleteModeActive = false;

    function toggleDeleteMode() {
        deleteModeActive = !deleteModeActive;
        const directoryContainer = document.querySelector('.directory-container');

        if (deleteModeActive) {
            directoryContainer.classList.add('delete-mode-active');
            document.getElementById('toggleDeleteMode').textContent = 'Disable Delete Mode';
        } else {
            directoryContainer.classList.remove('delete-mode-active');
            document.getElementById('toggleDeleteMode').textContent = 'Enable Delete Mode';
        }
    }

    document.getElementById('toggleDeleteMode').addEventListener('click', toggleDeleteMode);
});
