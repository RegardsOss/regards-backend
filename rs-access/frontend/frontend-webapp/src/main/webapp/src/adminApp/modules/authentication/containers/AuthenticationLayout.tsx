import * as React from "react"
import { ThemeContextType, ThemeContextInterface } from "../../../../common/theme/ThemeContainerInterface"
import Layout from "../../../../common/layout/containers/Layout"
import SelectTheme from "../../../../common/theme/containers/SelectTheme"
import SelectLanguage from "../../../../common/i18n/containers/SelectLocaleContainer"
import Authentication from "./AuthenticationContainer"
export class AuthenticationLayout extends React.Component<any, any> {

  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface

  constructor () {
    super()
  }

  render (): JSX.Element {
    const layoutStyle = this.context.muiTheme.adminApp.loginForm
    return (
      <Layout style={layoutStyle}>
        <div key='selectTheme'><SelectTheme /></div>
        <div key='authentication'><Authentication /></div>
        <div key='selectLanguage'><SelectLanguage locales={['en','fr']}/></div>
      </Layout>
    )
  }
}


export default AuthenticationLayout // connect<{}, {}, any> (mapStateToProps) (Authentication)
