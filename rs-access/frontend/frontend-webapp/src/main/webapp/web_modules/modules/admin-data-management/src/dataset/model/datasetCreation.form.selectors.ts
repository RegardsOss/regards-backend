import {BasicSelector} from "@regardsoss/store"

// Selectors
export const getDatasets = (state: any) => state.items

class DatasetCreationFormSelectors extends BasicSelector {
  constructor () {
    super(["admin"])
  }

  getViewState (state: any): any {
    return this.uncombineStore(state).viewState
  }


}

export default new DatasetCreationFormSelectors()
