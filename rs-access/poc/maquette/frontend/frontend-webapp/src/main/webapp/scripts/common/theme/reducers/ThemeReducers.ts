export default (state:any = {}, action:any) => {
  let newState = Object.assign({}, state)
  switch (action.type){
    case "SET_THEME" :
      newState.selected = action.theme
      return newState
    default :
      return state
  }
}
