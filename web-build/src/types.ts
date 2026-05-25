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

export interface BaseWebAction {
    type: string
}

export interface WebActionRequestUploadPermission extends BaseWebAction {
    type: 'request_upload_permission';
}

export interface FileUploadInfo {
    checksum: String,
    name: string,
    mimeType: string,
    size: number
}

export interface WebActionInitializeFileUpload extends BaseWebAction {
    type: 'initialize_file_upload';
    draftId: number;
    files: FileUploadInfo[]
}


export type WebAction = 
    | WebActionRequestUploadPermission
    | WebActionInitializeFileUpload



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


export interface WebEventUploadAccepted extends BaseWebEvent {
    type: 'upload_accepted';
    draftId: number;
    id: string;
}

export interface WebEventUploadDenied extends BaseWebEvent {
    type: 'upload_denied';
    draftId: number
}

export interface WebEventUploadUpdated extends BaseWebEvent {
    type: 'upload_updated';
    id: string;
    fileChecksum: string;
    status: 'pending' | 'uploading' | 'finished' | 'error' | 'canceled';
    progress: number;
}

export type WebEvent =
    | WebEventInit
    | WebEventAddFile
    | WebEventRemoveFile
    | WebEventAddText
    | WebEventRemoveText
    | WebEventUploadPermissionChange
    | WebEventUploadAccepted
    | WebEventUploadDenied
    | WebEventUploadUpdated;

export interface SidebarItem {
    id: number;
    content: string;
    mimeType: string;
    label?: string;
    text?: string; // For display in template
    data?: string; // For display in template
}

export enum UploadFileState {
    PENDING,
    UPLOADING,
    FINISHED,
    ERROR,
    CANCELED
}

export interface UploadFile {
    checksum: String,
    file: File,
    name: string,
    mimeType: string,
    size: number,
    state: UploadFileState,
    progress?: number
}


declare global {
    interface Window {
        initialFiles: FileInfo[];
        token: string;
        uploadEnabled: boolean;
        Alpine: any;
    }
}
