/** @module AdminAuthentication */
import * as React from 'react'
import { connect } from 'react-redux'

import LoginComponent from '../components/LoginComponent'
import { fetchAuthenticate } from '../../../../common/authentication/AuthenticateActions'


/**
 * React container for authentication form.
 * Contains logic for authentication
 */
export class Authentication extends React.Component<any,any> {

  constructor(){
    super();
  }

  render(){
    return (
      <LoginComponent
        onLogin={this.props.onLogin}
        errorMessage={this.props.errorMessage}/>
    );
  }
}

const mapStateToProps = (state: any)=> ({
  errorMessage: state.common.authentication.error,
  theme: state.common.theme
})

const mapDispatchToProps = ( dispatch: any )=> ({
  onLogin: (userName: string,password: string) => dispatch(fetchAuthenticate(userName, password))
})
export default connect(mapStateToProps,mapDispatchToProps)(Authentication)
