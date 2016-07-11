"use strict";
const routes = {
    path: 'home',
    getComponents(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, {
                content: require('./Home')
            });
        });
    }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = routes;
module.exports = routes;
//# sourceMappingURL=routes.js.map