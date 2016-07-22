import * as React from 'react'
import { connect } from 'react-redux'
import { forEach } from 'lodash'
import { setTheme } from '../actions/ThemeActions'
import SelectField from 'material-ui/SelectField'
import MenuItem from 'material-ui/MenuItem'
import {Card, CardText} from 'material-ui/Card';

class SelectTheme extends React.Component<any, any> {

  constructor() {
    super()
    this.handleChange = this.handleChange.bind(this)
  }

  handleChange(event: any, index: any, value: any) {
    this.setState({value})
    this.props.setTheme(value)
  }

  render() {
    const items = this.props.themes.map((theme: any) => {
      return <MenuItem value={theme} key={theme} primaryText={theme} />
    })

    return (
      <Card>
        <CardText>
          <SelectField
            value={this.props.currentTheme}
            onChange={this.handleChange}
            fullWidth={true} >
            {items}
          </SelectField>
        </CardText>
      </Card>
    )
  }
}

const mapStateToProps = (state: any) => ({
  themes: state.common.themes.items,
  currentTheme: state.common.themes.selected
})
const mapDispatchToProps = (dispatch: any) => ({
  setTheme: (theme: string) => dispatch(setTheme(theme))
})

export default connect<{}, {}, {}>(mapStateToProps, mapDispatchToProps)(SelectTheme);
