/**
 * Combine all reducers module to a single root reducer.
 */
import { combineReducers } from 'redux';
import themeReducers from 'common/theme/reducers/ThemeReducers'
import pluginReducers from 'common/plugins/PluginReducers'
import accessRightsReducers from 'common/access-rights/AccessRightsReducers'
import authentication from 'common/authentication/AuthenticateReducers'

// Keeping both notations as an example
export default combineReducers({
  theme: themeReducers,
  plugins: pluginReducers,
  views: accessRightsReducers,
  authentication
});
