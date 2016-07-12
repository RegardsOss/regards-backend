"use strict";
exports.homeRoutes = {
    path: 'home',
    getComponents(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, {
                content: require('./Home')
            });
        });
    }
};
//# sourceMappingURL=routes.js.map