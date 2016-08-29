// Default theme
import lightBaseTheme from "material-ui/styles/baseThemes/lightBaseTheme"
import { merge } from "lodash"

import { red900, grey900, blueGrey50 } from "material-ui/styles/colors"
export default merge ({}, lightBaseTheme, {
  palette: {
    errorColor: {
      color: red900
    }
  },
  linkWithoutDecoration: {
    textDecoration: "blink",
    color: grey900
  },
  adminApp: {
    loginForm: {
      backgroundColor: blueGrey50,
      display: "flex",
      alignItems: "center"
    },
    layout: {
      backgroundColor: "transparent",
      backgroundImage: "url('/img/background.jpg')",
    }
  }

})
