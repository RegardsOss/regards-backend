// Redux middleware provides a third-party extension point
// between dispatching an action, and the moment it reaches the reducer
"use strict";
// This exemple log an action before it reached the reducers
function createLoggerMiddleware() {
    return ({ dispatch, getState }) => (next) => (action) => {
        console.log("ACTION : ", action);
        return next(action);
    };
}
const logger = createLoggerMiddleware();
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = logger;
//# sourceMappingURL=ActionLoggerMiddleware.js.map