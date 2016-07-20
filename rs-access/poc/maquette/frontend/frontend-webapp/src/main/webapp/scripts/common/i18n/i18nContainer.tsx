/** @module common */
import * as React from 'react'
import { connect } from 'react-redux'
import { addLocaleData, IntlProvider } from 'react-intl'
import * as fr from 'react-intl/locale-data/fr'

addLocaleData(fr)

interface i18nProps {
  // Properties set by react redux connection
  locale?: string,
  messages?: any,
  children?: any
}

export class I18nContainer extends React.Component<i18nProps, any> {

  render(){
    return (
      <IntlProvider
        locale={this.props.locale}
        messages={this.props.messages}>
        {this.props.children}
      </IntlProvider>
    )
  }
}

const mapStateToProps = (state:any) => ({
  locale: state.common.i18n.locale,
  messages: state.common.i18n.messages
})

export default connect<{}, {}, i18nProps>(mapStateToProps)(I18nContainer)
