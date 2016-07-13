/**
 * Combine all reducers module to a single root reducer.
 */
import { combineReducers } from 'redux';
import themeReducers from './theme/reducers/ThemeReducers'
import pluginReducers from './plugins/PluginReducers'
import accessRightsReducers from './access-rights/AccessRightsReducers'
import authentication, * as fromAuthentication from './authentication/AuthenticateReducers'
var _ = require('lodash')

// Keeping both notations as an example
export default combineReducers({
  theme: themeReducers,
  plugins: pluginReducers,
  views: accessRightsReducers,
  authentication
});

export const deleteEntityReducer = (state: any, action:any) => (
  Object.assign({}, state, {
    items: _.pickBy(state.items, (value:string, key:string) => key !== action.id),
    ids: state.ids.filter( (id:string) => id !== action.id)
  })
)
