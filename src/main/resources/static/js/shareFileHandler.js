document.addEventListener('DOMContentLoaded', () => {
    const shareForms = document.querySelectorAll('.share-form');

    shareForms.forEach(form => {
        form.addEventListener('submit', function (e) {
            e.preventDefault(); // Prevent default form submission behavior

            const fileId = this.querySelector('input[name="fileId"]').value;
            const username = this.querySelector('input[name="username"]').value;
            const sharedWithEmail = this.querySelector('input[name="sharedWithEmail"]').value;
            const version = this.querySelector('select[name="version"]').value;

            const formData = new FormData();
            formData.append('username', username);
            formData.append('fileId', fileId);
            formData.append('sharedWithEmail', sharedWithEmail);
            formData.append('version', version);

            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

            fetch(this.action, {
                method: 'POST',
                body: formData,
                headers: {
                    [csrfHeader]: csrfToken
                }
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => { throw new Error(err.error || 'File sharing failed!'); });
                    }
                    return response.json();
                })
                .then(data => {
                    console.log("Backend response:", data);

                    alert(data.message || 'File successfully shared.');

                    const fileCard = this.closest('.file-card');
                    if (fileCard) {
                        const revokeContainer = fileCard.querySelector('.revoke-form')?.parentElement;
                        if (revokeContainer) {
                            revokeContainer.style.display = 'block';
                        }
                    }

                    this.reset();

                    const revokeForm = document.querySelector(`#revoke-form-${fileId}`);
                    if (revokeForm) {
                        const versionDropdown = revokeForm.querySelector('select[name="version"]');
                        const emailDropdown = revokeForm.querySelector('select[name="sharedWithEmail"]');

                        if (versionDropdown) {
                            versionDropdown.value = "";
                        }
                        if (emailDropdown) {
                            emailDropdown.value = "";
                        }
                    }
                })
                .catch(error => {
                    console.error(error);
                    alert('An error occurred: ' + error.message);
                });
        });
    });
});
