"use strict";
/** Main routes.
 * /      -> PortalApp
 * /user  -> UserApp
 * /admin -> AdminApp
 */
const routes_1 = require('./userApp/routes');
const routes_2 = require('./adminApp/routes');
const routes_3 = require('./portalApp/routes');
const PortalApp_1 = require('./portalApp/PortalApp');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    component: 'div',
    childRoutes: [{
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
        }]
};
//# sourceMappingURL=routes.js.map