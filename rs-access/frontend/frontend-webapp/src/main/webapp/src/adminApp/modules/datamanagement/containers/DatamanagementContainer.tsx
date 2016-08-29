import * as React from "react"
import { Card, CardTitle, CardText } from "material-ui/Card"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { FormattedMessage } from "react-intl"
import { Link } from "react-router"
import { ThemeContextInterface, ThemeContextType } from "../../../../common/theme/ThemeContainerInterface"
import { map } from "lodash"

interface DatamanagementCreateProps {
  test?: any
  // From router
  params: any
}

/**
 * Show the list of users for the current project
 */
export default class DatasetCreateContainer extends React.Component<DatamanagementCreateProps, any> {

  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface

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
  renderItem = (element: any, elementStyles: any, elementClasses: string, linkStyle: any) => {
    return (
      <div className={elementClasses}>
        <Card
          initiallyExpanded={true}
          style={elementStyles}
        >
          />
          <Link
            to={element.path}
            style={linkStyle}
          >
            <CardText>
              {element.title}
            </CardText>
          </Link>
        </Card>
      </div>
    )
  }


  render (): JSX.Element {
    const style = {
      items: {
        classes: this.context.muiTheme.adminApp.datamanagement.home.items.classes.join(' '),
        styles: this.context.muiTheme.adminApp.datamanagement.home.items.styles,

      },
      container: {
        classes: this.context.muiTheme.adminApp.datamanagement.home.container.classes.join(' '),
        styles: this.context.muiTheme.adminApp.datamanagement.home.container.styles,
      },
      links: this.context.muiTheme.linkWithoutDecoration
    }
    const elementsList = [
      {
        title: (<FormattedMessage id="datamanagement.collection.list"/>),
        path: this.getCollectionList()
      },
      {
        title: (<FormattedMessage id="datamanagement.dataset.list"/>),
        path: this.getDatasetList()
      },
      {
        title: (<FormattedMessage id="datamanagement.model.list"/>),
        path: this.getModelList()
      },
      {
        title: (<FormattedMessage id="datamanagement.datasource.list"/>),
        path: this.getDatasourceList()
      },
      {
        title: (<FormattedMessage id="datamanagement.connection.list"/>),
        path: this.getConnectionList()
      }
    ]
    const elementsCreate = [
      {
        title: (<FormattedMessage id="datamanagement.collection.add"/>),
        path: this.getCollectionCreate()
      },
      {
        title: (<FormattedMessage id="datamanagement.dataset.add"/>),
        path: this.getDatasetCreate()
      },
      {
        title: (<FormattedMessage id="datamanagement.model.add"/>),
        path: this.getModelCreate()
      },
      {
        title: (<FormattedMessage id="datamanagement.datasource.add"/>),
        path: this.getDatasourceCreate()
      },
      {
        title: (<FormattedMessage id="datamanagement.connection.add"/>),
        path: this.getConnectionCreate()
      }
    ]

    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          <Card
            initiallyExpanded={true}>
            <CardTitle
              title={<FormattedMessage id="datamanagement.header"/>}
            />
          </Card>
          <div className={style.container.classes} style={style.container.styles}>
            {map(elementsList, (element: any, id: string) => {
              return this.renderItem(element, style.items.styles, style.items.classes, style.links)
            })}
          </div>
          <div className={style.container.classes} style={style.container.styles}>
            {map(elementsCreate, (element: any, id: string) => {
              return this.renderItem(element, style.items.styles, style.items.classes, style.links)
            })}
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
