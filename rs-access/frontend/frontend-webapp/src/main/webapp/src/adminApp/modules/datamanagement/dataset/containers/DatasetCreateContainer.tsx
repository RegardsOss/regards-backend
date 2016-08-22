import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"
import PickModelFormComponent from "../components/add/PickModelFormComponent"
import CreateModelFormComponent from "../components/add/CreateModelFormComponent"
import StepperCreateDatasetComponent from "../components/add/StepperCreateDatasetComponent"
import PickDatasourceFormComponent from "../components/add/PickDatasourceFormComponent"
import CreateDatasetSuccessComponent from "../components/add/CreateDatasetSuccessComponent"
import CreateDatasourceFormComponent from "../components/add/CreateDatasourceFormComponent"
import CreateConnectionFormComponent from "../../connection/components/CreateConnectionFormComponent"

export const STATES = {
  SELECT_MODELE: "select_modele",
  SELECT_MODELE_DONE: "select_modele_done",
  SELECT_SOURCE: "select_source",
  DONE: "done",
}


interface DatasetCreateProps {
  // From router
  router: any,
  route: any,
  params: any
}
/**
 */
export default class DatasetCreateContainer extends React.Component<DatasetCreateProps, any> {


  render (): JSX.Element {
    const {params} = this.props
    console.log(params)
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          <StepperCreateDatasetComponent
            state={params.step}
          />

          {(() => {
            switch (params.step) {
              case STATES.SELECT_MODELE:
                return <PickModelFormComponent />
              case STATES.SELECT_MODELE_DONE:
                return <PickModelFormComponent />
              case STATES.SELECT_SOURCE:
                return <PickDatasourceFormComponent />
              case STATES.DONE:
                return <CreateDatasetSuccessComponent />
              default:
                throw 'Undefined state ' + params.step
            }
          })()}
          <hr />
          <CreateModelFormComponent />
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

