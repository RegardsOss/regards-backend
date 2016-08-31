import * as React from "react"
import { Card, CardText } from "material-ui/Card"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import { FormattedMessage, intlShape } from "react-intl"
import { Link } from "react-router"
import { map } from "lodash"
import CardActions from "material-ui/Card/CardActions"
import AddIcon from "material-ui/svg-icons/content/add-circle"
import ListIcon from "material-ui/svg-icons/content/filter-list"
import IconButton from "material-ui/IconButton"
import KeyboardArrowUp from "material-ui/svg-icons/hardware/keyboard-arrow-up"
import KeyboardArrowDown from "material-ui/svg-icons/hardware/keyboard-arrow-down"
import RaisedButton from "material-ui/RaisedButton"

interface DatamangementProps {
  params: any
}

/**
 * Show the list of users for the current project
 */
class DatamanagementComponent extends React.Component<DatamangementProps, any> {
  static contextTypes: Object = {
    intl: intlShape,
    muiTheme: React.PropTypes.object.isRequired
  }
  context: {
    intl: any,
    muiTheme: any
  }

  constructor (props: any) {
    super(props)
    this.state = {
      showAdvanced: false
    }
  }

  handleToggleAdvanced = () => {
    const {showAdvanced} = this.state
    this.setState({
      showAdvanced: !showAdvanced
    })
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
      <div className={elementClasses} key={element.pathList}>
        <Card
          initiallyExpanded={true}
          style={elementStyles}
        >
          <CardText>
            {element.title}
          </CardText>
          <CardText>
            {element.description}
          </CardText>
          <CardActions>
            <Link
              to={element.pathList}
              style={linkStyle}
            >
              <IconButton tooltip={this.context.intl.formatMessage({id: "datamanagement.action.list.tooltip"})}>
                <ListIcon />
              </IconButton>
            </Link>

            <Link
              to={element.pathCreate}
              style={linkStyle}
            >
              <IconButton tooltip={this.context.intl.formatMessage({id: "datamanagement.action.add.tooltip"})}>
                <AddIcon />
              </IconButton>
            </Link>
          </CardActions>
        </Card>
      </div>
    )
  }


  render (): JSX.Element {
    const style = {
      section: {
        items: {
          classes: this.context.muiTheme.adminApp.datamanagement.home.section1.items.classes.join(' '),
          styles: this.context.muiTheme.adminApp.datamanagement.home.section1.items.styles,

        },
        container: {
          classes: this.context.muiTheme.adminApp.datamanagement.home.section1.container.classes.join(' '),
          styles: this.context.muiTheme.adminApp.datamanagement.home.section1.container.styles,
        },
      },
      action: {
        classes: this.context.muiTheme.adminApp.datamanagement.home.action.classes.join(' '),
        styles: this.context.muiTheme.adminApp.datamanagement.home.action.styles,
      },
      links: this.context.muiTheme.linkWithoutDecoration
    }
    const elementsCommon = [
      {
        title: (<FormattedMessage id="datamanagement.collection"/>),
        description: (<FormattedMessage id="datamanagement.collection.info"/>),
        pathList: this.getCollectionList(),
        pathCreate: this.getCollectionCreate()
      },
      {
        title: (<FormattedMessage id="datamanagement.dataset"/>),
        description: (<FormattedMessage id="datamanagement.dataset.info"/>),
        pathList: this.getDatasetList(),
        pathCreate: this.getDatasetCreate()
      }
    ]
    if (this.state.showAdvanced) {
      elementsCommon.push({
        title: (<FormattedMessage id="datamanagement.model"/>),
        description: (<FormattedMessage id="datamanagement.model.info"/>),
        pathList: this.getModelList(),
        pathCreate: this.getModelCreate()
      })
      elementsCommon.push({
        title: (<FormattedMessage id="datamanagement.datasource"/>),
        description: (<FormattedMessage id="datamanagement.datasource.info"/>),
        pathList: this.getDatasourceList(),
        pathCreate: this.getDatasourceCreate()
      })
      elementsCommon.push({
        title: (<FormattedMessage id="datamanagement.connection"/>),
        description: (<FormattedMessage id="datamanagement.connection.info"/>),
        pathList: this.getConnectionList(),
        pathCreate: this.getConnectionCreate()
      })
    }
    const labelToggleAdvanced = this.state.showAdvanced ?
      <FormattedMessage id="datamanagement.collection.action.hideAdvanced"/> :
      <FormattedMessage id="datamanagement.collection.action.showAdvanced"/>
    const iconToggleAdvanced = this.state.showAdvanced ?
      <KeyboardArrowUp /> :
      <KeyboardArrowDown />
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          <div
            className={style.section.container.classes}
            style={style.section.container.styles}
          >
            {map(elementsCommon, (element: any, id: string) => {
              return this.renderItem(element, style.section.items.styles, style.section.items.classes, style.links)
            })}
          </div>
          <div
            className={style.action.classes}
            style={style.action.styles}
          >
            <RaisedButton
              label={labelToggleAdvanced}
              primary={true}
              icon={iconToggleAdvanced}
              onTouchTap={this.handleToggleAdvanced}
            />
          </div>
        </div>
      </I18nProvider>
    )
  }
}

export default DatamanagementComponent;
/*
 const mapStateToProps = (state: any, ownProps: any) => {
 }
 const mapDispatchToProps = (dispatch: any) => ({
 })
 export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetCreateContainer)
 */
