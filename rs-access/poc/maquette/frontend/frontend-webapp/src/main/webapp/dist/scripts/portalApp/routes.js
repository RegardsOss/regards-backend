"use strict";
const PortalApp_1 = require('./PortalApp');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    path: "portal",
    getComponent(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, PortalApp_1.default);
        });
    }
};
//# sourceMappingURL=routes.js.map