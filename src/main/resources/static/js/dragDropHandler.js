const dragDropArea = document.getElementById('dragDropArea');
const fileInput = document.getElementById('file');
const fileChosen = document.getElementById('file-chosen');

// Dosya alanına tıklama
dragDropArea.addEventListener('click', () => {
    fileInput.click();
});

// Dosya sürükleme işlemleri
dragDropArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    dragDropArea.style.backgroundColor = 'rgba(0, 123, 255, 0.2)';
});

dragDropArea.addEventListener('dragleave', () => {
    dragDropArea.style.backgroundColor = 'transparent';
});

dragDropArea.addEventListener('drop', (e) => {
    e.preventDefault();
    dragDropArea.style.backgroundColor = 'transparent';
    const files = e.dataTransfer.files;
    fileInput.files = files;
    fileChosen.textContent = files[0] ? files[0].name : 'Dosya seçilmedi';
});

// Dosya seçildiğinde
fileInput.addEventListener('change', () => {
    fileChosen.textContent = fileInput.files[0] ? fileInput.files[0].name : 'Dosya seçilmedi';
});
