import * as React from "react"
import { Card, CardTitle, CardText } from "material-ui/Card"
import CancelButtonComponent from "../../components/CancelButtonComponent"
import MainButtonComponent from "../../components/MainButtonComponent"
import { map, find } from "lodash"
import FlatButton from "material-ui/FlatButton"
import TextField from "material-ui/TextField"
import { ModelAttribute } from "../ModelAttribute"
import CreateAttributeModal from "./CreateAttributeModal"
import Delete from "material-ui/svg-icons/action/delete"
import { TableRowColumn, Table, TableBody, TableHeader, TableHeaderColumn, TableRow } from "material-ui/Table"
import { FormattedMessage } from "react-intl"
import Edit from "material-ui/svg-icons/editor/mode-edit"
import { JavaTypes } from "../../JavaTypes"

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
  handleEditBtn = (entity: ModelAttribute) => {
    console.log("todo")
  }

  printType = (typeValue: string) => {
    const type: any = find(JavaTypes, {"value": typeValue})

    return (<FormattedMessage id={type.toString()}/>)
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
            <TableHeaderColumn>
              <FormattedMessage
                id="datamanagement.model.table.name"/>
            </TableHeaderColumn>
            <TableHeaderColumn>
              <FormattedMessage
                id="datamanagement.model.table.type"/>
            </TableHeaderColumn>
            <TableHeaderColumn>
              <FormattedMessage
                id="datamanagement.model.table.actions"/>
            </TableHeaderColumn>
            <TableHeaderColumn>
            </TableHeaderColumn>
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
                {this.printType(attribute.type)}
              </ TableRowColumn >
              <TableRowColumn>
                <FlatButton
                  icon={<Edit />}
                  onTouchTap={() => this.handleEditBtn(attribute)}
                  disabled={true}
                />
              </TableRowColumn>
              <TableRowColumn>
                <FlatButton
                  icon={<Delete />}
                  onTouchTap={() => this.handleDeleteBtn(attribute)}
                />
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
        <CardTitle
          title={
            <FormattedMessage
              id="datamanagement.dataset.add.header"
              values={
                {
                  label: <i>{labelAsTitle}</i>
                }
              }
            />
          }
        />
        <CardText>
          {modal}

          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="datamanagement.model.add.input.name" />}
            fullWidth={true}
            onChange={this.handleModelLabelChange}
          />
          <div style={styleAddAttribute}>
            <FlatButton
              label={<FormattedMessage id="datamanagement.model.add.action.add_attribute" />}
              primary={true}
              onClick={this.handleOpenPopupCreateParameter}
            />
          </div>

          {currentListAttributes}

          <div style={styleCardActions}>
            <CancelButtonComponent
              label={<FormattedMessage id="datamanagement.model.add.action.cancel" />}
              url={this.handleCancelUrl()}
            />
            <MainButtonComponent
              label={<FormattedMessage id="datamanagement.model.add.action.save" />}
              onTouchTap={this.handleSaveButton}
              isVisible={isSaveButtonVisible}
            />
          </div>
        </CardText>
      </Card>
    )
  }
}
