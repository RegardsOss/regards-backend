import * as React from "react"
import { connect } from "react-redux"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import LoginComponent from "../components/LoginComponent"
import { fetchAuthenticate } from "../../../../common/authentication/AuthenticateActions"
import { fetchEndpoints } from "../../../../common/endpoints"

export interface AuthenticationProps {
  // From mapStateToProps
  theme?: any,
  errorMessage?: string
  // From mapDispatchToProps
  fetchAuthenticate?: (username: string, password: string) => void,
  fetchEndpoints?: () => void
}

export class Authentication extends React.Component<AuthenticationProps, any> {

  constructor () {
    super()
  }

  handleLogin = (userName: string, password: string) => {
    Promise.resolve(this.props.fetchAuthenticate(userName, password))
           .then(this.props.fetchEndpoints)
  }

  render (): JSX.Element {
    return (
      <I18nProvider messageDir="adminApp/modules/authentication/i18n">
        <LoginComponent
          onLogin={this.handleLogin}
          errorMessage={this.props.errorMessage}/>
      </I18nProvider>
    )
  }
}

const mapStateToProps = (state: any) => ({
  // If we do not have the theme variable here, subcontainers don't refresh
  // their theme when the user select another theme
  theme: state.common.theme,
  errorMessage: state.common.authentication.error
})
const mapDispatchToProps = (dispatch: any) => ({
  fetchAuthenticate: (userName: string, password: string) => dispatch(fetchAuthenticate(userName, password)),
  fetchEndpoints: () => dispatch(fetchEndpoints())
})

export default connect<{}, {}, AuthenticationProps>(mapStateToProps, mapDispatchToProps)(Authentication)
