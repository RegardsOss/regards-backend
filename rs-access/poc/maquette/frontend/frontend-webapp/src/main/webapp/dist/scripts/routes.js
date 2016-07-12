"use strict";
const routes_1 = require('./userApp/routes');
const routes_2 = require('./adminApp/routes');
const routes_3 = require('./portalApp/routes');
const PortalApp_1 = require('./portalApp/PortalApp');
const childRoutes = [{
        path: '/',
        childRoutes: [
            routes_1.default,
            routes_2.default,
            routes_3.default
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