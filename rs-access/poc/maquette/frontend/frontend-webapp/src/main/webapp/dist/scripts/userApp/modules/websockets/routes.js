"use strict";
const WebSockets_1 = require('./WebSockets');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    path: "time",
    getComponent(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, {
                content: WebSockets_1.default
            });
        });
    }
};
//# sourceMappingURL=routes.js.map