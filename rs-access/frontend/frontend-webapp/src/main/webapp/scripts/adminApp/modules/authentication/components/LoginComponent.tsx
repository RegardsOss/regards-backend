/** @module AdminAuthentication */
import * as React from "react";
import { FormattedMessage } from "react-intl";
import I18nProvider from "../../../../common/i18n/I18nProvider";
import {Card, CardActions, CardHeader, CardMedia, CardTitle, CardText} from 'material-ui/Card';
import FlatButton from 'material-ui/FlatButton';
import TextField from 'material-ui/TextField';

export interface LoginProps {
  onLogin: (username: string, password: string) => void,
  errorMessage: string
}

/**
 * React component for login form in administration application
 * @prop {Function} onLogin Callback for on login action
 * @prop {String} errorMessage Error message to display
 */
class LoginComponent extends React.Component<LoginProps, any> {

  constructor() {
    super ();
    this.state = {
      username: "",
      password: "",
      error: ""
    }
  }

  componentWillMount(): any {
    this.handleKeyPress = this.handleKeyPress.bind (this);
  }

  /**
   * handleKeyPress - Handle 'Enter' key press to validate form
   *
   * @param  {type} event: KeyboardEvent
   * @return {type}
   */
  handleKeyPress(event: KeyboardEvent): any {
    if (event.key === 'Enter') {
      this.props.onLogin (this.state.username, this.state.password)
    }
  }

  render(): any {
    let errorMessage: any = null
    if (this.props.errorMessage && this.props.errorMessage !== '') {
      errorMessage = <FormattedMessage id={this.props.errorMessage}/>
    }
    return (
      <I18nProvider messageDir="adminApp/modules/authentication/i18n">
        <Card>
          <CardHeader
            title="URL Avatar"
            subtitle="Subtitle"
            avatar="http://lorempixel.com/100/100/nature/"
          />
          <CardTitle title="Card title" subtitle="Card subtitle" />
          <CardText>

            <div onKeyDown={this.handleKeyPress}>

              <p>{errorMessage}</p>
              <label htmlFor="username"><FormattedMessage id="login.username"/></label>
              <TextField
                hintText="login.username"
                floatingLabelText="username"
                onChange={(event: React.FormEvent) => {
                  this.setState({ "username" :(event.target as any).value})
                }}
              /><br/>
              <label htmlFor="password"><FormattedMessage id="login.password"/></label>
              <TextField
                hintText="login.password"
                floatingLabelText="password"
                type="password"
                onChange={(event: React.FormEvent) => {
                  this.setState({"password": (event.target as any).value})
                }}
              />

            </div>
          </CardText>
          <CardActions>
            <button onClick={() => {
                this.props.onLogin(this.state.username,this.state.password);
              }}><FormattedMessage id="login.button"/></button>
          </CardActions>
        </Card>
      </I18nProvider>
    );
  }
}

export default LoginComponent
