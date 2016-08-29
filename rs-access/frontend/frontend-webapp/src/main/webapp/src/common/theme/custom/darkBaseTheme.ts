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
      backgroundImage: "url('/img/background.jpg')",
      display: "flex",
      alignItems: "center"
    },
    layout: {
      backgroundColor: "transparent",
      background: "url('/img/background.jpg') top right no-repeat",
      backgroundAttachment: "fixed"
    },
    datamanagement: {
      home: {
        items: {
          classes: ["col-xs-50", "col-sm-33", "col-lg-16"],
          styles: {
            padding: "10px 0",
            textAlign: "center"
          }
        },
        container: {
          classes: ["row"],
          styles: {
            display: "flex",
            justifyContent: "space-between",
            marginTop: "10px"
          }
        }
      }
    }
  }
})
