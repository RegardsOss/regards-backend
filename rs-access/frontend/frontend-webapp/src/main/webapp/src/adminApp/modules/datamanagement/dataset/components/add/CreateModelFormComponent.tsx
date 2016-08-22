import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import TextField from "material-ui/TextField"
import FlatButton from "material-ui/FlatButton"
import TimePicker from "material-ui/TimePicker"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"

/**
 */
export default class CreateModelFormComponent extends React.Component<any, any> {


  render (): JSX.Element {
    return (
      <Card
        initiallyExpanded={true}>
        <CardHeader
          title={<FormattedMessage id="datamanagement.create.model.header"/>}
          actAsExpander={true}
          showExpandableButton={false}
        />
        <CardText>
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="datamanagement.create.model.attribute"/>}
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

          <TextField
            type="number"
            floatingLabelText="Attribut 1 de type string"
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
          <TextField
            type="text"
            floatingLabelText="Attribut 2 de type string"
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
          <FlatButton label="Create new model" primary={true}/>
          <br />
          <FlatButton label="Cancel" primary={true}/>
          <FlatButton label="Create" secondary={true}/>

        </CardText>
      </Card>
    )
  }
}

/*
 const mapStateToProps = (state: any, ownProps: any) => {
 }
 const mapDispatchToProps = (dispatch: any) => ({
 })
 export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetCreateContainer)
 */
