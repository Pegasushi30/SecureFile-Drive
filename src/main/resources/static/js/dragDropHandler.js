document.addEventListener('DOMContentLoaded', () => {
    const dragDropArea = document.getElementById('dragDropArea');
    const fileDesktopInput = document.getElementById('file-desktop'); // Masaüstü dosya input
    const fileMobileInput = document.getElementById('file-mobile');  // Mobil dosya input
    const fileChosen = document.getElementById('file-chosen');
    const form = document.getElementById('fileUploadForm');

    // Drag-drop işlemleri
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

    // Masaüstü Dosya Seçimi
    fileDesktopInput.addEventListener('change', () => {
        const file = fileDesktopInput.files[0];
        fileChosen.textContent = file ? file.name : 'Henüz dosya seçilmedi';
    });

    // Mobil Dosya Seçimi
    fileMobileInput.addEventListener('change', () => {
        const file = fileMobileInput.files[0];
        if (file) {
            const dataTransfer = new DataTransfer();
            dataTransfer.items.add(file);
            fileDesktopInput.files = dataTransfer.files; // Masaüstü input ile eşleştirildi
            fileChosen.textContent = file.name;
        } else {
            fileChosen.textContent = 'Henüz dosya seçilmedi';
        }
    });

    // Form submit işlemi
    form.addEventListener('submit', (event) => {
        const desktopFiles = fileDesktopInput.files;
        const mobileFiles = fileMobileInput.files;

        if (!desktopFiles.length && !mobileFiles.length) {
            event.preventDefault();
            alert('Lütfen bir dosya seçin!');
        } else {
            console.log('Form gönderiliyor...');
        }
    });
});