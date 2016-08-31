import * as React from "react"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import DatamanagementComponent from "../components/DatamanagementComponent"

interface DatamanagementProps {
  // From Router
  params: any
}
/**
 */
class DatamanagementContainer extends React.Component<DatamanagementProps, any> {

  render (): JSX.Element {
    const {params} = this.props
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <DatamanagementComponent params={params}/>
      </I18nProvider>
    )
  }
}


export default DatamanagementContainer
/*
 const mapStateToProps = (state: any, ownProps: any) => {
 const viewState = Selectors.getFormDatasetViewState(state)
 const models = Selectors.getModels(state)
 return {
 viewState,
 models
 }
 }
 const mapDispatchToProps = (dispatch: any) => ({
 setViewState: (newState: string) => dispatch(Actions.setViewState(newState)),
 setDatasetLabel: (label: string) => dispatch(Actions.setDatasetLabel(label)),
 setDatasetModelType: (modelType: number) => dispatch(Actions.setDatasetModelType(modelType)),
 setDatasetDefaultModelAttributes: (attributesDefined: Array<DatasetDefaultModelAttribute>) =>
 dispatch(Actions.setDatasetDefaultModelAttributes(attributesDefined))

 })
 export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetCreateContainer)
 */
