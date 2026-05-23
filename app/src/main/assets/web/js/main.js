// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

function humanFileSize(size) {
    if (size === 0) return '0 B';
    const i = Math.floor(Math.log(size) / Math.log(1024));
    return (size / Math.pow(1024, i)).toFixed(2) * 1 + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
}

document.addEventListener('alpine:init', () => {
    // Central SSE instance (used by everyone)
    const sse = new EventSource('/' + window.token + '/events');

    // --- GLOBAL APP STORE (Status & Permissions) ---
    Alpine.store('app', {
        uploadStatus: 'none', // none, pending, granted, denied
        showUploadModal: false,
        uploadEnabled: window.uploadEnabled,

        init() {
            sse.addEventListener('uploadPermission', (e) => {
                if (e.data === 'granted' || e.data === 'denied') {
                    uploadStatus = e.date
                }
            });
        },

        requestUploadPermission() {
            fetch(`/${window.token}/upload/request`, { method: 'POST' })
                .then(() => {
                    this.uploadStatus = 'pending';
                })
                .catch(err => console.error("Request failed", err));
        }
    });

    // --- SIDEBAR STORE (Shared Content & Notifications) ---
    Alpine.store('sidebar', {
        items: [],
        isOpen: false,
        hasNew: false,

        init() {
            sse.addEventListener('addSharedContent', (event) => {
                this.items.push(JSON.parse(event.data));
                if (!this.isOpen) this.hasNew = true;
            });

            sse.addEventListener('removeSharedContent', (event) => {
                const id = event.data;
                this.items = this.items.filter(item => item.id != id);
            });
        },

        toggle() {
            this.isOpen = !this.isOpen;
            if (this.isOpen) this.hasNew = false;
        },

        copy(text) {
            navigator.clipboard.writeText(text);
        }
    });

    // --- FILE MANAGER (File list & View only) ---
    Alpine.data('fileManager', (initialFiles) => ({
        view: 'grid',
        slideshow: false,
        files: initialFiles,
        sortKey: 'filename',
        sortAsc: true,
        copiedId: null,
        currentIndex: 0,

        init() {
            sse.addEventListener('add', (event) => {
                const newFile = JSON.parse(event.data);
                if (!this.files.find(f => f.fileId === newFile.fileId)) {
                    this.files.push(newFile);
                }
            });

            sse.addEventListener('remove', (event) => {
                const id = event.data;
                this.files = this.files.filter(f => f.fileId !== id);
                if (this.currentIndex >= this.files.length) {
                    this.currentIndex = Math.max(0, this.files.length - 1);
                }
            });

            sse.addEventListener('init', () => {
                this.files = [];
            });
        },

        get currentFile() { return this.sortedFiles[this.currentIndex]; },
        get isGrid() { return !this.slideshow && this.view === 'grid'; },
        get isList() { return !this.slideshow && this.view === 'list'; },
        get isSlideshow() { return this.slideshow; },

        toggleSlideshow() { this.slideshow = !this.slideshow; },
        showList() { this.slideshow = false; this.view = 'list'; },
        showGrid() { this.slideshow = false; this.view = 'grid'; },
        nextSlide() { this.currentIndex = (this.currentIndex + 1) % this.sortedFiles.length; },
        prevSlide() { this.currentIndex = (this.currentIndex - 1 + this.sortedFiles.length) % this.sortedFiles.length; },

        loadPreview(el, file) {
            if (!file) return;
            if (el.dataset.isSlideshow) {
                el.src = `/${window.token}/icon/${file.icon}`;
                el.classList.remove('max-w-full', 'max-h-[60vh]', 'shadow-2xl', 'rounded-lg');
                el.classList.add('object-contain', 'w-48', 'h-48', 'md:w-64', 'md:h-64');
            }
            const canHaveThumbnail = file.hasThumbnail || (file.mimeType?.startsWith('image/') || file.mimeType?.startsWith('video/'));
            if (!canHaveThumbnail) return;

            const thumbUrl = `/${window.token}/thumbnail/${file.fileId}`;
            const img = new Image();
            img.onload = () => {
                if (file.fileId !== (el.dataset.isSlideshow ? this.currentFile?.fileId : file.fileId)) return;
                el.src = thumbUrl;
                el.classList.remove('p-2', 'w-20', 'h-20', 'w-8', 'h-8', 'w-48', 'h-48', 'md:w-64', 'md:h-64');
                if (el.dataset.isSlideshow) {
                     el.classList.add('max-w-full', 'max-h-[60vh]', 'object-contain', 'shadow-2xl', 'rounded-lg');
                } else {
                     el.classList.add('object-cover', 'w-full', 'h-full');
                }
            };
            img.src = thumbUrl;
        },

        get sortedFiles() {
            return [...this.files].sort((a, b) => {
                let valA = a[this.sortKey]; let valB = b[this.sortKey];
                if (typeof valA === 'string') { valA = valA.toLowerCase(); valB = valB.toLowerCase(); }
                if (valA < valB) return this.sortAsc ? -1 : 1;
                if (valA > valB) return this.sortAsc ? 1 : -1;
                return 0;
            });
        },

        copyLink(fileId) {
            const link = window.location.origin + '/' + window.token + '/file/' + fileId;
            navigator.clipboard.writeText(link).then(() => {
                this.copiedId = fileId;
                setTimeout(() => { if(this.copiedId === fileId) this.copiedId = null; }, 2000);
            });
        }
    }));

    // --- DROPZONE (Upload Logic) ---
    Alpine.data("dropzone", () => ({
        files: [],
        dragActive: false,
        isUploading: false,
        updateFileInput() {
            const dt = new DataTransfer();
            this.files.forEach(f => dt.items.add(f));
            this.$refs.fileInput.files = dt.files;
        },
        dropFile(e) {
            this.dragActive = false;
            this.files = [...this.files, ...e.dataTransfer.files];
            this.updateFileInput();
        },
        handleFiles(list) {
            this.files = Array.from(list);
            this.updateFileInput();
        },
        removeFile(i) {
            this.files.splice(i, 1);
            this.updateFileInput();
        },
        async submit() {
            if (this.files.length === 0) return;
            this.isUploading = true;

            const formData = new FormData();
            this.files.forEach(file => formData.append('files', file));

            try {
                const response = await fetch(`/${window.token}/upload`, {
                    method: 'POST',
                    body: formData
                });

                if (response.ok) {
                    this.files = [];
                    this.updateFileInput();
                    Alpine.store('app').showUploadModal = false;
                } else {
                    const text = await response.text();
                    alert('Upload failed: ' + text);
                }
            } catch (err) {
                console.error('Upload error:', err);
                alert('Upload failed. See console for details.');
            } finally {
                this.isUploading = false;
            }
        },
        dragOver() { this.dragActive = true; },
        dragLeave() { this.dragActive = false; }
    }));
});
