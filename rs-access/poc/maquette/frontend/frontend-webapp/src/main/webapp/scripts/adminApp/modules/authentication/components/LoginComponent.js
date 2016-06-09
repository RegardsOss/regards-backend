import React from 'react';

class LoginComponent extends React.Component {

  constructor(){
    super();
    this.state = {
      username: "",
      password: "",
      error :""
    }
    this.handleKeyPress = this.handleKeyPress.bind(this);
  }

  handleKeyPress(e) {
    if (e.key === 'Enter') {
      this.props.onLogin(this.state.username,this.state.password);
    }
  }

  render(){
    const { styles } = this.props;
    return (
      <div className={styles["login-modal"]} onKeyDown={this.handleKeyPress}>
        <p className={styles["login-error"]}>{this.props.errorMessage}</p>
        <label for="username" >Username</label>
        <input id="username" onChange={(event) => {
             this.setState({username:event.target.value});
          }}/>
        <br/>
        <label for="password" >Password</label>
        <input type="password" id="password"
          onChange={(event) => {
               this.setState({password:event.target.value});
            }}/>
        <br/>
        <button className={styles.button} onClick={() => {
            this.props.onLogin(this.state.username,this.state.password);
          }}>Log in</button>
      </div>
    );
  }
}

LoginComponent.propTypes = {
  styles: React.PropTypes.object.isRequired,
  onLogin: React.PropTypes.func.isRequired,
  errorMessage: React.PropTypes.string
}

export default LoginComponent;
