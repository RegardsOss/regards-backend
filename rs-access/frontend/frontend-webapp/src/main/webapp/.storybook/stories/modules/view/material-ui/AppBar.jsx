import AppBar from "material-ui/AppBar";
import MuiThemeProvider from "material-ui/styles/MuiThemeProvider";
import getMuiTheme from "material-ui/styles/getMuiTheme";

const AppBars = ({theme}) => (
  <MuiThemeProvider muiTheme={getMuiTheme(theme)}>
    <div>
      <AppBar title="primary1Color" style={{backgroundColor: theme.palette.primary1Color}}/>
      <AppBar title="primary2Color" style={{backgroundColor: theme.palette.primary2Color}}/>
      <AppBar title="primary3Color" style={{backgroundColor: theme.palette.primary3Color}}/>
      <AppBar title="accent1Color" style={{backgroundColor: theme.palette.accent1Color}}/>
      <AppBar title="accent2Color" style={{backgroundColor: theme.palette.accent2Color}}/>
      <AppBar title="accent3Color" style={{backgroundColor: theme.palette.accent3Color}}/>
    </div>
  </MuiThemeProvider>
)

AppBars.propTypes = {
  theme: React.PropTypes.object.isRequired
};

export default AppBars
