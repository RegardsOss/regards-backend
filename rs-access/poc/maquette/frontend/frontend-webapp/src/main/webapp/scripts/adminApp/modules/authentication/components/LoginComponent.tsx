/** @module AdminAuthentication */
import * as React from 'react';

interface LoginProps {
  styles: Object,
  onLogin: (username: string, password: string) => void,
  errorMessage: string
}

/**
 * React component for login form in administration application
 * @prop {Object} styles
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
    return (
      <div className={styles["login-modal"]} onKeyDown={this.handleKeyPress}>
        <p className={styles["login-error"]}>{this.props.errorMessage}</p>
        <label htmlFor="username" >Username</label>
        <input type='text' onChange={(event: React.FormEvent) => {
          this.setState({ "username" :(event.target as any).value})
        }}/>
        <label htmlFor="password" >Password</label>
        <input type="password" onChange={(event: React.FormEvent) => {
          this.setState({"password": (event.target as any).value})
        }}/>
        <button className={styles.button} onClick={() => {
            this.props.onLogin(this.state.username,this.state.password);
          }}>Log in</button>
      </div>
    );
  }
}

export default LoginComponent
