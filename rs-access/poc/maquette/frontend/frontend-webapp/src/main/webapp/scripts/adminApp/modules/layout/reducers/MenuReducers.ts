/** @module AdminLayout */
import { showProjects, SHOW_PROJECTS } from '../actions/MenuActions'
import { logout, LOGOUT} from '../../../../common/authentication/AuthenticateActions'


/**
 * Menu actions reducers
 * @prop {Object} state Curent state from store
 * @prop {Object} action Action to reduce [LOGOUT|SHOW_PROJECTS]
 */
export const MenuReducer = (state: any, action: any) => {
  switch (action.type) {
    case LOGOUT:
      console.log("Logging out");
      return state;
    case SHOW_PROJECTS:
      console.log("Showing projects");
      return state;
    default:
      return state;
  }
}

export default MenuReducer
