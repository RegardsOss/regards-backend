import * as injectTapEventPlugin from 'react-tap-event-plugin'
import lightBaseTheme from 'material-ui/styles/baseThemes/lightBaseTheme'
import darkBaseTheme from 'material-ui/styles/baseThemes/darkBaseTheme'
import getMuiTheme from 'material-ui/styles/getMuiTheme'
import { MuiTheme } from 'material-ui/styles'
// Needed for onTouchTap
// http://stackoverflow.com/a/34015469/988941
injectTapEventPlugin()

class ThemeHelper {

  static getThemes(): { [name: string] : MuiTheme; } {
    return {
      lightBaseTheme,
      darkBaseTheme
    }
  }

  static getByName(name: string): MuiTheme {
    return getMuiTheme(this.getThemes()[name])
  }

}

export default ThemeHelper
