"use strict";
const preloadedState = {
    common: {
        theme: '',
        plugins: {},
        views: [],
        authentication: {}
    },
    userApp: {
        ws: {}
    },
    portalApp: {
        projects: {}
    },
    adminApp: {
        projects: {
            items: [
                {
                    id: '0',
                    name: 'Project X',
                    selected: false,
                    admins: [0, 1]
                },
                {
                    id: '1',
                    name: 'Blair witch project',
                    selected: false,
                    admins: [2, 3]
                }
            ]
        },
    }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = preloadedState;
//# sourceMappingURL=preloadedState.js.map