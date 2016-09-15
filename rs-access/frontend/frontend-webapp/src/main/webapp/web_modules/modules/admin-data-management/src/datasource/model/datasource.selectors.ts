import {BasicSelector} from "@regardsoss/store"

// Selectors
export const getDatasets = (state: any) => state.items

class DatasourceSelectors extends BasicSelector {
  constructor () {
    super(["admin"])
  }

  getDatasources (state: any): any {
    return this.uncombineStore(state).items
  }
  getDatasourceById (state: any, id: number): any {
    return this.uncombineStore(state).items[id]
  }

}



export default new DatasourceSelectors()
