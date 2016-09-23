import * as React from "react";
import RaisedButton from "material-ui/RaisedButton";
import MuiThemeProvider from "material-ui/styles/MuiThemeProvider";
import getMuiTheme from "material-ui/styles/getMuiTheme";

const style = {
  margin: 10
}

export default ({theme}) => (
  <MuiThemeProvider muiTheme={getMuiTheme(theme)}>
    <div>
      <RaisedButton style={style} label="Primary" primary={true}/>
      <RaisedButton style={style} label="Secondary" secondary={true}/>
    </div>
  </MuiThemeProvider>
)
