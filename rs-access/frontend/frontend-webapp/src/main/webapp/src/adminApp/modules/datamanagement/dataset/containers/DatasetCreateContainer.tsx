import * as React from "react"
import { Card, CardHeader } from "material-ui/Card"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import { FormattedMessage } from "react-intl"
const URL_PROJECTS_USERS = "http://localhost:8080/api/users"


interface DatasetCreateProps {
  test?: any
}

/**
 * Show the list of users for the current project
 */
export default class DatasetCreateContainer extends React.Component<DatasetCreateProps, any> {


  constructor (props: any) {
    super(props)
  }

  render (): JSX.Element {
    return (
      <I18nProvider messageDir='adminApp/modules/userManagement/i18n'>
        <Card
          initiallyExpanded={true}>
          <CardHeader
            title={<FormattedMessage id="userlist.header"/>}
            actAsExpander={true}
            showExpandableButton={false}
          />
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
