document.addEventListener('DOMContentLoaded', () => {
    const revokeForms = document.querySelectorAll('.revoke-form');

    revokeForms.forEach(form => {
        form.addEventListener('submit', function (e) {
            e.preventDefault();

            const formData = new FormData(this);
            const actionUrl = this.getAttribute('action');

            fetch(actionUrl, {
                method: 'POST',
                body: formData
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to revoke the share!');
                    }
                    return response.json();
                })
                .then(data => {
                    console.log("Backend response:", data);

                    alert('Share successfully revoked!');

                    if (this.closest('.shared-directories-table')) {
                        const tableRow = this.closest('tr');
                        if (tableRow) {
                            tableRow.remove();
                        }

                        const sharedDirectoriesTable = document.querySelector('.shared-directories-table');
                        const tableBody = sharedDirectoriesTable.querySelector('tbody');
                        if (!tableBody || tableBody.children.length === 0) {
                            sharedDirectoriesTable.style.display = 'none';
                        }
                    } else if (this.closest('.file-card')) {
                        const fileCard = this.closest('.file-card');
                        const shareContainer = fileCard.querySelector('.revoke-form').parentElement;

                        if (data.remainingShares === 0) {
                            shareContainer.style.display = 'none';
                        }
                    } else {
                        console.warn('No specific action taken for this area.');
                    }
                })
                .catch(error => {
                    console.error(error);
                    alert('An error occurred: ' + error.message);
                });
        });
    });
});
