import * as React from "react"
import { Card, CardHeader, CardText, CardTitle } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import TextField from "material-ui/TextField"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"
import FlatButton from "material-ui/FlatButton"
import { Model } from "../../../../model/Model"
import { map } from "lodash"
import MainButtonComponent from "../../../../components/MainButtonComponent"
import CancelButtonComponent from "../../../../components/CancelButtonComponent"
import PickModelModelAttributeDefaultValuesComponent from "./DatasetModelAttributeComponent"

interface FormProps {
  handleNextStep: () => void
  goToNewModel: () => void
  save: (label: string, modelType: number, attributesDefined: Array<any>) => void
  handleGetBack: () => void
  models: Array<Model>
}
/**
 */
class FormComponent extends React.Component<FormProps, any> {

  state: any = {
    label: "",
    modelType: 0
  }
  refs: {
    defaultModelAttributeValues: PickModelModelAttributeDefaultValuesComponent
  }

  handleGetBack = () => {
    return this.props.handleGetBack()
  }

  handleNextButton = () => {
    const {modelType, label} = this.state
    const attributesDefined = this.getAttributesDefined()
    this.props.save(label, modelType, attributesDefined)
    this.props.handleNextStep()
  }


  handleNewModel = () => {
    this.props.goToNewModel()
  }

  handleDatasetLabelChange = (event: React.FormEvent): any => {
    const newLabel = (event.target as any).value
    this.setState({
      label: newLabel
    })
  }

  handleModelTypeChange = (event: React.FormEvent, index: number, value: any) => {
    this.setState({
      modelType: value
    })
  }
  getAttributesDefined = () => {
    // We use refs here because we do not want these values to be reactive or connected to Redux
    return this.refs.defaultModelAttributeValues.getAttributesDefined()
  }

  render (): JSX.Element {
    const {models} = this.props
    const {modelType, label} = this.state
    const styleCardActions = {
      display: "flex",
      flexDirection: "row",
      justifyContent: "flex-end"
    }
    const isNextButtonVisible = modelType > 0 && label.length > 0
    const isModelListAttributeVisible = modelType > 0
    const defaultModelValuesComponent = isModelListAttributeVisible ? (
      <PickModelModelAttributeDefaultValuesComponent
        model={models[modelType]}
        ref="defaultModelAttributeValues"
      ></PickModelModelAttributeDefaultValuesComponent>
    ) : null
    return (
      <Card
        initiallyExpanded={true}>
        <CardTitle
          title={<FormattedMessage id="datamanagement.dataset.add.header"/>}
          children={this.props.children}
        />

        <CardText>
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="datamanagement.dataset.add.1.label"/>}
            fullWidth={true}
            onChange={this.handleDatasetLabelChange}
          />
          <SelectField
            floatingLabelText={<FormattedMessage id="datamanagement.dataset.add.1.modelType" />}
            value={modelType}
            onChange={this.handleModelTypeChange}
          >
            {map(models, (model: Model, id: string) => (
              <MenuItem key={id} value={model.id} primaryText={model.name}/>
            ))}
          </SelectField>
          <FlatButton
            label={<FormattedMessage id="datamanagement.dataset.add.1.action.createNewModel" />}
            primary={true}
            onTouchTap={this.handleNewModel}
          />

          {defaultModelValuesComponent}

          <div style={styleCardActions}>

            <CancelButtonComponent
              label={<FormattedMessage id="datamanagement.dataset.add.1.action.back" />}
              onTouchTap={this.handleGetBack}
            />
            <MainButtonComponent
              label={<FormattedMessage id="datamanagement.dataset.add.1.action.next" />}
              onTouchTap={this.handleNextButton}
              isVisible={isNextButtonVisible}
            />
          </div>
        </CardText>
      </Card>
    )
  }
}

export default FormComponent
/*

 <TimePicker
 format="24hr"
 hintText="Attribut 3 de type date"
 fullWidth={true}
 />
 <SelectField
 floatingLabelText="Input type"
 value={3}
 fullWidth={true}
 >
 <MenuItem value={1} primaryText="Integer"/>
 <MenuItem value={2} primaryText="Float"/>
 <MenuItem value={3} primaryText="String"/>
 <MenuItem value={4} primaryText="Geometric"/>
 </SelectField>
 */
