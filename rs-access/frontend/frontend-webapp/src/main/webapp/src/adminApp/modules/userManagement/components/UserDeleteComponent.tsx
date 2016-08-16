import * as React from "react"
import Dialog from "material-ui/Dialog"
import FlatButton from "material-ui/FlatButton"
import { FormattedMessage } from "react-intl"
import { intlShape } from "react-intl"

/**
 * Dialog with action buttons.
 *
 * You can also close this dialog by clicking outside the dialog, or with the 'Esc' key.
 */

/**
 *
 */
export interface UserDeleteInterface {
  onClose: any,
  onDelete: any
}
/**
 *
 */
class UserDeleteComponent extends React.Component<UserDeleteInterface, any> {

  /**
   *
   * @type {{intl: ReactIntl.IntlShape}}
   */
  static contextTypes: Object = {
    intl: intlShape
  }
  context: any


  /**
   *
   */
  handleClose = () => {
    this.props.onClose()
  }

  /**
   *
   */
  handleDelete = () => {
    this.props.onDelete()
  }

  /**
   *
   * @returns {any}
   */
  render (): JSX.Element {
    const title = this.context.intl.formatMessage({id: "dialog.delete.title"})
    const actions = [
      <FlatButton
        label={<FormattedMessage id="dialog.delete.cancel"/>}
        primary={true}
        onTouchTap={this.handleClose}
      />,
      <FlatButton
        label={<FormattedMessage id="dialog.delete.accept"/>}
        primary={true}
        keyboardFocused={true}
        onTouchTap={this.handleDelete}
      />,
    ]
    return (
      <Dialog
        title={title}
        actions={actions}
        modal={false}
        open={true}
        onRequestClose={this.handleClose}
      >
      </Dialog>
    )
  }
}

export default UserDeleteComponent
