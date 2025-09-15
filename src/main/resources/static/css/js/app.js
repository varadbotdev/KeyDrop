// Global JavaScript for P2P Sharing Portal

document.addEventListener('DOMContentLoaded', function() {
    // Initialize any global functionality here

    // Add smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();

            const targetId = this.getAttribute('href');
            if (targetId === '#') return;

            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                window.scrollTo({
                    top: targetElement.offsetTop - 100,
                    behavior: 'smooth'
                });
            }
        });
    });

    // Add file size validation
    const fileInput = document.getElementById('file');
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            if (this.files.length > 0) {
                const fileSize = this.files[0].size; // in bytes
                const maxSize = 50 * 1024 * 1024; // 50MB

                if (fileSize > maxSize) {
                    alert('File size exceeds the maximum limit of 50MB');
                    this.value = ''; // Clear the file input
                }
            }
        });
    }

    // Form validation
    const shareForm = document.querySelector('.share-form');
    if (shareForm) {
        shareForm.addEventListener('submit', function(e) {
            const textContent = document.getElementById('text')?.value.trim() || '';
            const fileInput = document.getElementById('file');
            const hasFile = fileInput && fileInput.files.length > 0;

            if (!textContent && !hasFile) {
                e.preventDefault();
                alert('Please provide either text content or a file to share.');
                return false;
            }

            // Show loading state
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Sharing...';
            }
        });
    }

    // Copy to clipboard functionality
    const copyButtons = document.querySelectorAll('.copy-btn');
    copyButtons.forEach(button => {
        button.addEventListener('click', function() {
            const textToCopy = this.getAttribute('data-clipboard-text');
            if (textToCopy) {
                navigator.clipboard.writeText(textToCopy).then(() => {
                    const originalText = this.textContent;
                    this.textContent = 'Copied!';
                    setTimeout(() => {
                        this.textContent = originalText;
                    }, 2000);
                }).catch(err => {
                    console.error('Failed to copy: ', err);
                    alert('Failed to copy to clipboard');
                });
            }
        });
    });
});

// Utility function to format file size
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Add a global error handler
window.addEventListener('error', function(e) {
    console.error('An error occurred:', e.error);
});

// Add online/offline detection
window.addEventListener('online', function() {
    console.log('Connection restored');
});

window.addEventListener('offline', function() {
    alert('You are offline. Some features may not work.');
});