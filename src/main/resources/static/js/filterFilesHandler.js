function filterFiles() {
    const emailQuery = document.getElementById('emailSearch').value.toLowerCase().trim();
    const fileQuery = document.getElementById('fileSearch').value.toLowerCase().trim();
    const emailGroups = document.querySelectorAll('.email-group');

    emailGroups.forEach(emailGroup => {
        const emailText = emailGroup.querySelector('.section-title span').innerText.toLowerCase();
        const directoryGroups = emailGroup.querySelectorAll('.directory-group');
        let emailMatch = emailText.includes(emailQuery);
        let anyDirectoryMatch = false;

        directoryGroups.forEach(directoryGroup => {
            const directoryTitle = directoryGroup.querySelector('.directory-title span').innerText.toLowerCase();
            const fileCards = directoryGroup.querySelectorAll('.file-card');
            let directoryMatch = directoryTitle.includes(emailQuery);
            let anyFileMatch = false;

            fileCards.forEach(fileCard => {
                const fileName = fileCard.querySelector('.file-name').innerText.toLowerCase();

                const senderMatch = emailText.includes(emailQuery);
                const fileMatch = fileName.includes(fileQuery);

                if ((emailQuery === "" || senderMatch) && (fileQuery === "" || fileMatch)) {
                    fileCard.style.display = "flex";
                    anyFileMatch = true;
                } else {
                    fileCard.style.display = "none";
                }
            });

            if (directoryMatch || anyFileMatch || (emailQuery === "" && fileQuery === "")) {
                directoryGroup.style.display = "block";
                anyDirectoryMatch = true;
            } else {
                directoryGroup.style.display = "none";
            }
        });

        if (emailMatch || anyDirectoryMatch || (emailQuery === "" && fileQuery === "")) {
            emailGroup.style.display = "block";
        } else {
            emailGroup.style.display = "none";
        }
    });

    const sharedFilesContainer = document.getElementById('shared-files-container');
    const visibleEmailGroups = sharedFilesContainer.querySelectorAll('.email-group:not([style*="display: none"])');

    if (visibleEmailGroups.length === 0) {
        sharedFilesContainer.querySelector('.no-files-message').style.display = "block";
    } else {
        sharedFilesContainer.querySelector('.no-files-message').style.display = "none";
    }
}
