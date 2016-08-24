import * as React from "react"
import Dialog from "material-ui/Dialog"
import FlatButton from "material-ui/FlatButton"
import TextField from "material-ui/TextField"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"


interface CreateAttributeModalProps {
  handleCreateNewParameter: (label: string, type: string) => void
  handleCloseModal: () => void
}
export default class CreateAttributeModal extends React.Component<CreateAttributeModalProps, any> {
  state: any = {
    label: "",
    type: 0
  }


  handleClose = () => {
    this.props.handleCloseModal()
  }
  addAttribute = () => {
    const {label, type} = this.state;
    this.props.handleCreateNewParameter(label, type);
  }
  handleAddAndReset = (event: React.FormEvent) => {
    this.addAttribute()
    this.setState({
      label: "",
      type: 0
    })
  }
  handleAddAndClose = (event: React.FormEvent) => {
    this.addAttribute()
    this.handleClose()
  }

  handleAttributeLabelChange = (event: React.FormEvent): any => {
    const newLabel = (event.target as any).value
    this.setState({
      label: newLabel
    })
  }
  handleAttributeTypeChange = (event: React.FormEvent, index: number, value: any) => {
    this.setState({
      type: value
    })
  }

  render (): JSX.Element {
    const {label, type} = this.state

    let actions = [
      <FlatButton
        label="Close"
        primary={true}
        onTouchTap={this.handleClose}
      />
    ]
    // Display save buttons only if attribute is well defined
    if (label.length > 0 && type !== 0) {
      actions.push(<FlatButton
        label="Create and close"
        primary={true}
        onTouchTap={this.handleAddAndClose}
      />)
      actions.push(<FlatButton
        label="Create and add another"
        primary={true}
        onTouchTap={this.handleAddAndReset}
      />)
    }

    return (
      <div>
        <Dialog
          title="Add a new project"
          actions={actions}
          modal={false}
          open={true}
          onRequestClose={this.handleClose}
        >

          <h3>Create a new attribute</h3>
          <TextField
            type="text"
            floatingLabelText="Parameter name"
            value={label}
            fullWidth={true}
            onChange={this.handleAttributeLabelChange}
          />
          <SelectField
            floatingLabelText="Type"
            value={type}
            onChange={this.handleAttributeTypeChange}
          >
            <MenuItem value="integer" primaryText="Integer"/>
            <MenuItem value="float" primaryText="Float"/>
            <MenuItem value="string" primaryText="String"/>
            <MenuItem value="geometric" primaryText="Geometric"/>
          </SelectField>
        </Dialog>
      </div>
    )
  }
}
