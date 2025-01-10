document.addEventListener('DOMContentLoaded', () => {
    const revokeForms = document.querySelectorAll('.revoke-form');

    revokeForms.forEach(form => {
        form.addEventListener('submit', function (e) {
            e.preventDefault(); // Formun varsayılan davranışını engelle

            const formData = new FormData(this); // Form verilerini al
            const actionUrl = this.getAttribute('action'); // Formun action URL'si

            fetch(actionUrl, {
                method: 'POST',
                body: formData
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Paylaşım iptali başarısız oldu!');
                    }
                    return response.json(); // Backend'den JSON bekleniyor
                })
                .then(data => {
                    console.log("Backend yanıtı:", data);

                    // UI'de paylaşım bilgisini güncelle
                    alert('Paylaşım başarıyla iptal edildi!');

                    // Formun bulunduğu alana göre işlem yap
                    if (this.closest('.shared-directories-table')) {
                        // Eğer "Paylaştığınız Dizinler" tablosundaysa
                        const tableRow = this.closest('tr');
                        if (tableRow) {
                            tableRow.remove(); // Satırı DOM'dan kaldır
                        }

                        const sharedDirectoriesTable = document.querySelector('.shared-directories-table');
                        const tableBody = sharedDirectoriesTable.querySelector('tbody');
                        if (!tableBody || tableBody.children.length === 0) {
                            sharedDirectoriesTable.style.display = 'none'; // Tablonun tamamını gizle
                        }
                    } else if (this.closest('.file-card')) {
                        // Eğer dosyalarla ilgili bir alandaysa
                        const fileCard = this.closest('.file-card');
                        const shareContainer = fileCard.querySelector('.revoke-form').parentElement;

                        // Eğer başka paylaşım yoksa ilgili bölümü gizle
                        if (data.remainingShares === 0) {
                            shareContainer.style.display = 'none';
                        }
                    } else {
                        console.warn('Tanımsız bir alan için işlem yapılmadı.');
                    }
                })
                .catch(error => {
                    console.error(error);
                    alert('Bir hata oluştu: ' + error.message);
                });
        });
    });
});
