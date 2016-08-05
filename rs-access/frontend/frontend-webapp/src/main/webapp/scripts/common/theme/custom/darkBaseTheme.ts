// Default theme
import darkBaseTheme from "material-ui/styles/baseThemes/darkBaseTheme"
import { merge } from "lodash"

export default merge ({}, darkBaseTheme, {
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
