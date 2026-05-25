// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

import Alpine from 'alpinejs';
import { SidebarItem, WebEventAddText, WebEventRemoveText } from '../types';
import { parseJSON } from '../utils';

export const SidebarStore = {
    items: [] as SidebarItem[],
    isOpen: false,
    hasNew: false,

    init() {
        const sse = Alpine.store('sse') as EventSource;

        sse.addEventListener('add_text', (event: MessageEvent) => {
            const item = parseJSON<WebEventAddText>(event.data);
            const sidebarItem: SidebarItem = {
                ...item,
                text: item.mimeType === 'text/plain' ? item.content : undefined,
                data: item.mimeType !== 'text/plain' ? item.content : undefined
            };
            this.items.push(sidebarItem);
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
};
