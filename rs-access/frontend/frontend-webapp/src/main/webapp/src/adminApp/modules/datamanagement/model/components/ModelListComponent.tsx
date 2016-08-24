import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import CancelButtonComponent from "../../components/CancelButtonComponent"
import MainButtonComponent from "../../components/MainButtonComponent"

interface ModelListProps {
  getBackUrl: () => string
  getCreateUrl: () => string
}
/**
 */
export default class ModelListComponent extends React.Component<ModelListProps, any> {


  getCreateUrl = (): string => {
    return this.props.getCreateUrl()
  }
  getBackUrl = (): string => {
    return this.props.getBackUrl()
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
          <h3>List model</h3>
          <CancelButtonComponent
            label="Back"
            url={this.getBackUrl()}
          />
          <MainButtonComponent
            label="Create new model"
            url={this.getCreateUrl()}
          />
        </CardText>
      </Card>
    )
  }
}
