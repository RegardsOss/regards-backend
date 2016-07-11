"use strict";
const routes_1 = require('./modules/home/routes');
const routes_2 = require('./modules/test/routes');
const routes_3 = require('./modules/projects/routes');
const AdminApp_1 = require('./AdminApp');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    path: "admin/:project",
    childRoutes: [
        routes_1.default, routes_2.default, routes_3.default
    ],
    getComponent(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, AdminApp_1.default);
        });
    }
};
//# sourceMappingURL=routes.js.map