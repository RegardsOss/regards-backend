"use strict";
const Test_1 = require('./Test');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    path: "test",
    getComponent(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, {
                content: Test_1.default
            });
        });
    }
};
//# sourceMappingURL=routes.js.map