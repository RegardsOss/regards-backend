/** @module common */
import * as React from "react"
import { map } from "lodash"
import { Card, CardText, CardTitle } from "material-ui/Card"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"
import { intlShape } from "react-intl"

interface SelectLocaleTypes {
  locales: Array<string>,
  curentLocale: string,
  setLocale: (locale: string) => void
}


/**
 * React component to display the language selector widget
 */
class SelectLocaleComponent extends React.Component<SelectLocaleTypes, any> {

  static contextTypes: Object = {
    intl: intlShape
  }
  context: any

  constructor() {
    super ()
    this.handleChange = this.handleChange.bind (this)
  }

  handleChange(event: any, index: any, value: any): any {
    this.setState ({value})
    this.props.setLocale (value)
  }

  render(): JSX.Element {

    const items = map (this.props.locales, (locale: string) => {
      return <MenuItem value={locale} key={locale} primaryText={locale}/>
    })
    return (
      <Card>
        <CardTitle title={this.context.intl.formatMessage({id:"title"})} />
        <CardText>
          <SelectField
            value={this.props.curentLocale}
            onChange={this.handleChange}
            fullWidth={true}>
            {items}
          </SelectField>
        </CardText>
      </Card>
    )
  }
}

export default SelectLocaleComponent
