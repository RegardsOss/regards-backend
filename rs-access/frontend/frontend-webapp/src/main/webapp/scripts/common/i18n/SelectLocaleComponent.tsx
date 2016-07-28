/** @module common */
import * as React from 'react'
import { connect } from 'react-redux'

import { updateLocale } from './I18nActions'

interface SelectLocaleTypes {
  locales: Array<string>,
  curentLocale?: string,
  onLocaleChange?: (locale:string) => void
}


/**
 * React component to display the language selector widget
 */
export class SelectLocaleComponent extends React.Component<SelectLocaleTypes, any> {
  constructor(){
    super()
    this.onChange = this.onChange.bind(this)
  }

  onChange(e: any){
    this.props.onLocaleChange(e.target.value)
  }

  componentWillMount(){
    this.setState({
      selectedValue: this.props.curentLocale
    })
  }

  render(){
    const { locales, onLocaleChange } = this.props

    return (
      <div>
        <select
          value={this.props.curentLocale}
          onChange={this.onChange}>
            {locales.map( (locale) => {
                return <option key={locale} value={locale}>{locale}</option>
            })}
        </select>
      </div>
    )
  }

}

// Add projects from store to the container props
const mapStateToProps = (state:any) => {
  return {
    curentLocale: state.common.i18n.locale
  }
}

// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch:any) => {
  return {
    onLocaleChange: (locale:string) => dispatch(updateLocale(locale))
  }
}

export default connect<{}, {}, SelectLocaleTypes>(mapStateToProps,mapDispatchToProps)(SelectLocaleComponent)
