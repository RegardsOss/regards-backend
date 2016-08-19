import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import FlatButton from "material-ui/FlatButton"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"
import TextField from "material-ui/TextField"

/**
 */
export default class CreateDatasourceFormComponent extends React.Component<any, any> {


  render (): JSX.Element {
    return (
      <Card
        initiallyExpanded={true}>
        <CardHeader
          title={<FormattedMessage id="datamanagement.datasource.create.header"/>}
          actAsExpander={true}
          showExpandableButton={false}
        />
        <CardText>


          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="datamanagement.model.create.label"/>}
            fullWidth={true}
          />

          <SelectField
            floatingLabelText="Select connection"
            value={3}
            fullWidth={true}
          >
            <MenuItem value={1} primaryText="Never"/>
            <MenuItem value={2} primaryText="Every Night"/>
            <MenuItem value={3} primaryText="Weeknights"/>
            <MenuItem value={4} primaryText="Weekends"/>
            <MenuItem value={5} primaryText="Weekly"/>
          </SelectField>

          <FlatButton label="New connection" primary={true}/>
          <hr />


          <SelectField
            floatingLabelText="Select modele"
            value={3}
            fullWidth={true}
          >
            <MenuItem value={1} primaryText="Never"/>
            <MenuItem value={2} primaryText="Every Night"/>
            <MenuItem value={3} primaryText="Weeknights"/>
            <MenuItem value={4} primaryText="Weekends"/>
            <MenuItem value={5} primaryText="Weekly"/>
          </SelectField>

          <FlatButton label="Create new model" primary={true}/>
          <hr />
          <SelectField
            floatingLabelText="Select plugin"
            value={3}
            fullWidth={true}
          >
            <MenuItem value={1} primaryText="Plugin #1"/>
            <MenuItem value={2} primaryText="Plugin #2"/>
            <MenuItem value={3} primaryText="Plugin #3"/>
            <MenuItem value={4} primaryText="Plugin #4"/>
            <MenuItem value={5} primaryText="Plugin #5"/>
          </SelectField>

          <FlatButton label="Create new plugin" primary={true}/>
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
