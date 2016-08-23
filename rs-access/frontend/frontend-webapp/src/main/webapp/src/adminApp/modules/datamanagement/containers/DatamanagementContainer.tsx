import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
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
    return "/admin/" + projectName + "/datamanagement/datasource/create"
  }
  getDatasourceCreate = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/datasource"
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
