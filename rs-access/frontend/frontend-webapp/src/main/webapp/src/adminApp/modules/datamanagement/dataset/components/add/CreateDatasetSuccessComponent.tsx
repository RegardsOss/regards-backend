import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import FlatButton from "material-ui/FlatButton"

interface CreateDatasetSuccessProps {
  handleNextStep: () => void
}
/**
 */
export default class CreateDatasetSuccessComponent extends React.Component<CreateDatasetSuccessProps, any> {


  handleNextButton = () => {
    this.props.handleNextStep()
  }


  render (): JSX.Element {
    return (
      <Card
        initiallyExpanded={true}>
        <CardHeader
          title={<FormattedMessage id="datamanagement.dataset.add.header"/>}
          actAsExpander={true}
          showExpandableButton={false}
        />
        <CardText>
          <h3>Success !</h3>

          <FlatButton
            label="Go back to datamanager"
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
