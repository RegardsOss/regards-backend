import React from 'react'
import { connect } from 'react-redux'

import { getThemeStyles } from 'common/theme/ThemeUtils'

import LoginComponent from './components/LoginComponent'
import { fetchAuthenticate } from 'common/authentication/AuthenticateActions'

class Authentication extends React.Component {

  constructor(){
    super();
  }

  render(){
    const styles = getThemeStyles(this.props.theme, 'adminApp/styles')
    return (
      <LoginComponent
        styles={styles}
        onLogin={this.props.onLogin}
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

const mapDispatchToProps = ( dispatch )=> {
  return {
    onLogin: (userName,password) => dispatch(fetchAuthenticate(userName, password))
  }
}
export default connect(mapStateToProps,mapDispatchToProps)(Authentication)
