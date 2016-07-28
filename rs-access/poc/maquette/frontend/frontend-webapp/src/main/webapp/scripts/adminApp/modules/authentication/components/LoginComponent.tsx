/** @module AdminAuthentication */
import * as React from 'react';

import { FormattedMessage } from 'react-intl'
// Containers
import I18nProvider from '../../../../common/i18n/I18nProvider'

export interface LoginProps {
  onLogin: (username: string, password: string) => void,
  errorMessage: string
}

/**
 * React component for login form in administration application
 * @prop {Function} onLogin Callback for on login action
 * @prop {String} errorMessage Error message to display
 */
class LoginComponent extends React.Component<LoginProps,any> {

  constructor(){
    super();
    this.state = {
      username: "",
      password: "",
      error :""
    }
  }

  componentWillMount(){
    this.handleKeyPress = this.handleKeyPress.bind(this);
  }

  /**
   * handleKeyPress - Handle 'Enter' key press to validate form
   *
   * @param  {type} event: KeyboardEvent
   * @return {type}
   */
  handleKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.props.onLogin(this.state.username,this.state.password)
    }
  }

  render(){
    const { styles } : any = this.props
    let errorMessage:any = null
    if (this.props.errorMessage && this.props.errorMessage !== ''){
      errorMessage = <FormattedMessage id={this.props.errorMessage} />
    }
    return (
      <I18nProvider messageDir="adminApp/modules/authentication/i18n">
        <div onKeyDown={this.handleKeyPress}>
            <p>{errorMessage}</p>
            <label htmlFor="username" ><FormattedMessage id="login.username" /></label>
            <input type='text' onChange={(event: React.FormEvent) => {
              this.setState({ "username" :(event.target as any).value})
            }}/>
            <label htmlFor="password" ><FormattedMessage id="login.password" /></label>
            <input type="password" onChange={(event: React.FormEvent) => {
              this.setState({"password": (event.target as any).value})
            }}/>
            <button onClick={() => {
                this.props.onLogin(this.state.username,this.state.password);
              }}><FormattedMessage id="login.button" /></button>
          </div>
      </I18nProvider>
    );
  }
}

export default LoginComponent
