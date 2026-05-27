// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

import Alpine from 'alpinejs';
import { UploadPermissionStatus, WebEventUploadPermissionChange } from '../types';
import { requestUploadPermission } from '../services/api';
import { UploadBatch } from '../models/UploadBatch';

export const AppStore = {
    uploadPermissionStatus: 'not_requested' as UploadPermissionStatus,
    uploadBatches: [] as UploadBatch[],
    showUploadModal: false,
    uploadEnabled: window.uploadEnabled,

    init() {
        const sse = Alpine.store('sse') as EventSource;
        sse.addEventListener('upload_permission_change', (e: MessageEvent) => {
            const data = JSON.parse(e.data) as WebEventUploadPermissionChange;
            this.uploadPermissionStatus = data.status;
        });
    },

    async requestUploadPermission() {
        try {
            await requestUploadPermission();
            this.uploadPermissionStatus = 'pending';
        } catch (err) {
            console.error("Permission request failed", err);
        }
    },

    createBatch(): UploadBatch {
        const sse = Alpine.store('sse') as EventSource;
        const batch = new UploadBatch(sse);
        const reactiveBatch = Alpine.reactive(batch);
        this.uploadBatches.push(reactiveBatch);
        return reactiveBatch;
    }
};
