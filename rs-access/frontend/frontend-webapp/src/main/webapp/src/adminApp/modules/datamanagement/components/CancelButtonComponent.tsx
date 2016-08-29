import * as React from "react"
import FlatButton from "material-ui/FlatButton"
import { Link } from "react-router"

/**
 * Generic back button
 */
interface CancelButtonProps {
  label: string | JSX.Element,
  url?: string,
  onTouchTap?: (event: any) => void
  style?: any
}
export default class CancelButtonComponent extends React.Component<CancelButtonProps, any> {
  handleClickBtn = (event: any): void => {
    this.props.onTouchTap(event)
  }
  render (): JSX.Element {
    const styleLink = this.props.style ? this.props.style : {}
    const {url, label} = this.props
    if (url) {
      return (

        <Link to={url} style={styleLink}>
          <FlatButton
            label={label}
            secondary={true}
          />
        </Link>
      )

    } else {
      return (
        <FlatButton
          label={label}
          primary={true}
          onTouchTap={this.handleClickBtn}
        />
      )
    }
  }
}
