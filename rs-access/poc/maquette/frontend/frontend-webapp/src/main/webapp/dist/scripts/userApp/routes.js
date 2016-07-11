"use strict";
const routes_1 = require('./modules/plugin/routes');
const routes_2 = require('./modules/test/routes');
const routes_3 = require('./modules/websockets/routes');
const UserApp_1 = require('./UserApp');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    path: "user/:project",
    childRoutes: [
        routes_1.default,
        routes_2.default,
        routes_3.default
    ],
    getComponent(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, UserApp_1.default);
        });
    }
};
//# sourceMappingURL=routes.js.map