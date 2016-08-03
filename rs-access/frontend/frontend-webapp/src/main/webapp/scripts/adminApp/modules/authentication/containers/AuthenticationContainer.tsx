/** @module AdminAuthentication */
import * as React from "react";
import { connect } from "react-redux";
import LoginComponent from "../components/LoginComponent";
import { fetchAuthenticate } from "../../../../common/authentication/AuthenticateActions";



export interface AuthenticationProps {
  onLogin?: (username: string, password: string) => void,
  errorMessage?: string
}

/**
 * React container for authentication form.
 * Contains logic for authentication
 */
export class Authentication extends React.Component<AuthenticationProps, any> {

  constructor() {
    super ();
  }

  render(): JSX.Element {
    return (
      <LoginComponent
        onLogin={this.props.onLogin}
        errorMessage={this.props.errorMessage}/>
    );
  }
}

const mapStateToProps = (state: any) => ({
  errorMessage: state.common.authentication.error
})

const mapDispatchToProps = (dispatch: any) => ({
  onLogin: (userName: string, password: string) => dispatch (fetchAuthenticate (userName, password))
})
// export default connect (mapStateToProps, mapDispatchToProps) (Authentication)
export default connect<{}, {}, AuthenticationProps> (mapStateToProps, mapDispatchToProps) (Authentication);
