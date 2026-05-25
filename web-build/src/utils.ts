// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

export function humanFileSize(size: number): string {
    if (size === 0) return '0 B';
    const i = Math.floor(Math.log(size) / Math.log(1024));
    return (size / Math.pow(1024, i)).toFixed(2) + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
}

export function parseJSON<T>(json: string): T {
    return JSON.parse(json) as T;
}

export function getFileIconUrl(icon: string): string {
    return `/${window.token}/icon/${icon}`;
}

export function getThumbnailUrl(fileId: string): string {
    return `/${window.token}/thumbnail/${fileId}`;
}
