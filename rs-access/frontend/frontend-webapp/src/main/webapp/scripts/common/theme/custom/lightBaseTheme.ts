// Default theme
import lightBaseTheme from "material-ui/styles/baseThemes/lightBaseTheme";
import { merge } from "lodash";

export default merge ({}, lightBaseTheme, {
  linkWithoutDecoration: {
    textDecoration: "blink"
  }
});
