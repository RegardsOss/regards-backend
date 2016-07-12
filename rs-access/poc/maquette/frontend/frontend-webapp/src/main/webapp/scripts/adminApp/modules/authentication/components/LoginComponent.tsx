import * as React from 'react';

interface LoginProps {
  styles: Object,
  onLogin: (username: string, password: string) => void,
  errorMessage: string
}

class LoginComponent extends React.Component<LoginProps,any> {

  constructor(){
    super();
    this.state = {
      username: "",
      password: "",
      error :""
    }
    this.handleKeyPress = this.handleKeyPress.bind(this)
  }

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
