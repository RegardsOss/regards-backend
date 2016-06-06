import React from 'react';
import { connect } from 'react-redux';
import Rest from 'grommet/utils/Rest';

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
        onLogin={this.onLogin} />
    );
  }
}

export default connect()(AuthenticateContainer);
