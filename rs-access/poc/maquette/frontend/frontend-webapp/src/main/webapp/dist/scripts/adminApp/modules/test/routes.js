"use strict";
const routes = {
    path: 'test',
    getComponents(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, {
                content: require('./Test')
            });
        });
    }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = routes;
module.exports = routes;
//# sourceMappingURL=routes.js.map