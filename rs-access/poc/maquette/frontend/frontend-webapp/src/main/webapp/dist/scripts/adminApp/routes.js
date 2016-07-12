"use strict";
const routes_1 = require('./modules/home/routes');
const routes_2 = require('./modules/test/routes');
const routes_3 = require('./modules/projects/routes');
const AdminApp_1 = require('./AdminApp');
exports.adminAppRoutes = {
    path: "admin/:project",
    childRoutes: [
        routes_1.homeRoutes,
        routes_2.testRoutes,
        routes_3.projectsRoutes
    ],
    getComponent(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, AdminApp_1.default);
        });
    }
};
//# sourceMappingURL=routes.js.map