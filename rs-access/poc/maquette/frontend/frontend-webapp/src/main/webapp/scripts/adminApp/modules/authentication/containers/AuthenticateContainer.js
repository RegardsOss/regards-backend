import React from 'react';
import { connect } from 'react-redux';

import { getThemeStyles } from 'common/theme/ThemeUtils';

import LoginComponent from '../components/LoginComponent';
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
    const styles = getThemeStyles(this.props.theme, 'adminApp/login');
    return (
      <LoginComponent
        styles={styles}
        onLogin={this.onLogin}
        errorMessage={this.props.errorMessage}/>
    );
  }
}

const mapStateToProps = (state)=> {
  return {
    errorMessage: state.authentication.error,
    theme: state.theme
  }
}
export default connect(mapStateToProps)(AuthenticateContainer);
