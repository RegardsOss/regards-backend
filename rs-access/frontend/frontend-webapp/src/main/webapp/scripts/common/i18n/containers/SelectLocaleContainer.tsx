/** @module common */
import * as React from "react"
import { connect } from "react-redux"
import { updateLocale } from "../I18nActions"
import SelectLocalComponent from "../components/SelectLocaleComponent"
import I18nProvider from "../I18nProvider"

interface SelectLocaleTypes {
  locales: Array<string>,
  curentLocale?: string,
  setLocale?: (locale: string) => void
}


/**
 * React component to display the language selector widget
 */
export class SelectLocaleContainer extends React.Component<SelectLocaleTypes, any> {

  constructor() {
    super ()
    this.handleChange = this.handleChange.bind (this)
  }

  handleChange(event: any, index: any, value: any): any {
    this.setState ({value})
    this.props.setLocale (value)
  }

  render(): any {

    return (
      <I18nProvider messageDir="common/i18n/messages">
        <SelectLocalComponent locales={this.props.locales} curentLocale={this.props.curentLocale} setLocale={this.props.setLocale}/>
      </I18nProvider>
    )
  }
}

// Add projects from store to the container props
const mapStateToProps = (state: any) => {
  return {
    curentLocale: state.common.i18n.locale
  }
}

// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch: any) => {
  return {
    setLocale: (locale: string) => dispatch (updateLocale (locale))
  }
}

export default connect<{}, {}, SelectLocaleTypes> (mapStateToProps, mapDispatchToProps) (SelectLocaleContainer)
