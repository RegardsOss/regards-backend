// Default theme
import darkBaseTheme from "material-ui/styles/baseThemes/darkBaseTheme"
import { merge } from "lodash"
import { red900 } from "material-ui/styles/colors"
export default merge ({}, darkBaseTheme, {
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
      backgroundColor: "transparent",
      backgroundImage: "url('/img/background.jpg')"
    }
  }
})
