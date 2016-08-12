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

import ProjectAdminsContainer from "./containers/ProjectAdminsContainer"

export { ProjectAdminsContainer }
