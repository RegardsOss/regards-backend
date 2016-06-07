import React from 'react';
import { connect } from 'react-redux';

import LoginComponent from 'adminApp/components/LoginComponent';
import { fetchAuthenticate } from 'common/authentication/AuthenticateActions';

class AuthenticateContainer extends React.Component {

  constructor(){
    super();
    this.onLogin = this.onLogin.bind(this);
  }

  onLogin(userName, password){
    const { dispatch } = this.props;
    dispatch(fetchAuthenticate(userName, password));
  }

  render(){
    return (
      <LoginComponent
        onLogin={this.onLogin}
        errorMessage={this.props.errorMessage}/>
    );
  }
}

const mapStateToProps = (state)=> {
  return {
    errorMessage: state.authentication.error
  }
}
export default connect(mapStateToProps)(AuthenticateContainer);
