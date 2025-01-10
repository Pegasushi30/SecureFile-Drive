document.addEventListener('DOMContentLoaded', () => {
    const deleteForms = document.querySelectorAll('.delete-form');

    deleteForms.forEach(form => {
        form.addEventListener('submit', function (e) {
            e.preventDefault(); // Formun varsayılan davranışını engelle

            const button = this.querySelector('.delete-btn');
            const fileId = button.getAttribute('data-file-id');
            const versionNumber = this.querySelector('select[name="versionNumber"]').value;
            const usernameFromForm = this.querySelector('input[name="username"]').value;

            if (!versionNumber) {
                alert('Lütfen silinecek versiyonu seçiniz.');
                return;
            }

            // Onay dialogu
            if (!confirm('Bu dosya versiyonunu silmek istediğinizden emin misiniz?')) {
                return;
            }

            // Endpoint URL
            const actionUrl = this.getAttribute('action');

            // Form verilerini oluştur
            const formData = new FormData();
            formData.append('versionNumber', versionNumber);
            formData.append('username', usernameFromForm);
            // `_method=delete` eklemeye gerek yok çünkü yeni endpoint DELETE metodunu doğrudan kabul ediyor

            // CSRF Token
            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

            fetch(actionUrl, {
                method: 'DELETE', // DELETE metodunu doğrudan kullanıyoruz
                body: formData,
                headers: {
                    [csrfHeader]: csrfToken // CSRF token'ı ekleyin
                }
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => { throw new Error(err.error || 'Dosya silme işlemi başarısız oldu!'); });
                    }
                    return response.json();
                })
                .then(data => {
                    console.log("Backend yanıtı:", data);

                    // UI'de dosyayı kaldır
                    const fileCard = this.closest('.file-card');
                    if (fileCard) {
                        fileCard.remove();
                    }

                    alert(data.message || 'Dosya başarıyla silindi!');
                })
                .catch(error => {
                    console.error(error);
                    alert('Bir hata oluştu: ' + error.message);
                });
        });
    });
});
