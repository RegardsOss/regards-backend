"use strict";
exports.testRoutes = {
    path: 'test',
    getComponents(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, {
                content: require('./Test')
            });
        });
    }
};
//# sourceMappingURL=routes.js.map