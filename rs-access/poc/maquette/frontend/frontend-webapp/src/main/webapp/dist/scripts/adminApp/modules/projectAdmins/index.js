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
const ProjectAdminsContainer_1 = require('./containers/ProjectAdminsContainer');
exports.ProjectAdminsContainer = ProjectAdminsContainer_1.default;
const UserFormContainer_1 = require('./containers/UserFormContainer');
exports.UserFormContainer = UserFormContainer_1.default;
//# sourceMappingURL=index.js.map