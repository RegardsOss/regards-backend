import * as React from "react"
import I18nProvider from "../../../../common/i18n/I18nProvider"
import DatamanagementComponent from "../components/DatamanagementComponent"
import ThemeInjector from "../../../../common/theme/ThemeInjector"
import I18nInjector from "../../../../common/i18n/I18nInjector"
import { ComposedInjector } from "@regardsoss/injector"

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
        <ComposedInjector >
          <DatamanagementComponent theme={null} intl={null} params={params}/>
        </ComposedInjector>
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
