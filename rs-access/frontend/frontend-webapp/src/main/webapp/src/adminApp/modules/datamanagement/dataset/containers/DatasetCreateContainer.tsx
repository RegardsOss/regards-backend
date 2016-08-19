import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import PickModelFormComponent from "../components/add/PickModelFormComponent"
import CreateModelFormComponent from "../components/add/CreateModelFormComponent"
import StepperCreateDatasetComponent from "../components/add/StepperCreateDatasetComponent"
import PickDatasourceFormComponent from "../components/add/PickDatasourceFormComponent"
import CreateDatasetSuccessComponent from "../components/add/CreateDatasetSuccessComponent"
import CreateDatasourceFormComponent from "../components/add/CreateDatasourceFormComponent"
import CreateConnectionFormComponent from "../components/add/CreateConnectionFormComponent"


/**
 */
export default class DatasetCreateContainer extends React.Component<any, any> {


  render (): JSX.Element {
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          <StepperCreateDatasetComponent />
          <PickModelFormComponent />
          <hr />
          <CreateModelFormComponent />
          <hr />
          <PickDatasourceFormComponent />
          <hr />
          <CreateDatasetSuccessComponent />
          <hr />
          <CreateDatasourceFormComponent />
          <hr />
          <CreateConnectionFormComponent />
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

