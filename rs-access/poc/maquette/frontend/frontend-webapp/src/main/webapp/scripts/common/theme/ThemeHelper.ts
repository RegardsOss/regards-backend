import { merge } from 'lodash'
import * as injectTapEventPlugin from 'react-tap-event-plugin'
import getMuiTheme from 'material-ui/styles/getMuiTheme'
import { MuiTheme } from 'material-ui/styles'
// Default themes
import lightBaseTheme from 'material-ui/styles/baseThemes/lightBaseTheme'
import darkBaseTheme from 'material-ui/styles/baseThemes/darkBaseTheme'
// Custom themes
import customThemes from './custom/index'

// Needed for onTouchTap
// http://stackoverflow.com/a/34015469/988941
injectTapEventPlugin()

class ThemeHelper {

  static getThemes(): { [name: string] : any; } {
    return merge({}, { lightBaseTheme, darkBaseTheme }, customThemes)
  }

  static getByName(name: string): any {
    return getMuiTheme(this.getThemes()[name])
  }

}

export default ThemeHelper
