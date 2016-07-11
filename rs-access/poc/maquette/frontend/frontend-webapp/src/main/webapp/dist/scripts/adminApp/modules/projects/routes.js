"use strict";
const ProjectsContainer_1 = require('./containers/ProjectsContainer');
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    path: 'projects',
    getComponents(nextState, cb) {
        require.ensure([], (require) => {
            cb(null, {
                content: ProjectsContainer_1.default
            });
        });
    }
};
//# sourceMappingURL=routes.js.map