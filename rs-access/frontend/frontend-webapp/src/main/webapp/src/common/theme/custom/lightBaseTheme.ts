// Default theme
import lightBaseTheme from "material-ui/styles/baseThemes/lightBaseTheme"
import { merge } from "lodash"
import { red900, grey900, blueGrey50 } from "material-ui/styles/colors"
export default merge({}, lightBaseTheme, {
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
