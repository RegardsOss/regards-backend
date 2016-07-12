"use strict";
const redux_thunk_1 = require('redux-thunk');
const redux_1 = require('redux');
const ActionLoggerMiddleware_1 = require('../logger/ActionLoggerMiddleware');
const AuthorizationMiddleware_1 = require('../authentication/AuthorizationMiddleware');
var { apiMiddleware } = require('redux-api-middleware');
// Root reducers
const reducer_1 = require('../../adminApp/reducer');
const reducers_1 = require('../../userApp/reducers');
const reducers_2 = require('../../portalApp/reducers');
const reducers_3 = require('../reducers');
const redux_form_1 = require('redux-form');
function configureStore(preloadedState) {
    // Root reducer
    const rootReducer = redux_1.combineReducers({
        userApp: reducers_1.default,
        portalApp: reducers_2.default,
        common: reducers_3.default,
        adminApp: reducer_1.default,
        form: redux_form_1.reducer
    });
    // Create the application store
    let win = window;
    const store = redux_1.createStore(rootReducer, preloadedState, redux_1.compose(redux_1.applyMiddleware(redux_thunk_1.default, // lets us dispatch() functions
    ActionLoggerMiddleware_1.default, // logs any dispatched action
    AuthorizationMiddleware_1.default, // inject authorization headers in all request actions
    apiMiddleware // middleware for calling an REST API
    ), win["devToolsExtension"] ? win["devToolsExtension"]() : (f) => f // Enable redux dev tools
    ));
    // Log any change in the store
    const render = () => {
        console.log("STORE UPDATED : ", store.getState());
    };
    store.subscribe(render);
    let mod = module;
    // Enable Webpack hot module replacement for reducers
    if (mod["hot"]) {
        mod["hot"].accept('../reducers', () => {
            const nextRootReducer = require('../reducers').default;
            store.replaceReducer(nextRootReducer);
        });
    }
    return store;
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = configureStore;
//# sourceMappingURL=configureStore.js.map