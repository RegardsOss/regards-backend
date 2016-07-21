/** @module common */
import * as React from 'react'
import { FormattedMessage } from 'react-intl'
import I18nProvider from '../../i18n/I18nProvider'

interface SelectThemeTypes {
  styles: any,
  themes: Array<any>,
  curentTheme: string,
  onThemeChange: (theme:string) => void
}


/**
 * React component to display the Theme selector widget
 */
class SelectThemeComponent extends React.Component<SelectThemeTypes, any> {
  constructor(){
    super()
    this.onChange = this.onChange.bind(this)
  }

  onChange(e: any){
    this.props.onThemeChange(e.target.value)
  }

  componentWillMount(){
    this.setState({
      selectedValue: this.props.curentTheme
    })
  }

  render(){
    const { styles, themes, onThemeChange } = this.props

    return (
      <I18nProvider messageDir="common/theme/i18n">
        <div className={styles["select-theme"]}>
          <span> <FormattedMessage id="select.theme.label" /> </span>
          <select
            value={this.props.curentTheme}
            onChange={this.onChange}>
              {themes.map( (theme) => {
                  return <option key={theme} value={theme}>{theme}</option>
              })}
          </select>
        </div>
      </I18nProvider>
    )
  }

}

export default SelectThemeComponent
