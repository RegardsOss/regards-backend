import * as React from "react"
import FlatButton from "material-ui/FlatButton"
import { Link } from "react-router"

/**
 * Generic back button
 */
interface CancelButtonProps {
  label: string,
  url: string,
  style?: any
}
export default class CancelButtonComponent extends React.Component<CancelButtonProps, any> {
  render (): JSX.Element {
    const styleLink = this.props.style ? this.props.style : {}
    const { url, label } = this.props
    return (
      <Link to={url} style={styleLink}>
        <FlatButton
          label={label}
          secondary={true}
        />
      </Link>
    )
  }
}
