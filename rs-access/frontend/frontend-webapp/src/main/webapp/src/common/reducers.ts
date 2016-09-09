/**
 * Combine all reducers module to a single root reducer.
 */
import { combineReducers } from "redux"
import themeReducers from "./theme/reducers/ThemeReducers"
import pluginReducers from "./plugins/PluginReducers"
import i18nReducers from "./i18n/I18nReducers"
import accessRightsReducers from "./access-rights/AccessRightsReducers"
import authentication from "./authentication/AuthenticateReducers"
import { pickBy } from "lodash"
import * as fromEndpoints from "./endpoints/index"

// Keeping both notations as an example
export default combineReducers({
  i18n: i18nReducers,
  theme: themeReducers,
  plugins: pluginReducers,
  api: accessRightsReducers,
  authentication,
  endpoints: fromEndpoints.reducer
})

export const deleteEntityReducer = (state: any, removeAction: any) => (
  Object.assign({}, state, {
    items: pickBy(state.items, (value: string, key: string) => key !== removeAction.id),
    ids: state.ids.filter((id: string) => id !== removeAction.id)
  })
)

// Selectors
export const getEndpointsItems = (state: any) =>
  fromEndpoints.getEndpointsItems(state.endpoints)
