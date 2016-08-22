import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import TextField from "material-ui/TextField"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"
import FlatButton from "material-ui/FlatButton"
import TimePicker from "material-ui/TimePicker"

interface PickModelFormProps {
  handleNextStep: () => void
  handleNewModel: () => void
}
/**
 */
export default class PickModelFormComponent extends React.Component<PickModelFormProps, any> {

  handleNextButton = () => {
    this.props.handleNextStep()
  }


  handleCreateNewModel = () => {
    this.props.handleNewModel()
  }

  render (): JSX.Element {
    return (
      <Card
        initiallyExpanded={true}>
        <CardHeader
          title={<FormattedMessage id="datamanagement.dataset.form.header"/>}
          actAsExpander={true}
          showExpandableButton={false}
        />
        <CardText>
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="datamanagement.dataset.form.create.label"/>}
            fullWidth={true}
          />
          <div>

            <SelectField
              floatingLabelText="Type de modèle"
              value={3}
            >
              <MenuItem value={1} primaryText="Never"/>
              <MenuItem value={2} primaryText="Every Night"/>
              <MenuItem value={3} primaryText="Weeknights"/>
              <MenuItem value={4} primaryText="Weekends"/>
              <MenuItem value={5} primaryText="Weekly"/>
            </SelectField>
            <FlatButton
              label="Create new model"
              primary={true}
              onTouchTap={this.handleCreateNewModel}
            />
          </div>
          <hr />
          <TextField
            type="number"
            floatingLabelText="Attribut 1 de type number"
            fullWidth={true}
          />
          <TextField
            type="text"
            floatingLabelText="Attribut 2 de type string"
            fullWidth={true}
          />
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
          <br />
          <FlatButton label="Cancel" primary={true}/>
          <FlatButton
            label="Next"
            secondary={true}
            onTouchTap={this.handleNextButton}
          />

        </CardText>
      </Card>
    )
  }
}
