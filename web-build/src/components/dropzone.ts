// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

import Alpine from 'alpinejs';
import { UploadBatch, UploadBatchState } from '../models/UploadBatch';

export const Dropzone = () => ({
    activeBatch: null as UploadBatch | null,
    dragActive: false,
    trigger: 0,

    get isUploading(): boolean {
        return this.activeBatch?.state === UploadBatchState.UPLOADING;
    },

    get files() {
        this.trigger
        return this.activeBatch?.files || [];
    },

    init() {
        // Start with a fresh draft batch
        this.createNewBatch();
    },

    createNewBatch() {
        const appStore = Alpine.store('app') as any;
        this.activeBatch = appStore.createBatch();
    },

    async handleFiles(fileList: FileList | File[]) {
        if (!this.activeBatch) this.createNewBatch();
        
        for (const file of Array.from(fileList)) {
            await this.activeBatch!.addFile(file);
        }
    },

    dropFile(e: DragEvent) {
        this.dragActive = false;
        if (e.dataTransfer?.files) {
            this.handleFiles(e.dataTransfer.files);
        }
    },

    removeFile(index: number) {
        if (!this.activeBatch) return;
        const file = this.activeBatch.files[index];
        if (file) {
            this.activeBatch.removeFile(file.checksum as string);
        }
    },

    async submit() {
        if (!this.activeBatch || this.activeBatch.files.length === 0) return;

        try {
            // 1. Initialize (Ask for permission/ID)
            await this.activeBatch.initialize();
            
            // Note: The UI might need to wait for 'upload_accepted' SSE event 
            // which changes state to ACCEPTED. 
            // We can poll or watch for state change here.
            
            const checkState = () => {
                if (this.activeBatch?.state === UploadBatchState.ACCEPTED) {
                    this.activeBatch.start();
                    // Close the modal when accepted
                    (Alpine.store('app') as any).showUploadModal = false;
                    // Reset for next time
                    this.createNewBatch();
                } else if (this.activeBatch?.state === UploadBatchState.REJECTED) {
                    // No alert anymore, UI handles showing rejected state
                    this.activeBatch.state = UploadBatchState.DRAFT; 
                } else if (this.activeBatch?.state === UploadBatchState.REQUESTED) {
                    setTimeout(checkState, 500);
                }
            };
            
            checkState();

        } catch (err) {
            console.error('Submit failed:', err);
            alert('No se pudo iniciar la carga.');
        }
    },

    dragOver() { this.dragActive = true; },
    dragLeave() { this.dragActive = false; }
});
