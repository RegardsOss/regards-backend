import * as React from "react";
import { connect } from "react-redux";
import { map, keys } from "lodash";
import { setTheme } from "../actions/ThemeActions";
import SelectField from "material-ui/SelectField";
import MenuItem from "material-ui/MenuItem";
import { Card, CardText } from "material-ui/Card";
import ThemeHelper from "../ThemeHelper";

export class SelectTheme extends React.Component<any, any> {

  constructor() {
    super ()
    this.handleChange = this.handleChange.bind (this)
  }

  handleChange(event: any, index: any, value: any): any {
    this.setState ({value})
    this.props.setTheme (value)
  }

  render(): any {
    const themes = ThemeHelper.getThemes ()
    const themeNames = keys (themes)
    const items = map (themeNames, (themeName: string) => {
      return <MenuItem value={themeName} key={themeName} primaryText={themeName}/>
    })

    return (
      <Card>
        <CardText>
          <SelectField
            value={this.props.theme}
            onChange={this.handleChange}
            fullWidth={true}>
            {items}
          </SelectField>
        </CardText>
      </Card>
    )
  }
}

const mapStateToProps = (state: any) => ({
  theme: state.common.theme
})
const mapDispatchToProps = (dispatch: any) => ({
  setTheme: (theme: string) => dispatch (setTheme (theme))
})

export default connect<{}, {}, {}> (mapStateToProps, mapDispatchToProps) (SelectTheme);
