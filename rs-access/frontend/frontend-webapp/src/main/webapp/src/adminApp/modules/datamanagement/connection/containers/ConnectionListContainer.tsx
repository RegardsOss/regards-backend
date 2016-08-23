import * as React from "react"
import I18nProvider from "../../../../../common/i18n/I18nProvider"


/**
 */
export default class DatasetListContainer extends React.Component<any, any> {


  render (): JSX.Element {
    return (
      <I18nProvider messageDir='adminApp/modules/datamanagement/i18n'>
        <div>
          <h2>List connection</h2>
        </div>
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
