// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

import Alpine from 'alpinejs';
import { AppStore } from './stores/app';
import { SidebarStore } from './stores/sidebar';
import { FileManager } from './components/fileManager';
import { Dropzone } from './components/dropzone';

// Initialize SSE as a global property so it's available before stores init
const sse = new EventSource(`/${window.token}/events`);

document.addEventListener('alpine:init', () => {
    // 1. Register Global Stores
    Alpine.store('sse', sse);
    Alpine.store('app', AppStore);
    Alpine.store('sidebar', SidebarStore);

    // 2. Register Components
    Alpine.data('fileManager', FileManager);
    Alpine.data('dropzone', Dropzone);

    // 3. Global Binds (Extracted from template.html)
    Alpine.bind('ThemeToggle', () => ({
        '@click'() {
            this.darkMode = !this.darkMode;
            localStorage.setItem('dark', String(this.darkMode));
        },
        'class': 'ls-btn-icon'
    }));

    Alpine.bind('UploadButton', () => ({
        '@click'() {
            // Expliziter Zugriff über das globale Alpine-Objekt
            (window as any).Alpine.store('app').showUploadModal = true;
        },
        'x-show'() {
            return (window as any).Alpine.store('app').uploadEnabled;
        },
        'class': 'ls-btn-icon'
    }));

    Alpine.bind('SidebarToggle', () => ({
        '@click'() {
            (Alpine.store('sidebar') as any).toggle();
        },
        'class': 'ls-btn-icon relative'
    }));
});

// Start Alpine
(window as any).Alpine = Alpine;
Alpine.start();
