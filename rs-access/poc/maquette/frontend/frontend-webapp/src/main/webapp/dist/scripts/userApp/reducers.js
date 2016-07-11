"use strict";
/**
 * Combine all reducers for this aa to a single root reducer.
 */
const redux_1 = require('redux');
const TimeReducers_1 = require('./modules/websockets/reducers/TimeReducers');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = redux_1.combineReducers({
    ws: TimeReducers_1.default
});
//# sourceMappingURL=reducers.js.map