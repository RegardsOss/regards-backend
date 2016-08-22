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

  getCollectionCreate = () => {
    return "/admin/cdpp/datamanagement/collection/create/"
  }
  getCollectionList = () => {
    return "/admin/cdpp/datamanagement/collection/"
  }
  getDatasetCreate = () => {
    return "/admin/cdpp/datamanagement/dataset/create/"
  }
  getDatasetList = () => {
    return "/admin/cdpp/datamanagement/dataset/"
  }
  getModelList = () => {
    return "/admin/cdpp/datamanagement/model/create"
  }
  getModelCreate = () => {
    return "/admin/cdpp/datamanagement/model/"
  }
  getDatasourceList = () => {
    return "/admin/cdpp/datamanagement/model/create"
  }
  getDatasourceCreate = () => {
    return "/admin/cdpp/datamanagement/model/"
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
              <Link to={this.getCollectionList()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.collection.list"/>
                </CardText>
              </Link>

            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getCollectionCreate()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.collection.add"/>
                </CardText>
              </Link>
            </Card>
            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getDatasetList()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.dataset.list"/>
                </CardText>
              </Link>

            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getDatasetCreate()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.dataset.add"/>
                </CardText>
              </Link>

            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getModelList()} style={style}>
                <CardText>
                  List models
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getModelCreate()} style={style}>
                <CardText>
                  Add model
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getDatasourceList()} style={style}>
                <CardText>
                  List datasources
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getDatasourceCreate()} style={style}>
                <CardText>
                  Add datasource
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
