document.addEventListener('DOMContentLoaded', () => {
    const directoryTitles = document.querySelectorAll('.directory-title');

    directoryTitles.forEach(directoryTitle => {
        const icon = directoryTitle.querySelector('.toggle-icon');
        const fileGrid = directoryTitle.nextElementSibling;

        icon.className = 'fas fa-plus toggle-icon';

        directoryTitle.addEventListener('click', () => {
            const isOpen = fileGrid.style.display === 'flex';
            fileGrid.style.display = isOpen ? 'none' : 'flex';
            icon.className = isOpen ? 'fas fa-plus toggle-icon' : 'fas fa-minus toggle-icon';
        });
    });
});
