/**
 * Define the public API accessible from other modules:
 *
 * BAD
 * import actions from '../todos/actions';
 * import TodoItem from '../todos/components/TodoItem';
 *
 * GOOD
 * import todos from '../todos';
 * const { actions, TodoItem } = todos;
 *
 */
"use strict";
const ProjectsContainer_1 = require('./containers/ProjectsContainer');
exports.ProjectsContainer = ProjectsContainer_1.default;
//# sourceMappingURL=index.js.map