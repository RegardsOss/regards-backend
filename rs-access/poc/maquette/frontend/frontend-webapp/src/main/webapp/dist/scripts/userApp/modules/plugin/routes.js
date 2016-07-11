"use strict";
const Plugin_1 = require('./Plugin');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    path: "plugins/:plugin",
    getComponent(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, {
                content: Plugin_1.default
            });
        });
    }
};
//# sourceMappingURL=routes.js.map