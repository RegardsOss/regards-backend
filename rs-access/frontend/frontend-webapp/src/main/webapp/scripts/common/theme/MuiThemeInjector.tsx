/** @module CommonTheme */
import * as React from 'react'
import { ThemeContextType } from "./ThemeContainerInterface"

/**
 * Retrieves the Material UI theme fom the context
 * and injects it as a prop to its child
 */
class MuiThemeInjector extends React.Component<any, any> {

  static contextTypes: Object = {
    muiTheme: ThemeContextType.muiTheme
  }
  context: any

  render(): JSX.Element {
    const { muiTheme } = this.context
    const child = React.Children.only(this.props.children)

    return React.cloneElement(child, { muiTheme: muiTheme })
  }
}

export default MuiThemeInjector
