function filterFiles() {
    const query = document.getElementById('searchQuery').value.toLowerCase();
    const fileCards = document.querySelectorAll('.file-card');

    fileCards.forEach(card => {
        const fileName = card.querySelector('span').innerText.toLowerCase();
        if (fileName.includes(query)) {
            card.style.display = "block"; // Eşleşen dosyayı göster
        } else {
            card.style.display = "none"; // Eşleşmeyen dosyayı gizle
        }
    });
}