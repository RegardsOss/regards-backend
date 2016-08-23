import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import FlatButton from "material-ui/FlatButton"
import CancelButtonComponent from "../../components/CancelButtonComponent"
interface CreateDatasetSuccessProps {
  getCancelUrl: () => string
  handleNextStep: () => void
}
/**
 */
export default class CreateDatasetSuccessComponent extends React.Component<CreateDatasetSuccessProps, any> {


  handleNextButton = () => {
    return this.props.handleNextStep()
  }
  handleCancelUrl = (): string => {
    return this.props.getCancelUrl()
  }


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
          <h3>Create model</h3>
          <CancelButtonComponent
            label="Back"
            url={this.handleCancelUrl()}
          />
          <FlatButton
            label="Save"
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
