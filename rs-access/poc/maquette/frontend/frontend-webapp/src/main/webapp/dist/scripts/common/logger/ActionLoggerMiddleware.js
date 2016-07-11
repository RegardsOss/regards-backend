"use strict";
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