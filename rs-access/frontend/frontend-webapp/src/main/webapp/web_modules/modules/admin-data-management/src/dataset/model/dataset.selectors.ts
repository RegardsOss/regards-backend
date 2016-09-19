import {BasicSelector} from "@regardsoss/store-utils"

// Selectors
export const getDatasets = (state: any) => state.items

class DatasetSelectors extends BasicSelector {
  constructor () {
    super(["admin", "data-management", "dataset"])
  }

  getDatasets (state: any): any {
    return this.uncombineStore(state).items
  }

}

const _instance = new DatasetSelectors()
export default _instance
