import * as React from "react";

/**
 *  Provides component interfaces to retrieve mui style
 */
export interface ThemeContextInterface {
  muiTheme: any;
}

export const ThemeContextType = {
  muiTheme: React.PropTypes.object.isRequired
}
