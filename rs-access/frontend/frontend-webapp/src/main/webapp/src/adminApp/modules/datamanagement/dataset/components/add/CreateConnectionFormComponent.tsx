import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import TextField from "material-ui/TextField"
import FlatButton from "material-ui/FlatButton"
import SelectField from 'material-ui/SelectField';
import MenuItem from 'material-ui/MenuItem';


/**
 */
export default class CreateConnectionFormComponent extends React.Component<any, any> {


  render (): JSX.Element {
    return (
      <Card
        initiallyExpanded={true}>
        <CardHeader
          title={<FormattedMessage id="datamanagement.connection.create.header"/>}
          actAsExpander={true}
          showExpandableButton={false}
        />
        <CardText>

          <SelectField
            floatingLabelText="Select the type of connection"
            value={3}
            fullWidth={true}
          >
            <MenuItem value={1} primaryText="Plugin oracle"/>
            <MenuItem value={2} primaryText="Plugin mongodb"/>
            <MenuItem value={3} primaryText="Plugin cassandra"/>
          </SelectField>
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="Label"/>}
            fullWidth={true}
          />
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="Username"/>}
            fullWidth={true}
          />
          <TextField
            type="password"
            floatingLabelText={<FormattedMessage id="Password"/>}
            fullWidth={true}
          />
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="URL"/>}
            fullWidth={true}
          />
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="Port"/>}
            fullWidth={true}
          />
          <FlatButton label="Save this new connection" primary={true}/>
          <FlatButton label="Cancel" primary={true}/>

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
