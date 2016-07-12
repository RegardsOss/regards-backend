"use strict";
const React = require('react');
const ReactDOM = require('react-dom');
const react_router_1 = require('react-router');
const react_redux_1 = require('react-redux');
const configureStore_1 = require('./common/store/configureStore');
const preloadedState_1 = require('./common/store/preloadedState');
const routes_1 = require('./routes');
const store = configureStore_1.default(preloadedState_1.default);
ReactDOM.render(React.createElement(react_redux_1.Provider, {store: store}, 
    React.createElement(react_router_1.Router, {history: react_router_1.browserHistory, routes: routes_1.routes})
), document.getElementById('app'));
function getSiteMap(parentRoute, routes) {
    routes.map((route) => {
        if (route) {
            let path = '';
            if (parentRoute.slice(-1) === '/' || route.path[0] === '/') {
                path = parentRoute + route.path;
            }
            else {
                path = parentRoute + '/' + route.path;
            }
            console.log(path);
            if (route.childRoutes) {
                getSiteMap(path, route.childRoutes);
            }
        }
    });
}
getSiteMap("", routes_1.routes.childRoutes);
//# sourceMappingURL=main.js.map