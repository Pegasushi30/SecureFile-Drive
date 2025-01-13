document.addEventListener('DOMContentLoaded', () => {
    const dragDropArea = document.getElementById('dragDropArea');
    const fileDesktopInput = document.getElementById('file-desktop'); // Desktop file input
    const fileMobileInput = document.getElementById('file-mobile');  // Mobile file input
    const fileChosen = document.getElementById('file-chosen');
    const form = document.getElementById('fileUploadForm');

    dragDropArea.addEventListener('click', () => fileDesktopInput.click());
    dragDropArea.addEventListener('dragover', (event) => {
        event.preventDefault();
        dragDropArea.classList.add('dragging');
    });
    dragDropArea.addEventListener('dragleave', () => dragDropArea.classList.remove('dragging'));
    dragDropArea.addEventListener('drop', (event) => {
        event.preventDefault();
        dragDropArea.classList.remove('dragging');
        const files = event.dataTransfer.files;

        if (files.length > 0) {
            const dataTransfer = new DataTransfer();
            for (let i = 0; i < files.length; i++) {
                dataTransfer.items.add(files[i]);
            }
            fileDesktopInput.files = dataTransfer.files;
            fileChosen.textContent = files[0].name;
        }
    });

    fileDesktopInput.addEventListener('change', () => {
        const file = fileDesktopInput.files[0];
        fileChosen.textContent = file ? file.name : 'No file selected yet';
    });

    fileMobileInput.addEventListener('change', () => {
        const file = fileMobileInput.files[0];
        if (file) {
            const dataTransfer = new DataTransfer();
            dataTransfer.items.add(file);
            fileDesktopInput.files = dataTransfer.files; // Sync with desktop input
            fileChosen.textContent = file.name;
        } else {
            fileChosen.textContent = 'No file selected yet';
        }
    });

    form.addEventListener('submit', (event) => {
        const desktopFiles = fileDesktopInput.files;
        const mobileFiles = fileMobileInput.files;

        if (!desktopFiles.length && !mobileFiles.length) {
            event.preventDefault();
            alert('Please select a file!');
        } else {
            console.log('Form is being submitted...');
        }
    });
});
