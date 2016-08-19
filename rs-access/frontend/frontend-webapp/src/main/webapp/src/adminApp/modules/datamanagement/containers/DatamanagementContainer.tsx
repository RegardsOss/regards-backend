import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { FormattedMessage } from "react-intl"
import { Link } from "react-router"


interface DatamanagementCreateProps {
  test?: any
}

/**
 * Show the list of users for the current project
 */
export default class DatasetCreateContainer extends React.Component<DatamanagementCreateProps, any> {


  constructor (props: any) {
    super(props)
  }

  render (): JSX.Element {
    const style = {}
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          <Card
            initiallyExpanded={true}>
            <CardHeader
              title={<FormattedMessage id="datamanagement.header"/>}
              actAsExpander={true}
              showExpandableButton={false}
            />
          </Card>
          <div style={style}>
            <Card
              initiallyExpanded={true}>
              />
              <Link to={"/admin/cdpp/datamanagement/"} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.collection.add"/>
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={"/admin/cdpp/datamanagement/"} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.collection.list"/>
                </CardText>
              </Link>

            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={"/admin/cdpp/datamanagement/dataset"} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.dataset.list"/>
                </CardText>
              </Link>

            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={"/admin/cdpp/datamanagement/dataset/create"} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.dataset.add"/>
                </CardText>
              </Link>

            </Card>
          </div>
        </div>
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
