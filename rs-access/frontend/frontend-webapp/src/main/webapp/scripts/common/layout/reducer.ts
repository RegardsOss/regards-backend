import { SET_LAYOUT } from './actions'

export default (state:Object = {}, action:any) => {
  switch (action.type) {
    case SET_LAYOUT:
      return action.layout
    default:
      return state
  }
}
