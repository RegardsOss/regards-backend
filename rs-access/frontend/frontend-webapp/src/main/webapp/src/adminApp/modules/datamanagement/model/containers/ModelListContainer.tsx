import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import ModelListComponent from "../components/ModelListComponent"

/**
 */
interface ModelListProps {
  // From router
  params: any
}
export default class ModelListContainer extends React.Component<ModelListProps, any> {

  getBackUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement"
  }
  getCreateUrl = () => {
    const projectName = this.props.params.project
    return "/admin/" + projectName + "/datamanagement/model/create"
  }

  render (): JSX.Element {
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <ModelListComponent
          getBackUrl={this.getBackUrl}
          getCreateUrl={this.getCreateUrl}
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
