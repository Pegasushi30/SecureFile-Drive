document.addEventListener('DOMContentLoaded', () => {
    const deleteForms = document.querySelectorAll('.delete-form');

    deleteForms.forEach(form => {
        form.addEventListener('submit', function (e) {
            e.preventDefault();

            const button = this.querySelector('.delete-btn');
            const fileId = button.getAttribute('data-file-id');
            const versionNumber = this.querySelector('select[name="versionNumber"]').value;
            const usernameFromForm = this.querySelector('input[name="username"]').value;

            if (!versionNumber) {
                alert('Please select a version to delete.');
                return;
            }

            if (!confirm('Are you sure you want to delete this file version?')) {
                return;
            }

            const actionUrl = this.getAttribute('action');

            const formData = new FormData();
            formData.append('versionNumber', versionNumber);
            formData.append('username', usernameFromForm);

            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

            fetch(actionUrl, {
                method: 'DELETE',
                body: formData,
                headers: {
                    [csrfHeader]: csrfToken
                }
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => { throw new Error(err.error || 'File deletion failed!'); });
                    }
                    return response.json();
                })
                .then(data => {
                    console.log("Backend response:", data);

                    const fileCard = this.closest('.file-card');
                    if (fileCard) {
                        fileCard.remove();
                    }

                    alert(data.message || 'File successfully deleted!');
                })
                .catch(error => {
                    console.error(error);
                    alert('An error occurred: ' + error.message);
                });
        });
    });
});
