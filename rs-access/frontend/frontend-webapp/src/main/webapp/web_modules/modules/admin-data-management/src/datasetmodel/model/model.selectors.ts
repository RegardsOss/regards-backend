import {BasicSelector} from "@regardsoss/store"

class ModelSelectors extends BasicSelector {
  constructor () {
    super(["admin"])
  }

  getDatasetModels (state: any): any {
    return this.uncombineStore(state).items
  }

  getDatasetModelById (state: any, id: number): any {
    return this.uncombineStore(state).items[id]
  }

}

export default new ModelSelectors()
