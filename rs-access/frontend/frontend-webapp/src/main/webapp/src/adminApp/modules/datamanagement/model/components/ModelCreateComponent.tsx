import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import CancelButtonComponent from "../../components/CancelButtonComponent"
import MainButtonComponent from "../../components/MainButtonComponent"
import { map } from "lodash"
import FlatButton from "material-ui/FlatButton"
import TextField from "material-ui/TextField"
import { ModelAttribute } from "../ModelAttribute"
import CreateAttributeModal from "./CreateAttributeModal"
import Delete from "material-ui/svg-icons/action/delete"
import { TableRowColumn, Table, TableBody, TableHeader, TableHeaderColumn, TableRow } from "material-ui/Table"
import { FormattedMessage } from "react-intl"

interface ModelCreateProps {
  getCancelUrl: () => string
  handleNextStep: (name: string, attributes: Array<ModelAttribute>) => void
}

/**
 */
export default class ModelCreateComponent extends React.Component<ModelCreateProps, any> {


  constructor (props: ModelCreateProps) {
    super(props)
    this.state = {
      label: "",
      attributes: [],
      openCreateParameterModal: false
    }
  }

  handleSaveButton = (event: React.FormEvent) => {
    return this.props.handleNextStep(this.state.label, this.state.attributes)
  }
  handleCancelUrl = (): string => {
    return this.props.getCancelUrl()
  }
  handleModelLabelChange = (event: React.FormEvent): any => {
    const newLabel = (event.target as any).value
    this.setState({
      "label": newLabel
    })
  }
  handleCloseModal = () => {
    this.setState({
      openCreateParameterModal: false
    })
  }
  handleCreateNewParameter = (label: string, type: string) => {
    let {attributes} = this.state
    attributes.push({
      name: label,
      type: type
    })
    this.setState({
      attributes: attributes
    })
  }
  handleOpenPopupCreateParameter = () => {
    this.setState({
      openCreateParameterModal: true
    })
  }
  handleDeleteBtn = (entity: ModelAttribute) => {
    let {attributes} = this.state
    this.setState({
      attributes: attributes.filter((element: ModelAttribute) => element.name !== entity.name)
    })
  }
  handleEditBtn = () => {
    console.log("todo")
  }


  render (): JSX.Element {
    const {attributes, label, openCreateParameterModal} = this.state

    // display the modal if required
    const modal = openCreateParameterModal ? (
      <CreateAttributeModal
        handleCreateNewParameter={this.handleCreateNewParameter}
        handleCloseModal={this.handleCloseModal}
      />) : null


    // display the list of attributes if there is
    const currentListAttributes = attributes.length > 0 ? (
      <Table
        selectable={false}
        multiSelectable={false}
      >
        <TableHeader
          enableSelectAll={false}
          adjustForCheckbox={false}
          displaySelectAll={false}
        >
          <TableRow>
            <TableHeaderColumn><FormattedMessage id="datamanagement.model.create.attribute.name"/></TableHeaderColumn>
            <TableHeaderColumn><FormattedMessage id="datamanagement.model.create.attribute.type"/></TableHeaderColumn>
            <TableHeaderColumn><FormattedMessage
              id="datamanagement.model.create.attribute.actions"/></TableHeaderColumn>
          </TableRow>
        </TableHeader>
        <TableBody displayRowCheckbox={false} preScanRows={false}>
          {map(attributes, (attribute: ModelAttribute, id: string) => (
            <TableRow
              key={attribute.name}>
              <TableRowColumn>
                {attribute.name}
              </TableRowColumn>
              < TableRowColumn >
                {attribute.type}
              </ TableRowColumn >
              <TableRowColumn>
                <FlatButton icon={<Delete />} onTouchTap={() => this.handleDeleteBtn(attribute)}/>
              </TableRowColumn>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    ) : null


    const labelAsTitle = label ? "\"" + label + "\"" : ""
    const isSaveButtonVisible = attributes.length > 0 && label.length > 0
    const styleAddAttribute = {
      display: "flex",
      flexDirection: "row",
      justifyContent: "center"
    }
    const styleCardActions = {
      display: "flex",
      flexDirection: "row",
      justifyContent: "flex-end"
    }
    return (
      <Card
        initiallyExpanded={true}>
        <CardHeader
          title={"Create a new model " + labelAsTitle }
          actAsExpander={true}
          showExpandableButton={false}
        />
        <CardText>
          {modal}
          <TextField
            type="text"
            floatingLabelText="Model label"
            fullWidth={true}
            onChange={this.handleModelLabelChange}
          />

          <div style={styleAddAttribute}>
            <FlatButton
              label="Add attribute"
              primary={true}
              onClick={this.handleOpenPopupCreateParameter}
            />
          </div>

          {currentListAttributes}

          <div style={styleCardActions}>
            <CancelButtonComponent
              label="Cancel"
              url={this.handleCancelUrl()}
            />
            <MainButtonComponent
              label="Save model"
              onTouchTap={this.handleSaveButton}
              isVisible={isSaveButtonVisible}
            />
          </div>
        </CardText>
      </Card>
    )
  }
}
