/**
 * Combine all reducers module to a single root reducer.
 */
import { combineReducers } from 'redux';
import themeReducers from './theme/reducers/ThemeReducers'
import pluginReducers from './plugins/PluginReducers'
import i18nReducers from './i18n/I18nReducers'
import accessRightsReducers from './access-rights/AccessRightsReducers'
import authentication, * as fromAuthentication from './authentication/AuthenticateReducers'
import layout from './layout/reducer'
import { pickBy } from 'lodash'

// Keeping both notations as an example
export default combineReducers({
  i18n: i18nReducers,
  theme: themeReducers,
  plugins: pluginReducers,
  views: accessRightsReducers,
  authentication,
  layout
});

export const deleteEntityReducer = (state: any, action:any) => (
  Object.assign({}, state, {
    items: pickBy(state.items, (value:string, key:string) => key !== action.id),
    ids: state.ids.filter( (id:string) => id !== action.id)
  })
)
