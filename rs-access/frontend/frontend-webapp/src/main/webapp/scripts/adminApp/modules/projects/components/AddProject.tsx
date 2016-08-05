import * as React from "react"
import Dialog from "material-ui/Dialog"
import FlatButton from "material-ui/FlatButton"
import TextField from "material-ui/TextField"
import AddBox from "material-ui/svg-icons/content/add-box"

/**
 * Dialog with action buttons. The actions are passed in as an array of React objects,
 * in this example [FlatButtons](/#/components/flat-button).
 *
 * You can also close this dialog by clicking outside the dialog, or with the 'Esc' key.
 */
export default class AddProject extends React.Component<any, any> {
  state: any = {
    open: false,
    value: ''
  }

  handleOpen = () => {
    this.setState ({open: true})
  }

  handleClose = () => {
    this.setState ({open: false})
  }

  handleSave = () => {
    this.handleClose ()
    this.props.onSave (this.state.value)
  }

  handleChange = (event: any) => {
    this.setState ({
      value: event.target.value,
    })
  }

  render(): any {
    const actions = [
      <FlatButton
        label="Cancel"
        primary={true}
        onTouchTap={this.handleClose}
      />,
      <FlatButton
        label="Submit"
        primary={true}
        keyboardFocused={true}
        onTouchTap={this.handleSave}
      />,
    ]

    return (
      <div>
        <FlatButton
          primary={true}
          label="Add"
          onTouchTap={this.handleOpen}
          icon={<AddBox />}/>
        <Dialog
          title="Add a new project"
          actions={actions}
          modal={false}
          open={this.state.open}
          onRequestClose={this.handleClose}
        >
          <TextField
            floatingLabelText="Project name"
            onChange={this.handleChange}/>
        </Dialog>
      </div>
    )
  }
}
