import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"


/**
 */
export default class CreateDatasetSuccessComponent extends React.Component<any, any> {


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
          <h3>Success !</h3>
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
