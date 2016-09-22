import * as injectTapEventPlugin from "react-tap-event-plugin"
import getMuiTheme from "material-ui/styles/getMuiTheme"
import customThemes from "./custom/index"
import adminAppLayer from "./custom/adminAppLayer"
import { merge } from "lodash"
// Custom themes

// Needed for onTouchTap
// http://stackoverflow.com/a/34015469/988941
injectTapEventPlugin()

class ThemeHelper {

  static getThemes (): { [name: string]: any; } {
    return customThemes
  }

  static addAppLayer = (theme: any): any => {
    return merge({}, adminAppLayer, theme)
  }

  /**
   * Todo - getMuiTheme always merge the custom theme with a standard one,
   */
  static getByName (name: string): any {
    const theme = getMuiTheme(this.addAppLayer(this.getThemes()[name]))
    return theme
  }

}

export default ThemeHelper
