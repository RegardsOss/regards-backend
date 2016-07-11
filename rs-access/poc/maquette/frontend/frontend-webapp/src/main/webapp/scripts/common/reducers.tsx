/**
 * Combine all reducers module to a single root reducer.
 */
import { combineReducers } from 'redux';
import themeReducers from 'common/theme/reducers/ThemeReducers'
import pluginReducers from 'common/plugins/PluginReducers'
import accessRightsReducers from 'common/access-rights/AccessRightsReducers'
import authentication, * as fromAuthentication from 'common/authentication/AuthenticateReducers'
import { pickBy } from 'lodash'

// Keeping both notations as an example
export default combineReducers({
  theme: themeReducers,
  plugins: pluginReducers,
  views: accessRightsReducers,
  authentication
});

export const deleteEntityReducer = (state, action) =>
(Object.assign({}, state,{
  items: pickBy(state.items, (value, key) => key !== action.id),
  ids: state.ids.filter(id => id !== action.id)
}))

// Selectors
export const getAuthorization = (state) =>
  fromAuthentication.getAuthorization(state.common)
