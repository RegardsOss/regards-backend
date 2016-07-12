"use strict";
const routes_1 = require('./userApp/routes');
const routes_2 = require('./adminApp/routes');
const routes_3 = require('./portalApp/routes');
const PortalApp_1 = require('./portalApp/PortalApp');
const childRoutes = [{
        path: '/',
        childRoutes: [
            routes_2.adminAppRoutes,
            routes_1.userAppRoutes,
            routes_3.portalAppRoutes
        ],
        getComponent(nextState, cb) {
            require.ensure([], (require) => {
                cb(null, PortalApp_1.default);
            });
        }
    }];
exports.routes = {
    childRoutes: childRoutes
};
//# sourceMappingURL=routes.js.map