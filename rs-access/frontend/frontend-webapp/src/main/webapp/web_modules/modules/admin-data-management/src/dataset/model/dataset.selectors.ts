import {BasicSelector} from "@regardsoss/store"

// Selectors
export const getDatasets = (state: any) => state.items

class DatasetSelectors extends BasicSelector {
  constructor () {
    super(["admin"])
  }

  getDatasets (state: any): any {
    return this.uncombineStore(state).items
  }

}


export default new DatasetSelectors()
