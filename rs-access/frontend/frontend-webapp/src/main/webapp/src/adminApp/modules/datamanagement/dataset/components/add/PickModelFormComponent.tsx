import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import TextField from "material-ui/TextField"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"
import FlatButton from "material-ui/FlatButton"


/**
 */
export default class PickModelFormComponent extends React.Component<any, any> {


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
          <SelectField
            floatingLabelText="Type de modèle"
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
          <br />
          <FlatButton label="Cancel" primary={true}/>
          <FlatButton label="Next" secondary={true}/>

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
