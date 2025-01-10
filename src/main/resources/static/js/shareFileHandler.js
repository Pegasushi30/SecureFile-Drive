document.addEventListener('DOMContentLoaded', () => {
    const shareForms = document.querySelectorAll('.share-form');

    shareForms.forEach(form => {
        form.addEventListener('submit', function (e) {
            e.preventDefault(); // Formun varsayılan davranışını engelle

            const fileId = this.querySelector('input[name="fileId"]').value;
            const username = this.querySelector('input[name="username"]').value;
            const sharedWithEmail = this.querySelector('input[name="sharedWithEmail"]').value;
            const version = this.querySelector('select[name="version"]').value;

            // Form verilerini oluştur
            const formData = new FormData();
            formData.append('username', username);
            formData.append('fileId', fileId);
            formData.append('sharedWithEmail', sharedWithEmail);
            formData.append('version', version);

            // CSRF Token
            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

            fetch(this.action, {
                method: 'POST',
                body: formData,
                headers: {
                    [csrfHeader]: csrfToken // CSRF token'ı ekleyin
                }
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => { throw new Error(err.error || 'Dosya paylaşım işlemi başarısız oldu!'); });
                    }
                    return response.json();
                })
                .then(data => {
                    console.log("Backend yanıtı:", data);

                    alert(data.message || 'Dosya başarıyla paylaşıldı.');

                    // Revoke formunu görünür hale getir
                    const fileCard = this.closest('.file-card');
                    if (fileCard) {
                        const revokeContainer = fileCard.querySelector('.revoke-form')?.parentElement;
                        if (revokeContainer) {
                            revokeContainer.style.display = 'block';
                        }
                    }

                    // Formu sıfırla
                    this.reset();

                    // Revoke formundaki dropdown'ları varsayılan duruma getir
                    const revokeForm = document.querySelector(`#revoke-form-${fileId}`);
                    if (revokeForm) {
                        const versionDropdown = revokeForm.querySelector('select[name="version"]');
                        const emailDropdown = revokeForm.querySelector('select[name="sharedWithEmail"]');

                        if (versionDropdown) {
                            versionDropdown.value = ""; // Varsayılan duruma getir
                        }
                        if (emailDropdown) {
                            emailDropdown.value = ""; // Varsayılan duruma getir
                        }
                    }
                })
                .catch(error => {
                    console.error(error);
                    alert('Bir hata oluştu: ' + error.message);
                });
        });
    });
});
