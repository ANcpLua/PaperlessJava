class DocumentAPI {
    constructor(baseURL = '/api/documents') {
        this.baseURL = baseURL;
    }

    async handleResponse(response) {
        if (!response.ok) {
            const error = new Error(response.statusText);
            error.status = response.status;
            throw error;
        }
        return response.json();
    }

    async uploadDocument(file) {
        const formData = new FormData();
        formData.append('file', file);
        const response = await fetch(this.baseURL, {
            method: 'POST',
            body: formData
        });
        return this.handleResponse(response);
    }

    async renameDocument(id, newName) {
        const response = await fetch(`${this.baseURL}/${id}?newName=${encodeURIComponent(newName)}`, {
            method: 'PATCH'
        });
        return this.handleResponse(response);
    }

    async downloadDocument(id, filename) {
        const response = await fetch(`${this.baseURL}/${id}/download`);
        if (!response.ok) {
            throw new Error('Download failed');
        }
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename || `document-${id}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    }

    async deleteDocument(id) {
        const response = await fetch(`${this.baseURL}/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            throw new Error('Delete failed');
        }
    }

    async listAllDocuments() {
        const response = await fetch(this.baseURL);
        return this.handleResponse(response);
    }

    async searchDocuments(query) {
        const response = await fetch(`${this.baseURL}/search?query=${encodeURIComponent(query)}`);
        return this.handleResponse(response);
    }
}

class DocumentUIController {
    constructor() {
        this.api = new DocumentAPI();
        this.lastDocStates = new Map();
        this.initializeUI();
        this.setupAutoRefresh();
    }

