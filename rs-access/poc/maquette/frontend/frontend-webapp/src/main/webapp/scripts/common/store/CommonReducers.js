
const theme = (state = '', action) => {
  switch (action.type){
    case "SET_THEME" :
      return action.theme;
    default :
      return state;
  }
}

const authenticated = (state = false, action) => {
  switch (action.type){
    case "AUTHENTICATED" :
      return true;
    case "LOGGED_OUT" :
      return false;
    default :
      return state;
  }
}


const commonReducers = {
  theme,
  authenticated
}

export default commonReducers
