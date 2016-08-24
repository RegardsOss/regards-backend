import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"
import FlatButton from "material-ui/FlatButton"
import TextField from "material-ui/TextField"


interface PickDatasourceFormProps {
  handleNextStep: () => void
}
/**
 */
export default class PickDatasourceFormComponent extends React.Component<PickDatasourceFormProps, any> {



  handleNextButton = () => {
    this.props.handleNextStep()
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
          <div>
            <SelectField
              floatingLabelText="Type de source de données"
              value={3}
            >
              <MenuItem value={1} primaryText="Oracle"/>
              <MenuItem value={2} primaryText="Mysql"/>
              <MenuItem value={3} primaryText="PostgreSQL"/>
              <MenuItem value={4} primaryText="Weekends"/>
              <MenuItem value={5} primaryText="Weekly"/>
            </SelectField>
            <FlatButton label="Add datasource" primary={true}/>
          </div>
          <br />
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

/*
 const mapStateToProps = (state: any, ownProps: any) => {
 }
 const mapDispatchToProps = (dispatch: any) => ({
 })
 export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetCreateContainer)
 */
