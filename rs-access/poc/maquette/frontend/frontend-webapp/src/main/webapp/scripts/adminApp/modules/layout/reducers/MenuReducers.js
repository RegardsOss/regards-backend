import { showProjects, SHOW_PROJECTS } from '../actions/MenuActions'
import { logout, LOGOUT} from 'common/authentication/AuthenticateActions'

const buttonReducers = (sate, action) => {
  switch (action.type) {
    case LOGOUT:
      console.console.log("Logging out");
      return state;
      break;
    case SHOW_PROJECTS:
      console.console.log("Showing projects");
      return state;
      break;
    default:
      return state;
  }
}

const MenuReducers = {
  buttonReducers
}

export default MenuReducers
