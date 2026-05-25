// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

import { WebAction } from '../types';

export async function apiActionRequest(action: WebAction): Promise<void> {
    const response = await fetch(`/${window.token}/api/action`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ action })
    });
    
    if (!response.ok) {
        throw new Error(`API Request failed: ${response.statusText}`);
    }
}

export async function requestUploadPermission(): Promise<void> {
    await apiActionRequest({
        type: 'request_upload_permission'
    });
}
