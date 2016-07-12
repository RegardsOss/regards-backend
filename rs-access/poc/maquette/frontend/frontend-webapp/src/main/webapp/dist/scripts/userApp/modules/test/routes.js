"use strict";
const Test_1 = require('./Test');
exports.testRoutes = {
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