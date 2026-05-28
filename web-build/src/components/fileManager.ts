// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

import Alpine from 'alpinejs';
import { FileInfo, WebEventAddFile, WebEventRemoveFile } from '../types';
import { humanFileSize, getThumbnailUrl, getFileIconUrl, copyTextToClipboard } from '../utils';

export const FileManager = (initialFiles: FileInfo[]) => ({
    view: 'grid' as 'grid' | 'list',
    slideshow: false,
    files: initialFiles,
    sortKey: 'filename' as keyof FileInfo,
    sortAsc: true,
    copiedId: null as string | null,
    currentIndex: 0,

    init() {
        const sse = Alpine.store('sse') as EventSource;

        sse.addEventListener('add_file', (event: MessageEvent) => {
            const data = JSON.parse(event.data) as WebEventAddFile;
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
            const data = JSON.parse(event.data) as WebEventRemoveFile;
            this.files = this.files.filter(f => f.fileId !== data.id);
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

        // Reset to icon first
        const iconUrl = getFileIconUrl(file.icon);
        if (el.dataset.isSlideshow) {
            el.src = iconUrl;
            el.className = 'object-contain w-48 h-48 md:w-64 md:h-64';
        } else {
            el.src = iconUrl;
        }

        if (!file.hasThumbnail) return;

        const thumbUrl = getThumbnailUrl(file.fileId);
        const img = new Image();
        img.onload = () => {
            // Check if still same file
            const currentId = el.dataset.isSlideshow ? this.currentFile?.fileId : file.fileId;
            if (file.fileId !== currentId) return;

            el.src = thumbUrl;
            if (el.dataset.isSlideshow) {
                el.className = 'max-w-full max-h-[60vh] object-contain shadow-2xl rounded-lg';
            } else {
                el.className = 'object-cover w-full h-full';
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

    async copyLink(fileId: string) {
        const link = window.location.origin + '/' + window.token + '/file/' + fileId;
        const success = await copyTextToClipboard(link);
        if (success) {
            this.copiedId = fileId;
            setTimeout(() => { if(this.copiedId === fileId) this.copiedId = null; }, 2000);
        }
    }
});
