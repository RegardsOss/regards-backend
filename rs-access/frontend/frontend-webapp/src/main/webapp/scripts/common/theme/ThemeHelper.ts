import * as injectTapEventPlugin from 'react-tap-event-plugin'
import getMuiTheme from 'material-ui/styles/getMuiTheme'
// Custom themes
import customThemes from './custom/index'

// Needed for onTouchTap
// http://stackoverflow.com/a/34015469/988941
injectTapEventPlugin()

class ThemeHelper {

  static getThemes(): { [name: string]: any; } {
    return customThemes
  }

  /**
  * Todo - getMuiTheme always merge the custom theme with a standart one,
  */
  static getByName(name: string): any {
    return getMuiTheme(this.getThemes()[name])
  }

}

export default ThemeHelper
