import * as React from 'react'
import { connect } from 'react-redux'

import { getThemeStyles } from '../../../common/theme/ThemeUtils'

import LoginComponent from './components/LoginComponent'
import { fetchAuthenticate } from '../../../common/authentication/AuthenticateActions'

class Authentication extends React.Component<any,any> {

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

const mapStateToProps = (state: any)=> {
  return {
    errorMessage: state.common.authentication.error,
    theme: state.common.theme
  }
}

const mapDispatchToProps = ( dispatch: any )=> {
  return {
    onLogin: (userName: string,password: string) => dispatch(fetchAuthenticate(userName, password))
  }
}
export default connect(mapStateToProps,mapDispatchToProps)(Authentication)
