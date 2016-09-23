import * as React from "react";
import Paper from "material-ui/Paper";
import MuiThemeProvider from "material-ui/styles/MuiThemeProvider";
import getMuiTheme from "material-ui/styles/getMuiTheme";
import { map } from "lodash";

const style = {
  height: 200,
  width: 200,
  margin: 20,
  textAlign: 'center',
  display: 'inline-block',
  paddingTop: 60,
  fontSize: 'small'
}

const hexaStyle = {
  fontWeight: 900,
  fontSize: '1.3em',
}

const merge = (style, color) => Object.assign({}, style, {backgroundColor: color})

export default class Palette extends React.Component {
  render() {
    const theme = getMuiTheme(this.props.theme)
    return (
      <MuiThemeProvider muiTheme={theme}>
        <div>
          {
            map(theme.palette, (value, key) => {
              return <Paper style={merge(style, value)}>{key}<br />
                <div style={hexaStyle}>{value}</div>
              </Paper>
            })
          }
        </div>
      </MuiThemeProvider>
    )
  }
}

