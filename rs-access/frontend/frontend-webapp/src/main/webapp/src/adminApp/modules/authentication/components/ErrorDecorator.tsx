/** @module Todo */
import * as React from "react"
import { ThemeContextType, ThemeContextInterface } from "../../../../common/theme/ThemeContainerInterface"
// Containers

export interface ErrorDecoratorProps {
  muiTheme?: any
}

/**
 * React component to add the color on a text or another component
 */
export default class ErrorDecoratorComponent extends React.Component<ErrorDecoratorProps, any> {
  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface

  render (): JSX.Element {
    const errorStyle = this.context.muiTheme.palette.errorColor
    return (
      <span style={errorStyle}>
        { this.props.children }
      </span>
    )
  }
}

