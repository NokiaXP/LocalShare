// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

export interface FileInfo {
    fileId: string;
    filename: string;
    icon: string;
    size: number;
    mimeType: string;
    hasThumbnail: boolean;
}

export type UploadPermissionStatus = 'not_requested' | 'pending' | 'granted' | 'denied' | 'unknown';

export type WebAction = 
    | { type: 'request_upload_permission' };

export interface ActionEnvelop {
    id?: number;
    action: WebAction;
}

export interface BaseWebEvent {
    correlationId?: number;
    type: string;
}

export interface WebEventInit extends BaseWebEvent {
    type: 'init';
    uploadsEnabled: boolean;
}

export interface WebEventAddFile extends BaseWebEvent {
    type: 'add_file';
    id: string;
    name: string;
    size: number;
    mimeType: string;
    iconFile: string;
}

export interface WebEventRemoveFile extends BaseWebEvent {
    type: 'remove_file';
    id: string;
}

export interface WebEventAddText extends BaseWebEvent {
    type: 'add_text';
    id: number;
    content: string;
    mimeType: string;
    label?: string;
}

export interface WebEventRemoveText extends BaseWebEvent {
    type: 'remove_text';
    id: number;
}

export interface WebEventUploadPermissionChange extends BaseWebEvent {
    type: 'upload_permission_change';
    status: UploadPermissionStatus;
}

export type WebEvent =
    | WebEventInit
    | WebEventAddFile
    | WebEventRemoveFile
    | WebEventAddText
    | WebEventRemoveText
    | WebEventUploadPermissionChange;

export interface SidebarItem {
    id: number;
    content: string;
    mimeType: string;
    label?: string;
    text?: string; // For display in template
    data?: string; // For display in template
}

declare global {
    interface Window {
        initialFiles: FileInfo[];
        token: string;
        uploadEnabled: boolean;
        Alpine: any;
    }
}
