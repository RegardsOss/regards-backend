import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import { Datasource } from "../Datasource"
import DatasourceListComponent from "../components/list/DatasourceListComponent"


interface DatasourceListProps {
  // From router
  params: any

  // From mapStateToProps
  datasources?: Array<Datasource>
}


/**
 */
class DatasourceListContainer extends React.Component<DatasourceListProps, any> {


  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }

  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/datasource/create"
  }

  render (): JSX.Element {
    const {datasources} = this.props
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <DatasourceListComponent
          getBackUrl={this.getBackUrl}
          getCreateUrl={this.getCreateUrl}
          datasources={datasources}
        />
      </I18nProvider>
    )
  }
}
/*
 const mapStateToProps = (state: any, ownProps: any) => {
 const viewState = Selectors.getFormViewState(state)
 return {
 viewState: viewState
 }
 }
 const mapDispatchToProps = (dispatch: any) => ({
 setViewState: (newState: string) => dispatch(Actions.setViewState(newState))
 })
 export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetCreateContainer)
 */
export default DatasourceListContainer
