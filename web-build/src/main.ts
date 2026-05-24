// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

import Alpine from 'alpinejs';
import { FileInfo, WebAction, WebEvent, SidebarItem, UploadPermissionStatus } from './types';

function humanFileSize(size: number): string {
    if (size === 0) return '0 B';
    const i = Math.floor(Math.log(size) / Math.log(1024));
    return (size / Math.pow(1024, i)).toFixed(2) + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
}

function parseJSON<T>(json: string): T {
    return JSON.parse(json) as T;
}

document.addEventListener('alpine:init', () => {
    // Central SSE instance
    const sse = new EventSource('/' + window.token + '/events');

    // --- GLOBAL APP STORE ---
    Alpine.store('app', {
        uploadPermissionStatus: 'not_requested' as UploadPermissionStatus,
        showUploadModal: false,
        uploadEnabled: window.uploadEnabled,

        init() {
            sse.addEventListener('upload_permission_change', (e: MessageEvent) => {
                const data = JSON.parse(e.data);
                this.uploadPermissionStatus = data.status;
            });
        },

        requestUploadPermission() {
            fetch(`/${window.token}/api/action`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    action: {
                        type: 'request_upload_permission'
                    }
                })
            })
            .then(() => {
                // The status will be updated via SSE, but we can set it to pending locally too
                this.uploadPermissionStatus = 'pending';
            })
            .catch(err => console.error("Request failed", err));
        }
    });

    // --- SIDEBAR STORE ---
    Alpine.store('sidebar', {
        items: [] as SidebarItem[],
        isOpen: false,
        hasNew: false,

        init() {
            sse.addEventListener('add_text', (event: MessageEvent) => {
                const item = parseJSON<WebEventAddText>(event.data);
                // Pre-process for template
                if (item.mimeType === 'text/plain') {
                    item.text = item.content;
                } else {
                    item.data = item.content;
                }
                this.items.push(item);
                if (!this.isOpen) this.hasNew = true;
            });

            sse.addEventListener('remove_text', (event: MessageEvent) => {
                const msg = parseJSON<WebEventRemoveText>(event.data);
                this.items = this.items.filter(item => item.id !== msg.id);
            });
        },

        toggle() {
            this.isOpen = !this.isOpen;
            if (this.isOpen) this.hasNew = false;
        },

        copy(text: string) {
            navigator.clipboard.writeText(text);
        }
    });

    // --- FILE MANAGER ---
    Alpine.data('fileManager', (initialFiles: FileInfo[]) => ({
        view: 'grid' as 'grid' | 'list',
        slideshow: false,
        files: initialFiles,
        sortKey: 'filename' as keyof FileInfo,
        sortAsc: true,
        copiedId: null as string | null,
        currentIndex: 0,

        init() {
            sse.addEventListener('add_file', (event: MessageEvent) => {
                const data = JSON.parse(event.data);
                const newFile: FileInfo = {
                    fileId: data.id,
                    filename: data.name,
                    size: data.size,
                    mimeType: data.mimeType,
                    icon: data.iconFile,
                    hasThumbnail: data.mimeType.startsWith('image/') || data.mimeType.startsWith('video/')
                };
                if (!this.files.find(f => f.fileId === newFile.fileId)) {
                    this.files.push(newFile);
                }
            });

            sse.addEventListener('remove_file', (event: MessageEvent) => {
                const data = JSON.parse(event.data);
                const id = data.id;
                this.files = this.files.filter(f => f.fileId !== id);
                if (this.currentIndex >= this.files.length) {
                    this.currentIndex = Math.max(0, this.files.length - 1);
                }
            });

            sse.addEventListener('init', () => {
                this.files = [];
            });
        },

        get currentFile(): FileInfo | undefined { return this.sortedFiles[this.currentIndex]; },
        get isGrid(): boolean { return !this.slideshow && this.view === 'grid'; },
        get isList(): boolean { return !this.slideshow && this.view === 'list'; },
        get isSlideshow(): boolean { return this.slideshow; },

        toggleSlideshow() { this.slideshow = !this.slideshow; },
        showList() { this.slideshow = false; this.view = 'list'; },
        showGrid() { this.slideshow = false; this.view = 'grid'; },
        nextSlide() { this.currentIndex = (this.currentIndex + 1) % this.sortedFiles.length; },
        prevSlide() { this.currentIndex = (this.currentIndex - 1 + this.sortedFiles.length) % this.sortedFiles.length; },

        humanFileSize(size: number) { return humanFileSize(size); },

        loadPreview(el: HTMLImageElement, file: FileInfo) {
            if (!file) return;
            if (el.dataset.isSlideshow) {
                el.src = `/${window.token}/icon/${file.icon}`;
                el.classList.remove('max-w-full', 'max-h-[60vh]', 'shadow-2xl', 'rounded-lg');
                el.classList.add('object-contain', 'w-48', 'h-48', 'md:w-64', 'md:h-64');
            }
            const canHaveThumbnail = file.hasThumbnail;
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

        get sortedFiles(): FileInfo[] {
            return [...this.files].sort((a, b) => {
                let valA = a[this.sortKey]; 
                let valB = b[this.sortKey];
                if (typeof valA === 'string' && typeof valB === 'string') { 
                    valA = valA.toLowerCase(); 
                    valB = valB.toLowerCase(); 
                }
                if (valA < valB) return this.sortAsc ? -1 : 1;
                if (valA > valB) return this.sortAsc ? 1 : -1;
                return 0;
            });
        },

        copyLink(fileId: string) {
            const link = window.location.origin + '/' + window.token + '/file/' + fileId;
            navigator.clipboard.writeText(link).then(() => {
                this.copiedId = fileId;
                setTimeout(() => { if(this.copiedId === fileId) this.copiedId = null; }, 2000);
            });
        }
    }));

    // --- DROPZONE ---
    Alpine.data("dropzone", () => ({
        files: [] as File[],
        dragActive: false,
        isUploading: false,
        updateFileInput() {
            const dt = new DataTransfer();
            this.files.forEach(f => dt.items.add(f));
            const input = (this as any).$refs.fileInput as HTMLInputElement;
            input.files = dt.files;
        },
        dropFile(e: DragEvent) {
            this.dragActive = false;
            if (e.dataTransfer?.files) {
                this.files = [...this.files, ...Array.from(e.dataTransfer.files)];
                this.updateFileInput();
            }
        },
        handleFiles(list: FileList) {
            this.files = Array.from(list);
            this.updateFileInput();
        },
        removeFile(i: number) {
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
                    (Alpine.store('app') as any).showUploadModal = false;
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

// Start Alpine
(window as any).Alpine = Alpine;
Alpine.start();
