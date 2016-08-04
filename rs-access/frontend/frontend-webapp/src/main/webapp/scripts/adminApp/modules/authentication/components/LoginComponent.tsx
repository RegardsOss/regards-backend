/** @module AdminAuthentication */
import * as React from "react";
import { FormattedMessage } from "react-intl";
import I18nProvider from "../../../../common/i18n/I18nProvider";
import { Card, CardActions, CardTitle, CardText } from "material-ui/Card";
import TextField from "material-ui/TextField";
import RaisedButton from "material-ui/RaisedButton";

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
    this.handleUserInputChange = this.handleUserInputChange.bind (this);
    this.handlePasswordInputChange = this.handlePasswordInputChange.bind (this);
    this.handleButtonPress = this.handleButtonPress.bind (this);
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

  handleUserInputChange(event: React.FormEvent): any {
    this.setState ({"username": (event.target as any).value})
  }

  handlePasswordInputChange(event: React.FormEvent): any {
    this.setState ({"password": (event.target as any).value})
  }

  handleButtonPress(event: React.FormEvent): any {
    this.props.onLogin (this.state.username, this.state.password)
  }

  render(): JSX.Element {
    let errorMessage: any = null
    if (this.props.errorMessage && this.props.errorMessage !== '') {
      errorMessage = <FormattedMessage id={this.props.errorMessage}/>
    }

    return (
      <I18nProvider messageDir="adminApp/modules/authentication/i18n">
        <Card>
          <CardTitle title="Connexion au panel d'admin"/>
          <CardText>
            <div onKeyDown={this.handleKeyPress}>
              <p>{errorMessage}</p>
              <TextField
                floatingLabelText={<FormattedMessage id="login.username"/>}
                fullWidth={true}
                onChange={this.handleUserInputChange}
              />
              <TextField
                floatingLabelText={<FormattedMessage id="login.password"/>}
                type="password"
                fullWidth={true}
                onChange={this.handlePasswordInputChange}
              />
            </div>
          </CardText>
          <CardActions style={{display: "flex",alignItems: "center",justifyContent: "center"}}>
            <RaisedButton
              label={<FormattedMessage id="login.button"/>}
              primary={true}
              onClick={this.handleButtonPress}
            />
          </CardActions>
        </Card>
      </I18nProvider>
    );
  }
}

export default LoginComponent
