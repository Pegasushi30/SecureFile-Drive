document.addEventListener('DOMContentLoaded', () => {
    const revokeForms = document.querySelectorAll('.revoke-form');

    revokeForms.forEach(form => {
        form.addEventListener('submit', async function (e) {
            e.preventDefault();

            try {
                const formData = new FormData(this);
                const actionUrl = this.getAttribute('action');

                // Fetch ile POST isteği
                const response = await fetch(actionUrl, {
                    method: 'POST',
                    body: formData
                });

                // Yanıt doğrulama
                if (!response.ok) {
                    // Sunucudan hata mesajı geldiyse çek
                    let errorData;
                    try {
                        errorData = await response.json();
                    } catch (_) {
                        errorData = {};
                    }
                    const errMsg = (errorData && errorData.error)
                        ? errorData.error
                        : 'Failed to revoke the share!';
                    throw new Error(errMsg);
                }

                // Sunucudan dönen JSON'u parse et
                const data = await response.json();
                console.log("Backend response:", data);

                // İsteğin başarılı olduğunu varsayarsak:
                alert(data.message || 'Share successfully revoked!');

                // 1) Eğer shared-directories-table içindeysek (Directory Revoke)
                if (this.closest('.shared-directories-table')) {
                    const tableRow = this.closest('tr');
                    if (tableRow) {
                        tableRow.remove(); // satırı DOM'dan kaldır
                    }

                    const sharedDirectoriesTable = document.querySelector('.shared-directories-table');
                    if (sharedDirectoriesTable) {
                        const tableBody = sharedDirectoriesTable.querySelector('tbody');
                        // Eğer tablo gövdesinde hiç row kalmadıysa tabloyu gizle
                        if (!tableBody || tableBody.children.length === 0) {
                            sharedDirectoriesTable.style.display = 'none';
                        }
                    }

                    // 2) Eğer file-card içindeysek (File Revoke)
                } else if (this.closest('.file-card')) {
                    const fileCard = this.closest('.file-card');
                    // .revoke-form'u bulup parentElement'ini yakala
                    const revokeFormElement = fileCard.querySelector('.revoke-form');
                    if (revokeFormElement) {
                        const shareContainer = revokeFormElement.parentElement;
                        // backend remainingShares döndürüyorsa, 0'sa revokeblok'u gizle
                        if (typeof data.remainingShares !== 'undefined' && data.remainingShares === 0) {
                            shareContainer.style.display = 'none';
                        }
                    }

                    // 3) Diğer durumlar (örneğin fallback)
                } else {
                    console.warn('No specific action taken for this area.');
                }

            } catch (error) {
                console.error(error);
                alert('An error occurred: ' + error.message);
            }
        });
    });
});
