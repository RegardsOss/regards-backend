import { combineReducers } from "redux"
import adminApp from "./adminApp/reducer"
import userApp from "./userApp/reducers"
import portalApp from "./portalApp/reducers"
import common from "./common/reducers"
import * as fromCommon from "./common/reducers"

export default combineReducers({
  userApp,
  portalApp,
  common,
  adminApp
})


// Selectors
export const getEndpointsItems = (state: any) =>
  fromCommon.getEndpointsItems(state.common)
