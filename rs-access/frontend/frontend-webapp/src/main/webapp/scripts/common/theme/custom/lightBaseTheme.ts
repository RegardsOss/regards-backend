// Default theme
import lightBaseTheme from "material-ui/styles/baseThemes/lightBaseTheme"
import { merge } from "lodash"

import { red900 } from "material-ui/styles/colors"
export default merge ({}, lightBaseTheme, {
  palette: {
    errorColor: {
      color: red900
    }
  },
  linkWithoutDecoration: {
    textDecoration: "blink"
  },
  adminApp: {
    loginForm: {
      backgroundColor: '#ECEFF1',
      display: "flex",
      alignItems: "center"
    }
  }

})
