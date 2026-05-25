// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

import { 
    UploadFile, 
    UploadFileState, 
    WebActionInitializeFileUpload, 
    WebEventUploadAccepted, 
    WebEventUploadDenied, 
    WebEventUploadUpdated 
} from '../types';
import { parseJSON } from '../utils';
import { apiActionRequest } from '../services/api';

export enum UploadBatchState {
    DRAFT,
    REQUESTED,
    REJECTED,
    ACCEPTED,
    UPLOADING,
    FINISHED,
    SUCCESS,
    ERROR,
    CANCELED
}

let nextDraftId = 1;

export class UploadBatch {
    private sse: EventSource;
    files: UploadFile[] = [];
    state: UploadBatchState = UploadBatchState.DRAFT;
    draftId: number;
    id?: number;
    private abortController?: AbortController;

    private handlers = {
        accepted: (e: MessageEvent) => this.onUploadAccepted(parseJSON<WebEventUploadAccepted>(e.data)),
        denied: (e: MessageEvent) => this.onUploadDenied(parseJSON<WebEventUploadDenied>(e.data)),
        updated: (e: MessageEvent) => this.onUploadUpdated(parseJSON<WebEventUploadUpdated>(e.data))
    };

    constructor(sse: EventSource) {
        this.sse = sse;
        this.draftId = nextDraftId++;
        this.addSSEEventListeners();
    }

    dispose() {
        this.abortUpload();
        this.removeSSEEventListeners();
    }

    private addSSEEventListeners() {
        this.sse.addEventListener('upload_accepted', this.handlers.accepted);
        this.sse.addEventListener('upload_denied', this.handlers.denied);
        this.sse.addEventListener('upload_updated', this.handlers.updated);
    }

    private removeSSEEventListeners() {
        this.sse.removeEventListener('upload_accepted', this.handlers.accepted);
        this.sse.removeEventListener('upload_denied', this.handlers.denied);
        this.sse.removeEventListener('upload_updated', this.handlers.updated);
    }

    async calculateChecksum(file: File): Promise<string> {
        const arrayBuffer = await file.arrayBuffer();
        const hashBuffer = await crypto.subtle.digest('SHA-256', arrayBuffer);
        const hashArray = Array.from(new Uint8Array(hashBuffer));
        return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    }

    async addFile(file: File) {
        if (this.state !== UploadBatchState.DRAFT) return;

        const checksum = await this.calculateChecksum(file);
        if (this.files.some(f => f.checksum === checksum)) return;

        this.files.push({
            checksum,
            file,
            name: file.name,
            mimeType: file.type || 'application/octet-stream',
            size: file.size,
            state: UploadFileState.PENDING,
            progress: 0
        });
    }

    removeFile(checksum: string) {
        this.files = this.files.filter(f => f.checksum !== checksum);
    }

    async initialize() {
        if (this.state !== UploadBatchState.DRAFT) return;
        this.state = UploadBatchState.REQUESTED;

        try {
            await apiActionRequest({
                type: 'initialize_file_upload',
                draftId: this.draftId,
                files: this.files.map(f => ({
                    checksum: f.checksum,
                    name: f.name,
                    mimeType: f.mimeType,
                    size: f.size
                }))
            } as WebActionInitializeFileUpload);
        } catch (err) {
            this.state = UploadBatchState.ERROR;
            throw err;
        }
    }

    async start() {
        if (this.state !== UploadBatchState.ACCEPTED) return;
        this.state = UploadBatchState.UPLOADING;

        this.abortController = new AbortController();
        const signal = this.abortController.signal;

        const uploadPromises = this.files.map(file => this.uploadSingleFile(file, signal));
        await Promise.allSettled(uploadPromises);
        this.state = UploadBatchState.FINISHED;
    }

    private async uploadSingleFile(file: UploadFile, signal: AbortSignal): Promise<void> {
        file.progress = 0;
        file.state = UploadFileState.UPLOADING;

        try {
            const response = await fetch(`/${window.token}/upload/${this.id}/${file.checksum}`, {
                method: 'POST',
                body: file.file,
                headers: { 'Content-Type': file.mimeType },
                signal: signal
            });

            if (!response.ok) throw new Error(await response.text());
            file.state = UploadFileState.FINISHED;
            file.progress = 100;
        } catch (err: any) {
            if (err.name === 'AbortError') {
                file.state = UploadFileState.CANCELED;
            } else {
                file.state = UploadFileState.ERROR;
                console.error(`Upload failed for ${file.name}:`, err);
            }
        }
    }

    abortUpload() {
        this.abortController?.abort();
        if (this.state === UploadBatchState.UPLOADING) {
            this.state = UploadBatchState.CANCELED;
        }
    }

    private onUploadAccepted(event: WebEventUploadAccepted) {
        if (this.state !== UploadBatchState.REQUESTED || this.draftId !== event.draftId) return;
        this.id = event.id;
        this.state = UploadBatchState.ACCEPTED;
        // Auto-start when accepted? Or wait for user?
        // Let's assume the UI will call start()
    }

    private onUploadDenied(event: WebEventUploadDenied) {
        if (this.draftId !== event.draftId) return;
        this.state = UploadBatchState.REJECTED;
    }

    private onUploadUpdated(event: WebEventUploadUpdated) {
        if (this.id !== event.id) return;

        const file = this.files.find(f => f.checksum === event.fileChecksum);
        if (!file) return;

        file.progress = event.progress;
        switch (event.status) {
            case 'pending': file.state = UploadFileState.PENDING; break;
            case 'uploading': file.state = UploadFileState.UPLOADING; break;
            case 'finished': file.state = UploadFileState.FINISHED; break;
            case 'error': file.state = UploadFileState.ERROR; break;
            case 'canceled': file.state = UploadFileState.CANCELED; break;
        }
    }
}
