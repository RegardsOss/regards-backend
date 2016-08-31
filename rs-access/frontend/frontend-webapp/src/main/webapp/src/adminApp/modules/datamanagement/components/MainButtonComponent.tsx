import * as React from "react"
import FlatButton from "material-ui/FlatButton"
import RaisedButton from "material-ui/RaisedButton"

import { Link } from "react-router"

/**
 * Generic back button
 */
interface MainButtonProps {
  label: string | JSX.Element
  url?: string
  style?: any
  onTouchTap?: (event: any) => void
  isVisible?: boolean
}
export default class MainButtonComponent extends React.Component<MainButtonProps, any> {


  handleClickBtn = (event: any): void => {
    this.props.onTouchTap(event)
  }

  render (): JSX.Element {
    const {url, label} = this.props
    const styleLink = this.props.style ? this.props.style : {}
    // If isVisible is defined && === false, then display nothing / otherwise display the button
    const isVisible = typeof this.props.isVisible === "boolean" ? this.props.isVisible : true
    if (!isVisible) {
      return null
    }
    if (url) {
      return (
        <Link to={url} style={styleLink}>
          <RaisedButton
            label={label}
            primary={true}
          />
        </Link>
      )
    } else {
      return (
        <RaisedButton
          label={label}
          primary={true}
          onTouchTap={this.handleClickBtn}
        />
      )
    }
  }
}
