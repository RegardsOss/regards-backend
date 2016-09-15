import {BasicSelector} from "@regardsoss/store"

// Selectors
export const getDatasets = (state: any) => state.items

class ConnectionSelectors extends BasicSelector {
  constructor () {
    super(["admin"])
  }

  getConnections (state: any): any {
    return this.uncombineStore(state).items
  }
  getConnectionById (state: any, id: number): any {
    return this.uncombineStore(state).state.items[id]
  }
}


export default new ConnectionSelectors()
