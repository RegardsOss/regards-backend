import * as React from 'react'
import { connect } from 'react-redux'
import { forEach } from 'lodash'
import { setTheme } from '../actions/ThemeActions'
import SelectField from 'material-ui/SelectField'
import MenuItem from 'material-ui/MenuItem'

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
      <div>
        <SelectField
          value={this.props.currentTheme}
          onChange={this.handleChange}
          autoWidth={true} >
          {items}
        </SelectField>
      </div>
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
