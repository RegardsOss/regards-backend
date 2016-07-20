/** @module common */
import * as React from 'react'

interface SelectLocaleTypes {
  styles: any,
  locales: Array<string>,
  curentLocale: string,
  onLocaleChange: (theme:string) => void
}


/**
 * React component to display the language selector widget
 */
class SelectLocaleComponent extends React.Component<SelectLocaleTypes, any> {
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
    const { styles, locales, onLocaleChange } = this.props

    return (
      <div className={styles["select-language"]}>
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

export default SelectLocaleComponent
