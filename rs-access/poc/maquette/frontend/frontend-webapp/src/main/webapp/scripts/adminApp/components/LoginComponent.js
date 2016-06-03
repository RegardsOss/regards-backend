import React from 'react';
import { Rest } from 'grommet';
import { connect } from 'react-redux';

import { getThemeStyles } from 'common/ThemeUtils';

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
    const styles = getThemeStyles(this.props.theme, 'adminApp/login');
    return (
      <div className={styles["login-modal"]}>
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
        <button onClick={() => {
            this.props.onLogin(this.state.username,this.state.password);
          }}>Log in</button>
      </div>
    );
  }
}

LoginComponent.propTypes = {
  onLogin: React.PropTypes.func.isRequired,
  errorMessage: React.PropTypes.string
}

const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
export default connect(mapStateToProps)(LoginComponent);
