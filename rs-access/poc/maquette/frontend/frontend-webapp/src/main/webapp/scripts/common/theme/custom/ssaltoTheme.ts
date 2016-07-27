import { merge } from 'lodash'
// Import the colors of your theme
// http://www.material-ui.com/#/customization/colors
import {
  lightGreen500, lightGreen700, grey300, limeA200, grey700, blueGrey800,
  brown50, amber50, deepOrange900, blueGrey900, cyan500, darkBlack, brown900
} from 'material-ui/styles/colors'
import {fade} from 'material-ui/utils/colorManipulator'
import spacing from 'material-ui/styles/spacing'

/**
 *  Custom theme for cdpp project.
 *  Only provide the keys you wish to customize, they will be merged to default
 *  theme variables when calling 'material-ui/styles/getMuiTheme'
 */
export default {
  spacing: spacing,
  fontFamily: 'Roboto, sans-serif',
  palette: {
    primary1Color: deepOrange900,
    primary2Color: blueGrey800,
    primary3Color: grey300,
    accent1Color: lightGreen500,
    accent2Color: grey300,
    accent3Color: grey700,
    textColor: lightGreen700,
    secondaryTextColor: fade(blueGrey800, 0.54),
    alternateTextColor: brown50,
  },
};