    initializeUI() {
        this.searchInput = document.getElementById('search-input');
        this.searchButton = document.getElementById('search-button');
        this.fileInput = document.getElementById('file-input');
        this.tableBody = document.getElementById('documents-table-body');
        this.loader = document.getElementById('loader');
        this.progressBar = document.getElementById('progress-bar');
        this.uploadButton = document.getElementById('upload-button');

        this.searchButton.addEventListener('click', () => this.handleSearch());
        this.searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleSearch();
        });
        this.fileInput.addEventListener('change', (e) => this.handleFileUpload(e));
        this.uploadButton.addEventListener('click', () => this.fileInput.click());
        this.tableBody.addEventListener('click', (e) => {
            const viewOcrButton = e.target.closest('button[data-ocr-text]');
            if (viewOcrButton) {
                const encodedText = viewOcrButton.dataset.ocrText;
                this.showOcrText(encodedText);
            }
        });
        const dropZone = document.querySelector('.upload-area');
        dropZone.style.cursor = 'pointer';
        dropZone.addEventListener('click', () => this.fileInput.click());

        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
            });
        });
        dropZone.addEventListener('drop', (e) => this.handleDrop(e));

        this.loadDocuments();
    }

    setupAutoRefresh() {
        setInterval(() => {
            this.loadDocuments(false);
        }, 7000);
    }

    showToast(message, type = 'success') {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = `toast toast-${type}`;
        toast.style.display = 'block';
        setTimeout(() => {
            toast.style.display = 'none';
        }, 2000);
    }

    toggleLoader(show) {
        this.loader.style.display = show ? 'flex' : 'none';
    }

    async loadDocuments(resetSearch = true) {
        try {
            this.toggleLoader(true);
            const query = resetSearch ? '' : this.searchInput.value.trim();
            let docs;
            if (!query) {
                docs = await this.api.listAllDocuments();
            } else {
                docs = await this.api.searchDocuments(query);
            }
            this.renderDocuments(docs);
            this.checkOcrCompletions(docs);
        } catch (error) {
            console.error('Failed to load documents:', error);
            this.showToast('Failed to load documents', 'error');
        } finally {
            this.toggleLoader(false);
        }
    }

    checkOcrCompletions(docs) {
        docs.forEach(doc => {
            const prevState = this.lastDocStates.get(doc.id);
            if (prevState === false && doc.ocrJobDone === true) {
                this.showToast(`OCR completed for "${doc.filename}"`, 'success');
            }
        });
        this.lastDocStates.clear();
        docs.forEach(doc => {
            this.lastDocStates.set(doc.id, doc.ocrJobDone);
        });
    }

    async handleSearch() {
        const query = this.searchInput.value.trim();
        try {
            this.toggleLoader(true);
            const docs = await this.api.searchDocuments(query);
            if (!docs || docs.length === 0) {
                this.showToast('No documents found', 'info');
            }
            this.renderDocuments(docs || []);
            this.checkOcrCompletions(docs || []);
        } catch (error) {
            console.error('Search error:', error);
            this.showToast(`Search failed: ${error.message || 'Unknown error'}`, 'error');
            this.renderDocuments([]);
        } finally {
            this.toggleLoader(false);
        }
    }

    async handleFileUpload(event) {
        const file = event.target.files[0];
        if (!file) return;
        try {
            this.toggleLoader(true);
            this.progressBar.style.width = '50%';
            await this.api.uploadDocument(file);
            this.showToast('Document uploaded successfully');
            await this.loadDocuments();
        } catch (error) {
            this.showToast('Upload failed', 'error');
            console.error(error);
        } finally {
            this.toggleLoader(false);
            this.progressBar.style.width = '0';
            this.fileInput.value = '';
        }
    }

    async handleDrop(event) {
        const file = event.dataTransfer.files[0];
        if (file) {
            await this.handleFileUpload({ target: { files: [file] } });
        }
    }

    async handleRename(id, currentName) {
        const newName = prompt('Enter new name:', currentName);
        if (!newName || newName === currentName) return;
        try {
            this.toggleLoader(true);
            await this.api.renameDocument(id, newName);
            this.showToast('Document renamed successfully');
            await this.loadDocuments(false);
        } catch (error) {
            this.showToast('Rename failed', 'error');
        } finally {
            this.toggleLoader(false);
        }
    }

    async handleDelete(id) {
        if (!confirm('Are you sure you want to delete this document?')) return;
        try {
            this.toggleLoader(true);
            await this.api.deleteDocument(id);
            this.showToast('Document deleted successfully');
            await this.loadDocuments(false);
        } catch (error) {
            this.showToast('Delete failed', 'error');
        } finally {
            this.toggleLoader(false);
        }
    }

    renderDocuments(documents) {
        this.tableBody.innerHTML = documents.map(doc => this.createTableRow(doc)).join('');
    }

    createTableRow(document) {
        let encodedOcrText = '';
        if (document.ocrJobDone && document.ocrText) {
            try {
                const normalizedText = document.ocrText.normalize('NFC');
                encodedOcrText = btoa(
                    encodeURIComponent(normalizedText)
                        .replace(/%([0-9A-F]{2})/g,
                            (match, p1) => String.fromCharCode('0x' + p1)
                        )
                );
            } catch (error) {
                console.error('Error encoding OCR text:', error);
                encodedOcrText = '';
            }
        }

        return `
        <tr>
            <td>${document.filename}</td>
            <td>${this.formatFileSize(document.filesize)}</td>
            <td>${document.filetype}</td>
            <td>${new Date(document.uploadDate + 'Z').toLocaleString()}</td>
            <td class="text-center">
                ${document.ocrJobDone
            ? '<span class="status-icon status-complete">✓</span>'
            : '<span class="status-icon status-pending" title="OCR in progress">○</span>'
        }
            </td>
            <td>
                <div class="action-buttons">
                    ${document.ocrJobDone && encodedOcrText
            ? `<button class="btn btn-success" data-ocr-text="${encodedOcrText}">
                               View OCR
                           </button>`
            : ''
        }
                    <button class="btn btn-secondary" onclick="documentUI.handleRename('${document.id}', '${document.filename}')">
                        Rename
                    </button>
                    <button class="btn btn-primary" onclick="documentUI.api.downloadDocument('${document.id}', '${document.filename}')">
                        Download
                    </button>
                    <button class="btn btn-danger" onclick="documentUI.handleDelete('${document.id}')">
                        Delete
                    </button>
                </div>
            </td>
        </tr>
    `;
    }

    showOcrText(encodedText) {
        try {
            const decodedText = decodeURIComponent(
                Array.prototype.map.call(
                    atob(encodedText),
                    c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
                ).join('')
            );

            const ocrTextElement = document.getElementById('ocrText');

            if (!decodedText) {
                ocrTextElement.textContent = 'No OCR text available';
                return;
            }

            ocrTextElement.textContent = decodedText.normalize('NFC');
            ocrTextElement.style.whiteSpace = 'pre-wrap';
            ocrTextElement.style.maxHeight = '400px';
            ocrTextElement.style.overflowY = 'auto';

            const modal = new bootstrap.Modal(document.getElementById('ocrModal'));
            modal.show();
        } catch (error) {
            console.error('Error displaying OCR text:', error);
            this.showToast('Error displaying OCR text', 'error');
        }
    }

    formatFileSize(bytes) {
        if (!bytes) return '0 B';
        const units = ['B', 'KB', 'MB', 'GB'];
        let size = bytes;
        let unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return `${size.toFixed(1)} ${units[unitIndex]}`;
    }
}