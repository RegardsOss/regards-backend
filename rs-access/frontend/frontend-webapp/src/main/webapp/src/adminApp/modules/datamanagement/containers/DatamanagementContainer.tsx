import * as React from "react"
import { Card, CardTitle, CardText } from "material-ui/Card"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { FormattedMessage } from "react-intl"
import { Link } from "react-router"

interface DatamanagementCreateProps {
  test?: any
  // From router
  params: any
}

/**
 * Show the list of users for the current project
 */
export default class DatasetCreateContainer extends React.Component<DatamanagementCreateProps, any> {


  constructor (props: any) {
    super(props)
  }

  getCollectionCreate = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/collection/create"
  }
  getCollectionList = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/collection"
  }
  getDatasetCreate = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/dataset/create"
  }
  getDatasetList = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/dataset"
  }
  getModelList = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/model"
  }
  getModelCreate = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/model/create"
  }
  getDatasourceList = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/datasource"
  }
  getDatasourceCreate = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/datasource/create"
  }
  getConnectionList = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/connection"
  }
  getConnectionCreate = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/connection/create"
  }

  render (): JSX.Element {
    const style = {}
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          <Card
            initiallyExpanded={true}>
            <CardTitle
              title={<FormattedMessage id="datamanagement.header"/>}
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
                  <FormattedMessage id="datamanagement.model.list"/>
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getModelCreate()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.model.add"/>
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getDatasourceList()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.datasource.list"/>
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getDatasourceCreate()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.datasource.add"/>
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getConnectionList()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.connection.list"/>
                </CardText>
              </Link>
            </Card>

            <Card
              initiallyExpanded={true}>
              />
              <Link to={this.getConnectionCreate()} style={style}>
                <CardText>
                  <FormattedMessage id="datamanagement.connection.add"/>
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
