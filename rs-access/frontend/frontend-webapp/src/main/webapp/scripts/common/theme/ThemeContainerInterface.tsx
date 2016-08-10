import * as React from "react"

/**
 *  Provides component interfaces to retrieve mui style
 */
export interface ThemeContextInterface {
  muiTheme: any
}

export const ThemeContextType = {
  muiTheme: React.PropTypes.object.isRequired
}

/**
 * A React component which can receive or not
 * a muiTheme prop implement this interface
 */
export interface MuiThemeInjectable {
  muiTheme?: any
}
