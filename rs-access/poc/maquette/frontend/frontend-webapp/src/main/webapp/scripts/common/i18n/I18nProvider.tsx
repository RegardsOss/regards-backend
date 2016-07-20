/** @module common */
import * as React from 'react'
import { connect } from 'react-redux'
import { addLocaleData, IntlProvider } from 'react-intl'
import * as fr from 'react-intl/locale-data/fr'
import { updateMessages } from './I18nActions'
import { localeMessagesStore } from './I18nReducers'

addLocaleData(fr)

interface i18nProps {
  messageDir : string,
  // Properties set by react redux connection
  locale?: string,
  updateMessages?: (messagesDir:string, locale:string)=>void,
  messages?: Array<localeMessagesStore>,
  children?: any
}

export class I18nContainer extends React.Component<i18nProps, any> {

  componentWillMount(){
    // Get messages associated to this Prodiver via the messageDir
    let localMessages = this.props.messages.find( (message) => message.messagesDir === this.props.messageDir)

    // init messages if not set
    if (!localMessages){
      this.props.updateMessages(this.props.messageDir,this.props.locale)
    }
  }

  render(){

    // Get messages associated to this Prodiver via the messageDir
    let localMessages = this.props.messages.find( (message) => message.messagesDir === this.props.messageDir)
    if (localMessages){
      return (
        <IntlProvider
          locale={this.props.locale}
          messages={localMessages.messages}>
          {this.props.children}
        </IntlProvider>
      )
    } else {
      return null
    }
  }
}

const mapStateToProps = (state:any) => ({
  locale: state.common.i18n.locale,
  messages: state.common.i18n.messages
})

const mapDispatchToProps = (dispatch:any) => ({
  updateMessages: (messageDir:string, locale:string) => dispatch(updateMessages(messageDir,locale))
})

export default connect<{}, {}, i18nProps>(mapStateToProps, mapDispatchToProps)(I18nContainer)
