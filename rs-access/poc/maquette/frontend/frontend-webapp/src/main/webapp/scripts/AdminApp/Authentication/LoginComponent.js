import React from 'react';
import { Rest } from 'grommet';
import CSSModules from 'react-css-modules';

const template = "cdpp";
const styles = require('AdminApp/login');

class LoginComponent extends React.Component {

  constructor(){
    super();
    this.state = {
      username: "",
      password: "",
      error :""
    }
  }

  render(){
    return (
      <div styleName="login-modal">
        <p styleName="login-error">{this.props.errorMessage}</p>
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
        <button onClick={() => {
            this.props.onLogin(this.state.userName,this.state.password);
          }}>Log in</button>
      </div>
    );
  }
}

LoginComponent.propTypes = {
  onLogin: React.PropTypes.func.isRequired,
  errorMessage: React.PropTypes.string
}

export default CSSModules(LoginComponent,styles);
