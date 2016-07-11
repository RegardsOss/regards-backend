"use strict";
const TimeActions_1 = require('../actions/TimeActions');
const WSTimeActions_1 = require('../actions/WSTimeActions');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = (state = {
        started: false,
        time: ''
    }, action) => {
    switch (action.type) {
        case TimeActions_1.RECEIVE_START_TIME:
            return Object.assign({}, state, {
                started: true
            });
        case WSTimeActions_1.SET_TIME:
            return Object.assign({}, state, {
                time: action.time
            });
        default:
            return state;
    }
};
// const timeReducers = {
//   ws
// }
//
// export default timeReducers
//# sourceMappingURL=TimeReducers.js.map