/** @module AdminLayout */
// Action to inform store to display the projects
export const SHOW_PROJECTS = 'SHOW_PROJECTS';

/**
 * showProjects - Create an SHOW_PROJECTS action
 *
 * @return {Action}  SHOW_PROJECTS action
 */
export function showProjects() {
  return {
    type: SHOW_PROJECTS
  }
}
