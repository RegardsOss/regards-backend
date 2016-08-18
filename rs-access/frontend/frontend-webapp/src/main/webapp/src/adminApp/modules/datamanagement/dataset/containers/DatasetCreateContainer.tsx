import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import TextField from "material-ui/TextField"
import I18nProvider from "../../../../../common/i18n/I18nProvider"


/**
 */
export default class DatasetCreateContainer extends React.Component<any, any> {


  render (): JSX.Element {
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <Card
          initiallyExpanded={true}>
          <CardHeader
            title={<FormattedMessage id="userlist.header"/>}
            actAsExpander={true}
            showExpandableButton={false}
          />
          <CardText>
            <TextField
              type="text"
              floatingLabelText={<FormattedMessage id="login.username"/>}
              fullWidth={true}
            />
          </CardText>
        </Card>
      </I18nProvider>
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
